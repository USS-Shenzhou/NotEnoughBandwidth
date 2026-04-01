package cn.ussshenzhou.notenoughbandwidth.mixin;

import cn.ussshenzhou.notenoughbandwidth.chunk.BitStorage3DgmHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.BitStorage;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PalettedContainer.Data.class)
public class PalettedContainerDataMixin {

    @Shadow
    @Final
    private BitStorage storage;

    @Redirect(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/FriendlyByteBuf;writeFixedSizeLongArray([J)Lnet/minecraft/network/FriendlyByteBuf;"))
    private FriendlyByteBuf neb3DgmEncode(FriendlyByteBuf instance, long[] longs) {
        //if (this.storage instanceof SimpleBitStorage simpleBitStorage) {
        //    instance.writeByteArray(BitStorage3DgmHelper.write(simpleBitStorage));
        //} else {
            instance.writeFixedSizeLongArray(this.storage.getRaw());
        //}
        return instance;
    }
}
