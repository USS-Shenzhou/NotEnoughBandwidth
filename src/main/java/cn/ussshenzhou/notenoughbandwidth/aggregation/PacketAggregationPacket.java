package cn.ussshenzhou.notenoughbandwidth.aggregation;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidthConfig;
import cn.ussshenzhou.notenoughbandwidth.ModConstants;
import cn.ussshenzhou.notenoughbandwidth.config.ConfigHelper;
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
@SuppressWarnings("MapOrSetKeyShouldOverrideHashCodeEquals")
@MethodsReturnNonnullByDefault
public class PacketAggregationPacket implements CustomPacketPayload {
    public static final Type<PacketAggregationPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "packet_aggregation_packet"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final FriendlyByteBuf uncompressed;
    //----------------------------------------encode----------------------------------------
    private final Map<Identifier, ArrayList<AggregatedEncodePacket>> packetsToEncode;
    private final ProtocolInfo<?> protocolInfo;
    private long minOrder;
    private PacketFlow packetFlow;

    public PacketAggregationPacket(Map<Identifier, ArrayList<AggregatedEncodePacket>> packetsToEncode, ProtocolInfo<?> protocolInfo, PacketFlow packetFlow) {
        this.packetsToEncode = packetsToEncode;
        this.uncompressed = new FriendlyByteBuf(Unpooled.buffer());
        this.protocolInfo = protocolInfo;
        this.packetFlow = packetFlow;
    }

    /**
     * <pre>
     * ┌---┬---┬---┬---┬----┬----┬----┬-...-┬---┬---┬---┬----┬----┬----┬-...-┐
     * │ S │ b │ t │ n │ o0 │ s0 │ d0 │ ... │ b │ t │ n │ o0 │ s0 │ d0 │ ... │
     * └---┴---┴---┴---┴----┴----┴----┴-...-┴---┴---┴---┴----┴----┴----┴-...-┘
     *     └-----all packets of type A-----┘└------all packets of type B-----┘
     *     └---------------------------compressed----------------------------┘
     *
     * S = varint, size of compressed buf
     * b = bool, whether using indexed type
     * t = medium or ResLoc, type
     * n = varint, subpacket amount of this type
     * o = varint, order of this subpacket
     * s = varint, size of this subpacket
     * d = bytes, data of this subpacket
     * </pre>
     */
    public void encode(FriendlyByteBuf buffer) {
        minOrder = getMinOrder();
        FriendlyByteBuf rawBuf = new FriendlyByteBuf(ByteBufAllocator.DEFAULT.buffer());
        packetsToEncode.forEach((tag, packets) -> {
            encodePackets(rawBuf, tag, packets);
        });
        var compressedBuf = new FriendlyByteBuf(ByteBufHelper.compress(rawBuf));
        logCompressRatio(rawBuf, compressedBuf);
        // S
        buffer.writeVarInt(rawBuf.readableBytes());
        buffer.writeBytes(compressedBuf);

        rawBuf.release();
        compressedBuf.release();
    }

    private long getMinOrder() {
        long min = Long.MAX_VALUE;
        for (var list : packetsToEncode.values()) {
            min = Math.min(min, list.getFirst().getOrder());
        }
        return min;
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

    private void encodePackets(FriendlyByteBuf raw, Identifier type, Collection<AggregatedEncodePacket> packets) {
        int nebIndex = NamespaceIndexManager.getNebIndexNotTight(type);
        // b, t
        if (nebIndex != 0) {
            raw.writeBoolean(true);
            raw.writeMedium(nebIndex);
        } else {
            raw.writeBoolean(false);
            raw.writeIdentifier(type);
        }
        // n
        raw.writeVarInt(packets.size());
        for (var packet : packets) {
            encodePacket(raw, packet);
        }
    }

    private void encodePacket(FriendlyByteBuf raw, AggregatedEncodePacket packet) {
        var b = new FriendlyByteBuf(ByteBufAllocator.DEFAULT.buffer());
        packet.encode(b, protocolInfo, packetFlow);
        // o
        raw.writeVarInt((int) (packet.getOrder() - minOrder));
        // s
        raw.writeVarInt(b.readableBytes());
        // d
        raw.writeBytes(b);
        b.release();
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
        var packetsToHandle = new Int2ObjectRBTreeMap<AggregatedDecodePacket>();
        while (this.uncompressed.readableBytes() > 0) {
            deAggregatePackets(this.uncompressed, packetsToHandle);
        }
        this.uncompressed.release();
        this.handlePackets(packetsToHandle, protocolInfo, context);
    }

    private void deAggregatePackets(FriendlyByteBuf buf, Int2ObjectRBTreeMap<AggregatedDecodePacket> packetsToHandle) {
        // b, t
        var type = buf.readBoolean()
                ? NamespaceIndexManager.getIdentifier(buf.readUnsignedMedium() & 0x00ffffff, false)
                : buf.readIdentifier();
        // n
        var amount = buf.readVarInt();
        for (var i = 0; i < amount; i++) {
            // o
            var order = buf.readVarInt();
            // s
            var size = buf.readVarInt();
            // d
            var data = new FriendlyByteBuf(buf.readRetainedSlice(size));
            packetsToHandle.put(order, new AggregatedDecodePacket(type, data));
        }
    }

    private void handlePackets(Int2ObjectRBTreeMap<AggregatedDecodePacket> packetsToHandle, ProtocolInfo<?> protocolInfo, IPayloadContext context) {
        packetsToHandle.values().forEach(packet -> {
            packet.handle(protocolInfo, context);
            packet.getData().release();
        });
    }
}
