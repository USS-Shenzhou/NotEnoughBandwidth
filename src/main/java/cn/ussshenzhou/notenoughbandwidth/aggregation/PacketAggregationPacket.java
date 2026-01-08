package cn.ussshenzhou.notenoughbandwidth.aggregation;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidthConfig;
import cn.ussshenzhou.notenoughbandwidth.ModConstants;
import cn.ussshenzhou.notenoughbandwidth.config.ConfigHelper;
import cn.ussshenzhou.notenoughbandwidth.indextype.CustomPacketPrefixHelper;
import cn.ussshenzhou.notenoughbandwidth.indextype.NamespaceIndexManager;
import cn.ussshenzhou.notenoughbandwidth.util.ByteBufHelper;
import com.mojang.logging.LogUtils;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author USS_Shenzhou
 */
@MethodsReturnNonnullByDefault
public class PacketAggregationPacket implements CustomPacketPayload {
    public static final Type<PacketAggregationPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "packet_aggregation_packet"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final FriendlyByteBuf uncompressed;
    //----------------------------------------encode----------------------------------------
    private final ArrayList<AggregatedEncodePacket> packetsToEncode;
    private final ProtocolInfo<?> protocolInfo;
    private PacketFlow packetFlow;

    public PacketAggregationPacket(ArrayList<AggregatedEncodePacket> packetsToEncode, ProtocolInfo<?> protocolInfo, PacketFlow packetFlow) {
        this.packetsToEncode = packetsToEncode;
        this.uncompressed = new FriendlyByteBuf(Unpooled.buffer());
        this.protocolInfo = protocolInfo;
        this.packetFlow = packetFlow;
    }

    /**
     * <pre>
     * ┌---┬----┬----┬----┬----┬----┬----...
     * │ S │ p0 │ s0 │ d0 │ p1 │ s1 │ d1 ...
     * └---┴----┴----┴----┴----┴----┴----...
     *     └--packet 1---┘└--packet 2---┘
     *     └----------compressed----------┘
     *
     * S = varint, size of compressed buf
     * p = prefix (medium/int/utf-8)， type of this subpacket
     * s = varint, size of this subpacket
     * d = bytes, data of this subpacket
     * </pre>
     */
    public void encode(FriendlyByteBuf buffer) {
        FriendlyByteBuf rawBuf = new FriendlyByteBuf(ByteBufAllocator.DEFAULT.buffer());
        packetsToEncode.forEach(p -> {
            encodePackets(rawBuf, p);
        });
        var compressedBuf = new FriendlyByteBuf(ByteBufHelper.compress(rawBuf));
        logCompressRatio(rawBuf, compressedBuf);
        // S
        buffer.writeVarInt(rawBuf.readableBytes());
        buffer.writeBytes(compressedBuf);
        rawBuf.release();
        compressedBuf.release();
    }

    private static void logCompressRatio(FriendlyByteBuf rawBuf, FriendlyByteBuf compressedBuf) {
        if (ConfigHelper.getConfigRead(NotEnoughBandwidthConfig.class).debugLog) {
            LogUtils.getLogger().debug("Packet aggregated and compressed: {} bytes-> {} bytes ( {} %).",
                    rawBuf.readableBytes(),
                    compressedBuf.readableBytes(),
                    String.format("%.2f", 100f * compressedBuf.readableBytes() / rawBuf.readableBytes())
            );
        } else {
            LogUtils.getLogger().trace("Packet aggregated and compressed: {} bytes-> {} bytes ( {} %).",
                    rawBuf.readableBytes(),
                    compressedBuf.readableBytes(),
                    String.format("%.2f", 100f * compressedBuf.readableBytes() / rawBuf.readableBytes())
            );
        }
    }

    private void encodePackets(FriendlyByteBuf raw, AggregatedEncodePacket packet) {
        var type = packet.type;
        // p
        CustomPacketPrefixHelper.get().index(type).save(raw);
        // s
        var d = new FriendlyByteBuf(ByteBufAllocator.DEFAULT.buffer());
        packet.encode(d, protocolInfo, packetFlow);
        raw.writeVarInt(d.readableBytes());
        // d
        raw.writeBytes(d);
        d.release();
    }

    //----------------------------------------decode----------------------------------------
    public PacketAggregationPacket(FriendlyByteBuf buffer) {
        this.protocolInfo = null;
        this.packetsToEncode = null;
        // S
        int size = buffer.readVarInt();
        this.uncompressed = new FriendlyByteBuf(ByteBufHelper.decompress(buffer.retainedDuplicate(), size));
        buffer.readerIndex(buffer.writerIndex());
    }

    //----------------------------------------handle----------------------------------------
    public void handler(IPayloadContext context) {
        var protocolInfo = context.connection().getInboundProtocol();
        var packetsToHandle = new ArrayList<AggregatedDecodePacket>();
        while (this.uncompressed.readableBytes() > 0) {
            deAggregatePackets(this.uncompressed, packetsToHandle);
        }
        this.uncompressed.release();
        this.handlePackets(packetsToHandle, protocolInfo, context);
    }

    private void deAggregatePackets(FriendlyByteBuf buf, ArrayList<AggregatedDecodePacket> packetsToHandle) {
        // p
        var type = CustomPacketPrefixHelper.getType(buf);
        // s
        var size = buf.readVarInt();
        // d
        var data = new FriendlyByteBuf(buf.readRetainedSlice(size));
        packetsToHandle.add(new AggregatedDecodePacket(type, data));
    }

    private void handlePackets(ArrayList<AggregatedDecodePacket> packetsToHandle, ProtocolInfo<?> protocolInfo, IPayloadContext context) {
        packetsToHandle.forEach(packet -> {
            packet.handle(protocolInfo, context);
            packet.getData().release();
        });
    }
}
