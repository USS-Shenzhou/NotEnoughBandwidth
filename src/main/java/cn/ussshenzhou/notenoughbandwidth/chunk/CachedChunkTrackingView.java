package cn.ussshenzhou.notenoughbandwidth.chunk;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidthConfig;
import cn.ussshenzhou.notenoughbandwidth.config.ConfigHelper;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A {@link ChunkTrackingView} implementation that adds a lightweight,
 * time-based cache on top of a primary ("major") tracking view.
 *
 * <p>This cache is used to temporarily retain chunks that have recently
 * left the major view, in order to reduce frequent enter/leave churn when
 * the player moves near view boundaries.</p>
 *
 * <p>A cached chunk will be evicted if <strong>any</strong> of the following
 * conditions is met:</p>
 * <ul>
 *   <li>It has not been within the major view boundary for a configured
 *       amount of time (timeout).</li>
 *   <li>It is farther than the allowed cache distance from the current
 *       view center.</li>
 *   <li>The cache exceeds the configured size limit, in which case the
 *       oldest cached chunks are removed first.</li>
 * </ul>
 */
public class CachedChunkTrackingView implements ChunkTrackingView {
    private static final long NO_CACHE = -1;

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * The primary {@link ChunkTrackingView.Positioned} that represents
     * the authoritative chunk visibility range.
     *
     * <p>All chunks contained in this view are considered visible regardless
     * of the cache state.</p>
     */
    private ChunkTrackingView.Positioned major;

    /**
     * A cache mapping packed {@link ChunkPos} values to the timestamp (in millis)
     * when they were last visible.
     *
     * <p>The map preserves insertion order, allowing efficient eviction of
     * the oldest cached entries when size or timeout constraints are exceeded.</p>
     *
     * <p>The default return value is {@link #NO_CACHE}, allowing fast
     * existence checks without boxing.</p>
     *
     * <p><b>Note:</b> This could potentially be replaced by a value-record
     * representation once Project Valhalla becomes available.</p>
     */
    private final Long2LongLinkedOpenHashMap cache = new Long2LongLinkedOpenHashMap();

    public CachedChunkTrackingView(ChunkTrackingView.Positioned major) {
        this.major = major;
        cache.defaultReturnValue(NO_CACHE);
    }

    @Override
    public boolean contains(int x, int z, boolean includeNeighbors) {
        // FIXME: Investigate how 'includeNeighbors' will affect the check.
        return major.contains(x, z, includeNeighbors) || cache.containsKey(ChunkPos.pack(x, z));
    }

    @Override
    public void forEach(@NotNull Consumer<ChunkPos> consumer) {
        major.forEach(consumer);

        LongIterator cache = this.cache.keySet().iterator();
        while (cache.hasNext()) {
            consumer.accept(ChunkPos.unpack(cache.nextLong()));
        }
    }

    /**
     * Updates the chunk tracking view for a player, applying cached tracking
     * semantics when possible.
     *
     * <p>If the player's view center or view distance changes, a new
     * {@link ChunkTrackingView.Positioned} is created and synchronized to
     * the client.</p>
     *
     * <p>If the current tracking view already supports caching, it will be
     * updated in-place; otherwise, a new {@link CachedChunkTrackingView} is
     * created and installed.</p>
     *
     * @param player             the player whose chunk tracking is being updated
     * @param playerViewDistance the player's view distance
     * @param onEnter            callback invoked when a chunk becomes visible
     * @param onLeave            callback invoked when a chunk is no longer tracked
     */
    public static void onUpdateChunkTracking(ServerPlayer player, int playerViewDistance, Consumer<ChunkPos> onEnter, Consumer<ChunkPos> onLeave) {
        ChunkTrackingView currentTrackingView = player.getChunkTrackingView();
        ChunkPos playerChunkPosition = player.chunkPosition();

        // Compute new ChunkTrackingView.Positioned instance and sync it to client if necessary.
        ChunkTrackingView.Positioned nextPositioned = null;
        ChunkTrackingView.Positioned lastPositioned = switch (currentTrackingView) {
            case CachedChunkTrackingView cachedView -> cachedView.major;
            case ChunkTrackingView.Positioned p -> p;
            default -> null;
        };
        if (lastPositioned == null || !lastPositioned.center().equals(playerChunkPosition) || lastPositioned.viewDistance() != playerViewDistance) {
            nextPositioned = new ChunkTrackingView.Positioned(playerChunkPosition, playerViewDistance);
            player.connection.send(new ClientboundSetChunkCacheCenterPacket(playerChunkPosition.x(), playerChunkPosition.z()));
        }

        // Use an in-place tick operation on CachedChunkTrackingView if possible, otherwise, create a new CachedChunkTrackingView.
        if (currentTrackingView instanceof CachedChunkTrackingView cachedView) {
            cachedView.tick(player, Objects.requireNonNullElse(nextPositioned, cachedView.major), onEnter, onLeave);
        } else if (nextPositioned != null) {
            CachedChunkTrackingView cachedView = new CachedChunkTrackingView(nextPositioned);
            ChunkTrackingView.difference(currentTrackingView, cachedView, onEnter, onLeave);

            player.setChunkTrackingView(cachedView);
        }
    }

