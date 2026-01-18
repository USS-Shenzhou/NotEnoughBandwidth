package cn.ussshenzhou.notenoughbandwidth.zstd;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidthConfig;
import com.github.luben.zstd.EndDirective;
import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdCompressCtx;
import com.github.luben.zstd.ZstdDecompressCtx;
import com.sun.management.HotSpotDiagnosticMXBean;
import sun.misc.Unsafe;

import java.io.Closeable;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
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
        compressCtx.setContentSize(false);
        compressCtx.setMagicless(true);
        compressCtx.setWindowLog(NotEnoughBandwidthConfig.get().getContextLevel());
        decompressCtx = new ZstdDecompressCtx();
        decompressCtx.setMagicless(true);
    }

    public ByteBuffer compress(ByteBuffer raw) {
        int maxDstSize = (int) Zstd.compressBound(raw.remaining());
        var dst = ByteBuffer.allocateDirect(maxDstSize);
        compressCtx.compressDirectByteBufferStream(dst, raw, EndDirective.FLUSH);
        dst.flip();
        return dst;
        //return compressCtx.compress(raw);
    }

    public ByteBuffer decompress(ByteBuffer compressed, int originalSize) {
        var dst = ByteBuffer.allocateDirect(originalSize);
        decompressCtx.decompressDirectByteBufferStream(dst, compressed);
        dst.flip();
        return dst;
        //return decompressCtx.decompress(compressed, originalSize);
    }


    @Override
    public void close() {
        compressCtx.close();
        decompressCtx.close();
    }

    //private static int getBestWindowLog() {
    //    long maxDirectMemory = getMaxDirectMemory();
    //}

    //private static long getMaxDirectMemory() {
    //    long direct = Long.parseLong(ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class).getVMOption("MaxDirectMemorySize").getValue());
    //    if (direct == 0) {
    //        direct = Runtime.getRuntime().maxMemory();
    //    }
    //    return direct;
    //}
}
