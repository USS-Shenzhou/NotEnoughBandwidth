package cn.ussshenzhou.notenoughbandwidth.mixin;

import cn.ussshenzhou.notenoughbandwidth.managers.PacketAggregationManager;
import cn.ussshenzhou.notenoughbandwidth.modnetwork.PacketAggregationPacket;
import com.mojang.logging.LogUtils;
import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.neoforged.neoforge.network.payload.MinecraftRegisterPayload;
import net.neoforged.neoforge.network.payload.MinecraftUnregisterPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

/**
 * @author USS_Shenzhou
 */
@Mixin(value = Connection.class, priority = 1)
public abstract class ConnectionMixin {

    @Shadow
    @Nullable
    private volatile PacketListener packetListener;

    @Shadow
    public abstract void send(Packet<?> packet, @org.jetbrains.annotations.Nullable PacketSendListener listener, boolean flush);

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;Z)V", at = @At("HEAD"), cancellable = true)
    private void nebwPacketAggregate(Packet<?> packet, PacketSendListener listener, boolean flush, CallbackInfo ci) {
        if (this.packetListener != null && this.packetListener.protocol() != ConnectionProtocol.PLAY) {
            return;
        }
        if (packet instanceof BundlePacket<?> bundlePacket) {
            bundlePacket.subPackets().forEach(p -> this.send(p, listener, flush));
            ci.cancel();
            return;
        }
        if (packet instanceof ServerboundCustomPayloadPacket(CustomPacketPayload payload) && payload instanceof PacketAggregationPacket) {
            return;
        }
        if (packet instanceof ClientboundCustomPayloadPacket(CustomPacketPayload payload) && payload instanceof PacketAggregationPacket) {
            return;
        }
        if (!PacketAggregationManager.aboutToSend(packet, (Connection) (Object) this)) {
            ci.cancel();
            return;
        }
    }
}
