package cn.ussshenzhou.notenoughbandwidth.mixin;

import cn.ussshenzhou.notenoughbandwidth.chunk.CachedChunkTrackingView;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;
import org.spongepowered.asm.mixin.*;

/**
 * @author Burning_TNT
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
    @Shadow
    @Final
    private TicketStorage ticketStorage;
    @Shadow
    @Final
    private ServerLevel level;

    @Unique
    private static TicketType TICKET_TYPE;

    /**
     * @author Burning_TNT
     * @reason NEB overwrites original chunk map update strategy.
     */
    @Overwrite
    private void updateChunkTracking(ServerPlayer player) {
        if (player.level() != this.level) {
            return;
        }

        CachedChunkTrackingView.onUpdateChunkTracking(player, getPlayerViewDistance(player), new CachedChunkTrackingView.Context() {
            @Override
            public void startChunkTracking(ChunkPos pos) {
                markChunkPendingToSend(player, pos);
            }

            @Override
            public void stopChunkTracking(ChunkPos pos) {
                dropChunk(player, pos);
            }

            @Override
            public void putTicket(ChunkPos pos, int ticks) {
                TicketType ticketType = TICKET_TYPE;
                if (ticketType == null || ticketType.timeout() != ticks) {
                    ticketType = TICKET_TYPE = new TicketType(ticks, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_CAN_EXPIRE_IF_UNLOADED);
                }
                ticketStorage.addTicketWithRadius(ticketType, pos, 1);
            }
        });
    }

    @Shadow
    protected abstract int getPlayerViewDistance(ServerPlayer player);

    @Shadow
    protected abstract void markChunkPendingToSend(ServerPlayer player, ChunkPos pos);

    @Shadow
    private static void dropChunk(ServerPlayer player, ChunkPos pos) {
    }
}
