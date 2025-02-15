package cn.ussshenzhou.notenoughbandwidth.modnetwork;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidth;
import cn.ussshenzhou.notenoughbandwidth.managers.PacketTypeIndexManager;
import com.github.luben.zstd.Zstd;
import com.mojang.logging.LogUtils;
import io.netty.buffer.*;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author USS_Shenzhou
 */
@MethodsReturnNonnullByDefault
public class PacketAggregationPacket implements CustomPacketPayload {
    public static final Type<PacketAggregationPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NotEnoughBandwidth.MODID, "packet_aggregation_packet"));
    private final ResourceLocation type;
    private final Collection<Packet<?>> packets;
    private final FriendlyByteBuf buf;
    private final ProtocolInfo<?> protocolInfo;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public PacketAggregationPacket(ResourceLocation type, Collection<Packet<?>> packets, ProtocolInfo<?> protocolInfo) {
        this.type = type;
        this.packets = packets;
        this.buf = new FriendlyByteBuf(Unpooled.buffer());
        this.protocolInfo = protocolInfo;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void encode(FriendlyByteBuf buffer) {
        FriendlyByteBuf bufRaw = new FriendlyByteBuf(ByteBufAllocator.DEFAULT.buffer());
        int index = PacketTypeIndexManager.getIndexNotTight(this.type);
        if (index != 0) {
            bufRaw.writeBoolean(true);
            bufRaw.writeMedium(index);
        } else {
            bufRaw.writeBoolean(false);
            bufRaw.writeResourceLocation(this.type);
        }
        for (Packet packet : this.packets) {
            var b = Unpooled.buffer();
            protocolInfo.codec().encode(b, packet);
            bufRaw.writeVarInt(b.readableBytes());
            bufRaw.writeBytes(b);
            b.release();
        }
        FriendlyByteBuf bufCompressed = new FriendlyByteBuf(Unpooled.wrappedBuffer(Zstd.compress(bufRaw.nioBuffer(), Zstd.defaultCompressionLevel())));
        float compressRatio = (float) bufCompressed.readableBytes() / bufRaw.readableBytes();
        if (compressRatio > 0.95f) {
            buffer.writeBoolean(false);
            buffer.writeBytes(bufRaw);
        } else {
            if (LogUtils.getLogger().isTraceEnabled()) {
                LogUtils.getLogger().trace("Packet {} aggregation compressed: {} bytes-> {} bytes ( {} %).",
                        type, bufRaw.readableBytes(), bufCompressed.readableBytes(),
                        String.format("%.2f", 100f * compressRatio));
            }
            buffer.writeBoolean(true);
            buffer.writeVarInt(bufRaw.readableBytes());
            buffer.writeBytes(bufCompressed);
        }
        bufRaw.release();
        bufCompressed.release();
    }

    public PacketAggregationPacket(FriendlyByteBuf buffer) {
        boolean compressed = buffer.readBoolean();
        this.protocolInfo = null;
        if (compressed) {
            int size = buffer.readVarInt();
            this.buf = new FriendlyByteBuf(decompress(buffer.retainedDuplicate(), size));
        } else {
            this.buf = new FriendlyByteBuf(buffer.retainedDuplicate());
        }
        if (buf.readBoolean()) {
            type = PacketTypeIndexManager.getResourceLocation(buf.readUnsignedMedium() & 0x00ffffff, false);
        } else {
            type = buf.readResourceLocation();
        }
        packets = new ArrayList<>();
        buffer.readerIndex(buffer.writerIndex());
    }

    @SuppressWarnings("unchecked")
    public void handler(IPayloadContext context) {
        var protocolInfo = context.connection().getInboundProtocol();
        while (buf.readableBytes() > 0) {
            int size = buf.readVarInt();
            var subBuf = buf.readRetainedSlice(size);
            Packet<ICommonPacketListener> packet = (Packet<ICommonPacketListener>) protocolInfo.codec().decode(subBuf);
            packet.handle(context.listener());
            subBuf.release();
        }
        this.buf.release();
    }

    public static ByteBuf decompress(ByteBuf compressed, int originalSize) {
        if (compressed.isDirect()) {
            return Unpooled.wrappedBuffer(Zstd.decompress(compressed.nioBuffer(), originalSize));
        } else {
            var directBuf = Unpooled.directBuffer(compressed.readableBytes());
            compressed.getBytes(compressed.readerIndex(), directBuf);
            var decompressed = Unpooled.wrappedBuffer(Zstd.decompress(directBuf.nioBuffer(), originalSize));
            directBuf.release();
            return decompressed;
        }
    }
}
