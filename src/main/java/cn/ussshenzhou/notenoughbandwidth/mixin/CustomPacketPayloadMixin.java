package cn.ussshenzhou.notenoughbandwidth.mixin;

import cn.ussshenzhou.notenoughbandwidth.NotEnoughBandwidthConfig;
import cn.ussshenzhou.notenoughbandwidth.indextype.CustomPacketPrefixHelper;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author USS_Shenzhou
 */
@Mixin(targets = "net.minecraft.network.protocol.common.custom.CustomPacketPayload$1")
public class CustomPacketPayloadMixin {

    @SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
    @Shadow
    @Final
    ConnectionProtocol val$protocol;

    @Redirect(method = "writeCap(Lnet/minecraft/network/FriendlyByteBuf;Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload$Type;Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/FriendlyByteBuf;writeIdentifier(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/network/FriendlyByteBuf;"))
    private FriendlyByteBuf nebwIndexedHeaderEncode(FriendlyByteBuf buf, Identifier Identifier) {
        if (NotEnoughBandwidthConfig.skipType(Identifier.toString()) || val$protocol != ConnectionProtocol.PLAY) {
            buf.writeIdentifier(Identifier);
            return buf;
        }
        CustomPacketPrefixHelper.get()
                .index(Identifier)
                .save(buf);
        return buf;
    }

    @Redirect(method = "decode(Lnet/minecraft/network/FriendlyByteBuf;)Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/FriendlyByteBuf;readIdentifier()Lnet/minecraft/resources/Identifier;"))
    private Identifier nebwIndexedHeaderDecode(FriendlyByteBuf buf) {
        try {
            var tryRead = new FriendlyByteBuf(buf.retainedDuplicate());
            var tryType = tryRead.readIdentifier();
            if (NotEnoughBandwidthConfig.skipType(tryType.toString())) {
                return buf.readIdentifier();
            }
        } catch (Exception ignored) {
        }
        if (val$protocol != ConnectionProtocol.PLAY) {
            return buf.readIdentifier();
        }
        return CustomPacketPrefixHelper.getType(buf);
    }
}
