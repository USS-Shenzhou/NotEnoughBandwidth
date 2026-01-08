package cn.ussshenzhou.notenoughbandwidth.zstd;

import com.github.luben.zstd.ZstdCompressCtx;
import com.github.luben.zstd.ZstdDecompressCtx;
import io.netty.buffer.ByteBuf;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author USS_Shenzhou
 */
public class Context implements Closeable {
    private final ZstdCompressCtx compressCtx;
    private final ZstdDecompressCtx decompressCtx;

    public Context() {
        compressCtx = new ZstdCompressCtx();
        compressCtx.setLevel(3);
        decompressCtx = new ZstdDecompressCtx();
    }

    public ByteBuffer compress(ByteBuffer raw) {
        return compressCtx.compress(raw);
    }

    public ByteBuffer decompress(ByteBuffer compressed, int originalSize) {
        return decompressCtx.decompress(compressed, originalSize);
    }


    @Override
    public void close() {
        compressCtx.close();
        decompressCtx.close();
    }
}
