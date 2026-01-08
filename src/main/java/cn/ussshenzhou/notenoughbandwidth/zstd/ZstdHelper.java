package cn.ussshenzhou.notenoughbandwidth.zstd;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.Connection;

import java.util.concurrent.ExecutionException;

/**
 * @author USS_Shenzhou
 */
public class ZstdHelper {

    private static final Cache<Connection, Context> ZSTD_CONTEXT_CACHE = CacheBuilder.newBuilder()
            .weakKeys()
            .removalListener((RemovalListener<Connection, Context>) notification -> {
                if (notification.getValue() != null) {
                    notification.getValue().close();
                }
            })
            .build();

    public static ByteBuf compress(Connection connection, ByteBuf raw) {
        return Unpooled.wrappedBuffer(get(connection).compress(raw.nioBuffer()));
    }

    public static ByteBuf decompress(Connection connection, ByteBuf compressed, int originalSize) {
        if (compressed.isDirect()) {
            return Unpooled.wrappedBuffer(get(connection).decompress(compressed.nioBuffer(), originalSize));
        } else {
            var directBuf = Unpooled.directBuffer(compressed.readableBytes());
            compressed.getBytes(compressed.readerIndex(), directBuf);
            var decompressed = Unpooled.wrappedBuffer(get(connection).decompress(directBuf.nioBuffer(), originalSize));
            directBuf.release();
            return decompressed;
        }
    }

    private static Context get(Connection connection) {
        try {
            return ZSTD_CONTEXT_CACHE.get(connection, Context::new);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
