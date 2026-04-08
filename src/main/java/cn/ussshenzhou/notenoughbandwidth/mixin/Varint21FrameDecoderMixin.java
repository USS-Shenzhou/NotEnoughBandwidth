package cn.ussshenzhou.notenoughbandwidth.mixin;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidthConfig;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.sugar.Local;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.Varint21FrameDecoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * @author USS_Shenzhou
 */
@Mixin(Varint21FrameDecoder.class)
public class Varint21FrameDecoderMixin {

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 3))
    private int nebAllowBiggerPacket0(int constant) {
        return 4;
    }

    @ModifyConstant(method = "copyVarint", constant = @Constant(intValue = 3))
    private static int nebAllowBiggerPacket1(int constant) {
        return 4;
    }

    @Definition(id = "length", local = @Local(type = int.class))
    @Expression("length == 0")
    @Inject(method = "decode", at = @At(value = "MIXINEXTRAS:EXPRESSION"))
    private void nebCheckPacketSize(ChannelHandlerContext ctx, ByteBuf in, List<Object> out, CallbackInfo ci, @Local int length) {
        int maxSize = NotEnoughBandwidthConfig.get().getMaxPacketSize();
        if (length > maxSize) {
            throw new EncoderException("NEB: Packet too large: size " + length + " is over " + maxSize);
        }
    }
}
