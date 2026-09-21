package cn.ussshenzhou.notenoughbandwidth.util;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidthConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.BandwidthDebugMonitor;
import net.minecraft.network.VarInt;
import net.minecraft.network.Varint21FrameDecoder;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author USS_Shenzhou
 * @see net.minecraft.network.Varint21FrameDecoder
 */
public class NebVarintFrameDecoder extends Varint21FrameDecoder {

    private static final int MAX_BYTES = 4;
    private final ByteBuf helperBuf = Unpooled.directBuffer(MAX_BYTES);

    public NebVarintFrameDecoder(@Nullable BandwidthDebugMonitor monitor) {
        super(monitor);
    }

    @Override
    protected void handlerRemoved0(ChannelHandlerContext ctx) {
        this.helperBuf.release();
    }

    private static boolean copyVarint(ByteBuf in, ByteBuf out) {
        for (int i = 0; i < MAX_BYTES; i++) {
            if (!in.isReadable()) {
                return false;
            }

            byte b = in.readByte();
            out.writeByte(b);
            if (!VarInt.hasContinuationBit(b)) {
                return true;
            }
        }

        throw new CorruptedFrameException("length wider than 21-bit");
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        in.markReaderIndex();
        this.helperBuf.clear();
        if (!copyVarint(in, this.helperBuf)) {
            in.resetReaderIndex();
        } else {
            int length = VarInt.read(this.helperBuf);
            int maxSize = NotEnoughBandwidthConfig.get().getMaxPacketSize();
            if (length > maxSize) {
                throw new EncoderException("NEB: Packet too large: size " + length + " is over " + maxSize);
            }
            if (length == 0) {
                throw new CorruptedFrameException("Frame length cannot be zero");
            } else if (in.readableBytes() < length) {
                in.resetReaderIndex();
            } else {
                if (this.monitor != null) {
                    this.monitor.onReceive(length + VarInt.getByteSize(length));
                }

                out.add(in.readBytes(length));
            }
        }
    }
}
