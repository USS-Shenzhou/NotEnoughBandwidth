package cn.ussshenzhou.notenoughbandwidth.mixin;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidthConfig;
import cn.ussshenzhou.notenoughbandwidth.aggregation.AggregationManager;
import cn.ussshenzhou.notenoughbandwidth.util.PacketUtil;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.local.LocalAddress;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.SocketAddress;

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

    @Shadow
    public abstract SocketAddress getRemoteAddress();

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"), cancellable = true)
    private void nebwPacketAggregate(Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        //only work on play
        if (this.getRemoteAddress() instanceof LocalAddress || this.packetListener == null || this.packetListener.protocol() != ConnectionProtocol.PLAY) {
            return;
        }
        //compatability and avoid infinite loop
        if (NotEnoughBandwidthConfig.skipType(PacketUtil.getTrueType(packet).toString())) {
            //flush to ensure packet order
            AggregationManager.flushConnection((Connection) (Object) this);
            return;
        }
        //de-bundle
        if (packet instanceof BundlePacket<?> bundlePacket) {
            bundlePacket.subPackets().forEach(p -> this.send(p, listener, flush));
            ci.cancel();
            return;
        }
        //take over
        AggregationManager.takeOver(packet, (Connection) (Object) this);
        ci.cancel();
    }
}
