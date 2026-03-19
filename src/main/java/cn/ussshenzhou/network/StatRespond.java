package cn.ussshenzhou.network;

import cn.ussshenzhou.notenoughbandwidth.ModConstants;
import cn.ussshenzhou.notenoughbandwidth.stat.SimpleStatManager;
import cn.ussshenzhou.notenoughbandwidth.util.TimeCounter;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author USS_Shenzhou
 */
public class StatRespond implements CustomPacketPayload {
    public static final Type<StatRespond> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "stat_resp"));
    public static final StreamCodec<ByteBuf, StatRespond> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public StatRespond decode(ByteBuf buf) {
            long inboundBytesBaked = ByteBufCodecs.VAR_LONG.decode(buf);
            long inboundBytesRaw = ByteBufCodecs.VAR_LONG.decode(buf);
            long outboundBytesBaked = ByteBufCodecs.VAR_LONG.decode(buf);
            long outboundBytesRaw = ByteBufCodecs.VAR_LONG.decode(buf);
            double inboundSpeedBaked = ByteBufCodecs.DOUBLE.decode(buf);
            double inboundSpeedRaw = ByteBufCodecs.DOUBLE.decode(buf);
            double outboundSpeedBaked = ByteBufCodecs.DOUBLE.decode(buf);
            double outboundSpeedRa = ByteBufCodecs.DOUBLE.decode(buf);
            return new StatRespond(inboundBytesBaked, inboundBytesRaw, outboundBytesBaked, outboundBytesRaw, inboundSpeedBaked, inboundSpeedRaw, outboundSpeedBaked, outboundSpeedRa);
        }

        @Override
        public void encode(ByteBuf buf, StatRespond value) {
            ByteBufCodecs.VAR_LONG.encode(buf, value.inboundBytesBaked);
            ByteBufCodecs.VAR_LONG.encode(buf, value.inboundBytesRaw);
            ByteBufCodecs.VAR_LONG.encode(buf, value.outboundBytesBaked);
            ByteBufCodecs.VAR_LONG.encode(buf, value.outboundBytesRaw);
            ByteBufCodecs.DOUBLE.encode(buf, value.inboundSpeedBaked);
            ByteBufCodecs.DOUBLE.encode(buf, value.inboundSpeedRaw);
            ByteBufCodecs.DOUBLE.encode(buf, value.outboundSpeedBaked);
            ByteBufCodecs.DOUBLE.encode(buf, value.outboundSpeedRa);
        }
    };

    public final long inboundBytesBaked;
    public final long inboundBytesRaw;
    public final long outboundBytesBaked;
    public final long outboundBytesRaw;
    public final double inboundSpeedBaked;
    public final double inboundSpeedRaw;
    public final double outboundSpeedBaked;
    public final double outboundSpeedRa;


    public StatRespond(long inboundBytesBaked, long inboundBytesRaw, long outboundBytesBaked, long outboundBytesRaw, double inboundSpeedBaked, double inboundSpeedRaw, double outboundSpeedBaked, double outboundSpeedRa) {
        this.inboundBytesBaked = inboundBytesBaked;
        this.inboundBytesRaw = inboundBytesRaw;
        this.outboundBytesBaked = outboundBytesBaked;
        this.outboundBytesRaw = outboundBytesRaw;
        this.inboundSpeedBaked = inboundSpeedBaked;
        this.inboundSpeedRaw = inboundSpeedRaw;
        this.outboundSpeedBaked = outboundSpeedBaked;
        this.outboundSpeedRa = outboundSpeedRa;
    }

    public void handle(IPayloadContext context) {
        SimpleStatManager.inboundBytesBakedServer = inboundBytesBaked;
        SimpleStatManager.inboundBytesRawServer = inboundBytesRaw;
        SimpleStatManager.outboundBytesBakedServer = outboundBytesBaked;
        SimpleStatManager.outboundBytesRawServer = outboundBytesRaw;
        SimpleStatManager.inboundSpeedBakedServer = inboundSpeedBaked;
        SimpleStatManager.inboundSpeedRawServer = inboundSpeedRaw;
        SimpleStatManager.outboundSpeedBakedServer = outboundSpeedBaked;
        SimpleStatManager.outboundSpeedRawServer = outboundSpeedRa;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
