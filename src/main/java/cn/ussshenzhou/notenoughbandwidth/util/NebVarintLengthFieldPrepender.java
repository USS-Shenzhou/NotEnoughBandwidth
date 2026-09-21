package cn.ussshenzhou.notenoughbandwidth.util;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidthConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.VarInt;
import net.minecraft.network.Varint21LengthFieldPrepender;

public class NebVarintLengthFieldPrepender extends Varint21LengthFieldPrepender {
    public static final int MAX_BYTES = 4;

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
        int bodyLength = msg.readableBytes();
        int maxSize = NotEnoughBandwidthConfig.get().getMaxPacketSize();
        if (bodyLength > maxSize) {
            throw new EncoderException("NEB: Packet too large: size " + bodyLength + " is over " + maxSize);
        }
        int headerLength = VarInt.getByteSize(bodyLength);
        if (headerLength > MAX_BYTES) {
            throw new EncoderException("Packet too large: size " + bodyLength + " is over 8");
        } else {
            out.ensureWritable(headerLength + bodyLength);
            VarInt.write(out, bodyLength);
            out.writeBytes(msg, msg.readerIndex(), bodyLength);
        }
    }
}
