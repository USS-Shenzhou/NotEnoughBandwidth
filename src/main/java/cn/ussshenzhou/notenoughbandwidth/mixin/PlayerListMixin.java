package cn.ussshenzhou.notenoughbandwidth.mixin;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidthConfig;
import net.minecraft.server.players.PlayerList;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @ModifyVariable(method = "setViewDistance", at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/server/players/PlayerList;viewDistance:I",
            opcode = Opcodes.PUTFIELD,
            shift = At.Shift.AFTER
    ), argsOnly = true)
    private int modifyViewDistance(int viewDistance) {
        try {
            return viewDistance + NotEnoughBandwidthConfig.get().dccDistance;
        } catch (IllegalStateException _) {
            return viewDistance;
        }
    }
}