    private void tick(ServerPlayer player, ChunkTrackingView.Positioned next, Consumer<ChunkPos> onEnter, Consumer<ChunkPos> onLeave) {
        long now = System.currentTimeMillis();
        var cfg = ConfigHelper.getConfigRead(NotEnoughBandwidthConfig.class);
        int chunkCacheBufferSize = cfg.dccSizeLimit;
        int chunkCacheDistance = cfg.dccDistance;
        int chunkCacheDistanceSquared = chunkCacheDistance * chunkCacheDistance;
        long chunkCacheTimeout = TimeUnit.SECONDS.toMillis(cfg.dccTimeout);

        if (!major.equals(next)) {
            // Update chunk tracking view.
            // 1. For newly-visible chunks,
            //    1) If they're inside cache view, remove them from cache view as it will be in major view.
            //    2) If not, call onEnter.
            // 2. For newly-invisible chunks, if they are within cache distance, push them into cache.
            ChunkTrackingView.difference(major, next, chunkPos -> {
                if (cache.remove(chunkPos.pack()) == NO_CACHE) {
                    onEnter.accept(chunkPos);
                    LOGGER.debug("Cache miss at {} in {}'s chunk cache.", chunkPos, player.getPlainTextName());
                } else {
                    LOGGER.debug("Cache hit at {} in {}'s chunk cache.", chunkPos, player.getPlainTextName());
                }
            }, chunkPos -> {
                if (next.center().distanceSquared(chunkPos) <= chunkCacheDistanceSquared) {
                    cache.put(chunkPos.pack(), now);
                }
            });

            // Remove all chunks that are too far from users.
            enumerate((pos, _) -> {
                if (next.center().distanceSquared(pos) > chunkCacheDistanceSquared) {
                    LOGGER.debug("Remove {} from {}'s chunk cache: too far away.", ChunkPos.unpack(pos), player.getPlainTextName());
                    return CacheConsumer.REMOVE;
                }
                return CacheConsumer.CONTINUE;
            });
        }

        // Remove legacy cache.
        enumerate((pos, time) -> {
            boolean legacy = time <= now - chunkCacheTimeout;
            if (legacy || cache.size() >= chunkCacheBufferSize) {
                ChunkPos chunkPos = ChunkPos.unpack(pos);
                onLeave.accept(chunkPos);
                LOGGER.debug("Remove {} from {}'s chunk cache: {}", chunkPos, player.getPlainTextName(), legacy ? "timeout" : "buffer is full");
                return CacheConsumer.REMOVE;
            } else {
                return CacheConsumer.STOP;
            }
        });

        major = next;
    }

    @FunctionalInterface
    private interface CacheConsumer {
        byte CONTINUE = 0, REMOVE = 1, STOP = 2;

        @MagicConstant(flags = {CONTINUE, REMOVE, STOP})
        byte accept(long pos, long time);
    }

    private void enumerate(CacheConsumer consumer) {
        ObjectIterator<Long2LongMap.Entry> iterator = Long2LongMaps.fastIterator(cache);
        while (iterator.hasNext()) {
            Long2LongMap.Entry entry = iterator.next();

            byte v = consumer.accept(entry.getLongKey(), entry.getLongValue());
            if ((v & CacheConsumer.REMOVE) != 0) {
                iterator.remove();
            }
            if ((v & CacheConsumer.STOP) != 0) {
                return;
            }
        }
    }
}
