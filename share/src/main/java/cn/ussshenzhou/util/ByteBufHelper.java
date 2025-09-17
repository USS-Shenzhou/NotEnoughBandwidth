package cn.ussshenzhou.util;

import com.github.luben.zstd.Zstd;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * @author USS_Shenzhou
 */
public class ByteBufHelper {

    public static ByteBuf compress(ByteBuf raw) {
        return Unpooled.wrappedBuffer(Zstd.compress(raw.nioBuffer(), Zstd.defaultCompressionLevel()));
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
