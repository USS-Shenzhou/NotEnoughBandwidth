package cn.ussshenzhou.notenoughbandwidth.aggregation;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidthConfig;
import cn.ussshenzhou.notenoughbandwidth.ModConstants;
import cn.ussshenzhou.notenoughbandwidth.Statistic;
import cn.ussshenzhou.notenoughbandwidth.config.ConfigHelper;
import cn.ussshenzhou.notenoughbandwidth.indextype.CustomPacketPrefixHelper;
import cn.ussshenzhou.notenoughbandwidth.zstd.ZstdHelper;
import com.mojang.logging.LogUtils;
import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.filters.GenericPacketSplitter;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;

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

    private final FriendlyByteBuf data;
    //----------------------------------------encode----------------------------------------
    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private final ArrayList<AggregatedEncodePacket> packetsToEncode;
    private final ProtocolInfo<?> protocolInfo;
    private Connection connection;

    public PacketAggregationPacket(ArrayList<AggregatedEncodePacket> packetsToEncode, ProtocolInfo<?> protocolInfo, Connection connection) {
        this.packetsToEncode = packetsToEncode;
        this.data = new FriendlyByteBuf(Unpooled.buffer());
        this.protocolInfo = protocolInfo;
        this.connection = connection;
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
    @SuppressWarnings("UnstableApiUsage")
    public void encode(FriendlyByteBuf buffer) {
        //skip GenericPacketSplitter
        if (WALKER.walk(s -> s.anyMatch(frame -> frame.getDeclaringClass() == GenericPacketSplitter.class))) {
            return;
        }

        FriendlyByteBuf rawBuf = new FriendlyByteBuf(ByteBufAllocator.DEFAULT.buffer());
        packetsToEncode.forEach(p -> {
            encodePackets(rawBuf, p);
        });
        var compressedBuf = new FriendlyByteBuf(ZstdHelper.compress(connection, rawBuf));
        logCompressRatio(rawBuf, compressedBuf);
        // S
        buffer.writeVarInt(rawBuf.readableBytes());
        buffer.writeBytes(compressedBuf);
        rawBuf.release();
        compressedBuf.release();
    }

    private static void logCompressRatio(FriendlyByteBuf rawBuf, FriendlyByteBuf compressedBuf) {
        int rawSize = rawBuf.readableBytes();
        int compressedSize = compressedBuf.readableBytes();

        long a = Statistic.OUTBOUND_RAW.addAndGet(rawSize);
        long b = Statistic.OUTBOUND_COMPRESSED.addAndGet(compressedSize);
        LogUtils.getLogger().warn("Packet aggregated and compressed: {} bytes -> {} bytes ( {} %).",
                a,
                b,
                String.format("%.2f", 100f * b / a)
        );

        if (ConfigHelper.getConfigRead(NotEnoughBandwidthConfig.class).debugLog) {
            LogUtils.getLogger().debug("Packet aggregated and compressed: {} bytes -> {} bytes ( {} %).",
                    rawSize,
                    compressedSize,
                    String.format("%.2f", 100f * compressedSize / rawSize)
            );
        } else {
            LogUtils.getLogger().trace("Packet aggregated and compressed: {} bytes -> {} bytes ( {} %).",
                    rawSize,
                    compressedSize,
                    String.format("%.2f", 100f * compressedSize / rawSize)
            );
        }
    }

    private void encodePackets(FriendlyByteBuf raw, AggregatedEncodePacket packet) {
        var type = packet.type;
        // p
        CustomPacketPrefixHelper.get().index(type).save(raw);
        // s
        var d = new FriendlyByteBuf(ByteBufAllocator.DEFAULT.buffer());
        packet.encode(d, protocolInfo, connection.getSending());
        raw.writeVarInt(d.readableBytes());
        // d
        raw.writeBytes(d);
        d.release();
    }

    //----------------------------------------decode----------------------------------------
    public PacketAggregationPacket(FriendlyByteBuf buffer) {
        this.protocolInfo = null;
        this.packetsToEncode = null;
        this.data = new FriendlyByteBuf(buffer.retainedDuplicate());
        buffer.readerIndex(buffer.writerIndex());
    }

    //----------------------------------------handle----------------------------------------
    public void handler(IPayloadContext context) {
        this.connection = context.connection();
        // S
        int size = data.readVarInt();
        var decompressed = new FriendlyByteBuf(ZstdHelper.decompress(connection, data.retainedDuplicate(), size));
        data.release();
        var protocolInfo = context.connection().getInboundProtocol();
        var packetsToHandle = new ArrayList<AggregatedDecodePacket>();
        while (decompressed.readableBytes() > 0) {
            deAggregatePackets(decompressed, packetsToHandle);
        }
        decompressed.release();
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
