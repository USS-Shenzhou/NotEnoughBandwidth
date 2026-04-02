package cn.ussshenzhou.notenoughbandwidth.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * @author USS_Shenzhou
 */
@Mixin(ClientboundLevelChunkPacketData.class)
public class ClientboundLevelChunkPacketDataMixin {

    @Definition(id = "writerIndex", method = "Lnet/minecraft/network/FriendlyByteBuf;writerIndex()I")
    @Definition(id = "buffer", local = @Local(argsOnly = true, ordinal = 0, type = FriendlyByteBuf.class))
    @Definition(id = "capacity", method = "Lnet/minecraft/network/FriendlyByteBuf;capacity()I")
    @Expression("buffer.writerIndex() != buffer.capacity()")
    @ModifyExpressionValue(method = "extractChunkData", at = @At(value = "MIXINEXTRAS:EXPRESSION"))
    private static boolean nebCancelSizeCheck(boolean original) {
        return false;
    }
}
