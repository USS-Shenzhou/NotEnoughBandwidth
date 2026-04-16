package cn.ussshenzhou.notenoughbandwidth.zstd;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidthConfig;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;
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
    private static final Cache<Connection, Boolean> CONNECTION_USE_CONTEXT = CacheBuilder.newBuilder()
            .weakKeys()
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
        ZSTD_CONTEXT_CACHE.asMap().entrySet().removeIf(e -> !e.getKey().isConnected());
        try {
            return ZSTD_CONTEXT_CACHE.get(connection, () -> new Context(useContext(connection)));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean useContext(Connection connection) {
        if (connection.getReceiving() == PacketFlow.CLIENTBOUND) {
            return true;
        }
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return true;
        }
        Boolean result = CONNECTION_USE_CONTEXT.getIfPresent(connection);
        if (result != null) {
            return result;
        }
        var to = server.getPlayerList().getPlayers()
                .stream()
                .filter(p -> p.connection.getConnection().equals(connection))
                .findFirst()
                .orElse(null);
        if (to == null) {
            CONNECTION_USE_CONTEXT.put(connection, true);
            return true;
        }
        var uuid = to.getUUID();
        var use = NotEnoughBandwidthConfig.get().playersDoNotUseContext.contains(uuid.toString());
        CONNECTION_USE_CONTEXT.put(connection, use);
        return use;
    }
}
