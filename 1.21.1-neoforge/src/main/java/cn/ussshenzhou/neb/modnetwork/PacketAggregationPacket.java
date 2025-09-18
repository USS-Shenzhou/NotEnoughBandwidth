package cn.ussshenzhou.neb.modnetwork;

import cn.ussshenzhou.ModConstants;
import cn.ussshenzhou.neb.managers.NamespaceIndexManager;
import cn.ussshenzhou.util.ByteBufHelper;
import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
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
    public static final Type<PacketAggregationPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ModConstants.MOD_ID, "packet_aggregation_packet"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final Map<ResourceLocation, ArrayList<Packet<?>>> packets;
    private final FriendlyByteBuf buf;
    // only used for encode
    private ProtocolInfo<?> protocolInfo;

    public PacketAggregationPacket(Map<ResourceLocation, ArrayList<Packet<?>>> packets, ProtocolInfo<?> protocolInfo) {
        this.packets = packets;
        this.buf = new FriendlyByteBuf(Unpooled.buffer());
        this.protocolInfo = protocolInfo;
    }

    /**
     * <pre>
     * ┌---┬---┬---┬---┬----┬----┬----┬----┬-...-┬---┬---┬---┬----┬----┬----┬----┐
     * │ S │ b │ t │ n │ s0 │ d0 │ s1 │ d1 │ ... │ b │ t │ n │ s0 │ d0 │ s1 │ d1 │
     * └---┴---┴---┴---┴----┴----┴----┴----┴-...-┴---┴---┴---┴----┴----┴----┴----┘
     *     └--------all packets of type A--------┘└-----all packets of type B----┘
     *     └------------------------------compressed-----------------------------┘
     *
     * S = varint, size of compressed buf
     * b = bool, whether using indexed type
     * t = medium or ResLoc, type
     * n = varint, subpacket amount of this type
     * s = varint, size of this subpacket
     * d = bytes, data of this subpacket
     * </pre>
     */
    public void encode(FriendlyByteBuf buffer) {
        FriendlyByteBuf rawBuf = new FriendlyByteBuf(ByteBufAllocator.DEFAULT.buffer());
        packets.forEach((tag, packets) -> {
            encodePackets(rawBuf, tag, packets);
        });
        var compressedBuf = new FriendlyByteBuf(ByteBufHelper.compress(rawBuf));
        if (LogUtils.getLogger().isTraceEnabled()) {
            LogUtils.getLogger().trace("Packet aggregation compressed: {} bytes-> {} bytes ( {} %).",
                    rawBuf.readableBytes(),
                    compressedBuf.readableBytes(),
                    String.format("%.2f", 100f * compressedBuf.readableBytes() / rawBuf.readableBytes())
            );
        }
        // S
        buffer.writeVarInt(rawBuf.readableBytes());
        buffer.writeBytes(compressedBuf);

        rawBuf.release();
        compressedBuf.release();
    }

    private void encodePackets(FriendlyByteBuf raw, ResourceLocation type, Collection<Packet<?>> packets) {
        int nebIndex = NamespaceIndexManager.getNebIndexNotTight(type);
        // b, t
        if (nebIndex != 0) {
            raw.writeBoolean(true);
            raw.writeMedium(nebIndex);
        } else {
            raw.writeBoolean(false);
            raw.writeResourceLocation(type);
        }
        // n
        raw.writeVarInt(packets.size());
        for (var packet : packets) {
            encodePacket(raw, packet);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void encodePacket(FriendlyByteBuf raw, Packet<?> packet) {
        var b = Unpooled.buffer();
        protocolInfo.codec().encode(b, (Packet) packet);
        // s
        raw.writeVarInt(b.readableBytes());
        // d
        raw.writeBytes(b);
        b.release();
    }

    public PacketAggregationPacket(FriendlyByteBuf buffer) {
        this.protocolInfo = null;
        this.packets = new HashMap<>();
        // S
        int size = buffer.readVarInt();
        this.buf = new FriendlyByteBuf(ByteBufHelper.decompress(buffer.retainedDuplicate(), size));
        buffer.readerIndex(buffer.writerIndex());
    }

    public void handler(IPayloadContext context) {
        this.protocolInfo = context.connection().getInboundProtocol();
        while (this.buf.readableBytes() > 0) {
            decodePackets(this.buf, context.listener());
        }
        this.buf.release();
    }

    private void decodePackets(FriendlyByteBuf buf, ICommonPacketListener listener) {
        // b, t
        var type = buf.readBoolean()
                ? NamespaceIndexManager.getResourceLocation(buf.readUnsignedMedium() & 0x00ffffff, false)
                : buf.readResourceLocation();
        // n
        var amount = buf.readVarInt();
        for (var i = 0; i < amount; i++) {
            decodePacket(buf, listener);
        }
    }

    @SuppressWarnings("unchecked")
    private void decodePacket(FriendlyByteBuf buf, ICommonPacketListener listener) {
        // s
        var size = buf.readVarInt();
        // d
        var data = buf.readRetainedSlice(size);
        var packet = (Packet<ICommonPacketListener>) protocolInfo.codec().decode(data);
        packet.handle(listener);
        data.release();
    }
}
