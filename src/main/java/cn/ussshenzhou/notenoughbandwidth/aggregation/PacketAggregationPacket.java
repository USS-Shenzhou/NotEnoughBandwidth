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
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
    public Type<PacketAggregationPacket> type() {
        return TYPE;
    }


    //----------------------------------------encode----------------------------------------
    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private final ArrayList<AggregatedEncodePacket> packetsToEncode;
    private final ProtocolInfo<?> protocolInfo;
    private Connection connection;

    public PacketAggregationPacket(ArrayList<AggregatedEncodePacket> packetsToEncode, ProtocolInfo<?> protocolInfo, Connection connection) {
        this.packetsToEncode = packetsToEncode;
        this.protocolInfo = protocolInfo;
        this.connection = connection;
    }

    /**
     * <pre>
     * ┌---┬-----┬----┬----┬----┬----┬----┬----...
     * │ C │ (S) │ p0 │ s0 │ d0 │ p1 │ s1 │ d1 ...
     * └---┴-----┴----┴----┴----┴----┴----┴----...
     *           └--packet 1---┘└--packet 2---┘
     *           └----------compressed----------┘
     *
     * C = bool, whether compressed
     * S = varint, size of compressed buf (if C == true)
     * p = prefix (medium/int/utf-8)， type of this subpacket
     * s = varint, size of this subpacket
     * d = bytes, data of this subpacket
     * </pre>
     */
    @SuppressWarnings("UnstableApiUsage")
    public void encode(RegistryFriendlyByteBuf buffer) {
        //skip GenericPacketSplitter
        if (WALKER.walk(s -> s.anyMatch(frame -> frame.getDeclaringClass() == GenericPacketSplitter.class))) {
            return;
        }
        var rawBuf = new RegistryFriendlyByteBuf(ByteBufAllocator.DEFAULT.buffer(), buffer.registryAccess(), buffer.getConnectionType());
        packetsToEncode.forEach(p -> encodePackets(rawBuf, p));
        var compressedBuf = new FriendlyByteBuf(ZstdHelper.compress(connection, rawBuf));
        // C, S
        int rawSize = rawBuf.readableBytes();
        int compressedSize = compressedBuf.readableBytes();
        FriendlyByteBuf baked;
        if (rawSize <= compressedSize) {
            buffer.writeBoolean(false);
            baked = rawBuf;
        } else {
            buffer.writeBoolean(true);
            baked = compressedBuf;
            buffer.writeVarInt(rawBuf.readableBytes());
        }
        logCompressRatio(rawSize, compressedSize);

        buffer.writeBytes(baked);
        rawBuf.release();
        compressedBuf.release();
    }

    private static void logCompressRatio(int rawSize, int compressedSize) {
        Statistic.OUTBOUND_RAW.addAndGet(rawSize);
        Statistic.OUTBOUND_BAKED.addAndGet(compressedSize);
        var log = "Packet aggregated"
                + (rawSize <= compressedSize ? ": " : "and compressed: ")
                + rawSize
                + " bytes -> "
                + Math.min(rawSize, compressedSize)
                + " bytes ( "
                + String.format("%.2f", 100f * Math.min(rawSize, compressedSize) / rawSize)
                + "%)";
        if (ConfigHelper.getConfigRead(NotEnoughBandwidthConfig.class).debugLog) {
            LogUtils.getLogger().debug(log);
        } else {
            LogUtils.getLogger().trace(log);
        }
    }

    private void encodePackets(RegistryFriendlyByteBuf raw, AggregatedEncodePacket packet) {
        var type = packet.type;
        // p
        CustomPacketPrefixHelper.get().index(type).save(raw);
        // s
        var d = new RegistryFriendlyByteBuf(ByteBufAllocator.DEFAULT.buffer(), raw.registryAccess(), raw.getConnectionType());
        packet.encode(d, protocolInfo, connection.getSending());
        raw.writeVarInt(d.readableBytes());
        // d
        raw.writeBytes(d);
        d.release();
    }

    //----------------------------------------decode----------------------------------------
    private RegistryFriendlyByteBuf data;

    public PacketAggregationPacket(RegistryFriendlyByteBuf buffer) {
        this.protocolInfo = null;
        this.packetsToEncode = null;
        this.data = new RegistryFriendlyByteBuf(buffer.retainedDuplicate(), buffer.registryAccess(), buffer.getConnectionType());
        buffer.readerIndex(buffer.writerIndex());
    }

    //----------------------------------------handle----------------------------------------
    public void handler(IPayloadContext context) {
        this.connection = context.connection();
        // C
        var compressed = data.readBoolean();
        // S
        RegistryFriendlyByteBuf raw;
        if (compressed) {
            int size = data.readVarInt();
            raw = new RegistryFriendlyByteBuf(
                    ZstdHelper.decompress(connection, data.retainedDuplicate(), size),
                    data.registryAccess(),
                    data.getConnectionType()
            );
        } else {
            raw = new RegistryFriendlyByteBuf(data.retainedDuplicate(), data.registryAccess(), data.getConnectionType());
        }
        data.release();
        var protocolInfo = context.connection().getInboundProtocol();
        var packetsToHandle = new ArrayList<AggregatedDecodePacket>();
        while (raw.readableBytes() > 0) {
            deAggregatePackets(raw, packetsToHandle);
        }
        raw.release();
        this.handlePackets(packetsToHandle, protocolInfo, context);
    }

    private void deAggregatePackets(RegistryFriendlyByteBuf buf, ArrayList<AggregatedDecodePacket> packetsToHandle) {
        // p
        var type = CustomPacketPrefixHelper.getType(buf);
        // s
        var size = buf.readVarInt();
        // d
        var data = new RegistryFriendlyByteBuf(buf.readRetainedSlice(size), this.data.registryAccess(), this.data.getConnectionType());
        packetsToHandle.add(new AggregatedDecodePacket(type, data));
    }

    private void handlePackets(ArrayList<AggregatedDecodePacket> packetsToHandle, ProtocolInfo<?> protocolInfo, IPayloadContext context) {
        packetsToHandle.forEach(packet -> {
            packet.handle(protocolInfo, context);
            packet.getData().release();
        });
    }
}
