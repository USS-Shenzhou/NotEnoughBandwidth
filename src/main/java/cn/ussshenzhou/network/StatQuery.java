package cn.ussshenzhou.network;

import cn.ussshenzhou.notenoughbandwidth.ModConstants;
import cn.ussshenzhou.notenoughbandwidth.stat.SimpleStatManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static cn.ussshenzhou.notenoughbandwidth.stat.SimpleStatManager.LOCAL;

/**
 * @author USS_Shenzhou
 */
public class StatQuery implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<StatQuery> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "stat_query"));
    public static final StreamCodec<ByteBuf, StatQuery> STREAM_CODEC = StreamCodec.unit(new StatQuery());

    public StatQuery() {
    }

    public StatQuery(FriendlyByteBuf buf) {
    }

    public void handle(IPayloadContext context) {
        if (context.player() instanceof ServerPlayer serverPlayer && context.player().permissions().hasPermission(Permissions.COMMANDS_MODERATOR)) {
            PacketDistributor.sendToPlayer(serverPlayer, new StatRespond(
                    LOCAL.inboundBytesBaked().get(),
                    LOCAL.inboundBytesRaw().get(),
                    LOCAL.outboundBytesBaked().get(),
                    LOCAL.outboundBytesRaw().get(),
                    LOCAL.inboundSpeedBaked().averageIn1s(),
                    LOCAL.inboundSpeedRaw().averageIn1s(),
                    LOCAL.outboundSpeedBaked().averageIn1s(),
                    LOCAL.outboundSpeedRaw().averageIn1s()
            ));
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
