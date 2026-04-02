package cn.ussshenzhou.notenoughbandwidth.mixin;

import cn.ussshenzhou.notenoughbandwidth.chunk.BitStorage3DgmHelper;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author USS_Shenzhou
 */
@Mixin(PalettedContainer.class)
public class PalettedContainerMixin {

    @SuppressWarnings("rawtypes")
    @Redirect(method = "read", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/FriendlyByteBuf;readFixedSizeLongArray([J)[J"))
    private long[] neb3DgmEncode(FriendlyByteBuf instance, long[] longs, @Local PalettedContainer.Data newData) {
        if (newData.storage() instanceof SimpleBitStorage simpleBitStorage && simpleBitStorage.size == 4096) {
            BitStorage3DgmHelper.read(simpleBitStorage, instance, longs);
        } else {
            instance.readFixedSizeLongArray(longs);
        }
        return longs;
    }

}
