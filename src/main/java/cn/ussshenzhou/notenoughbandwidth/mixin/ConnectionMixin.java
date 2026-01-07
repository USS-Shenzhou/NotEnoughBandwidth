package cn.ussshenzhou.notenoughbandwidth.mixin;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidthConfig;
import cn.ussshenzhou.notenoughbandwidth.aggregation.AggregationManager;
import cn.ussshenzhou.notenoughbandwidth.aggregation.PacketAggregationPacket;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author USS_Shenzhou
 */
@Mixin(value = Connection.class, priority = 1)
public abstract class ConnectionMixin {

    @Shadow
    @Nullable
    private volatile PacketListener packetListener;

    @Shadow
    public abstract void send(Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush);

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"), cancellable = true)
    private void nebwPacketAggregate(Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        if (this.packetListener != null && this.packetListener.protocol() != ConnectionProtocol.PLAY) {
            return;
        }
        if (NotEnoughBandwidthConfig.skipType(packet.type().id().toString())) {
            return;
        }
        if (packet instanceof BundlePacket<?> bundlePacket) {
            //de-bundle
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
        if (!AggregationManager.aboutToSend(packet, (Connection) (Object) this)) {
            ci.cancel();
            return;
        }
    }
}
