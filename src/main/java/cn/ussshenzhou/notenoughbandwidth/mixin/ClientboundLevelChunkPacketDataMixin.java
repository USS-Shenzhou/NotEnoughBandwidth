package cn.ussshenzhou.notenoughbandwidth.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

/**
 * @author USS_Shenzhou
 */
@Mixin(ClientboundLevelChunkPacketData.class)
public class ClientboundLevelChunkPacketDataMixin {

    @Shadow
    @Final
    private static StreamCodec<ByteBuf, Map<Heightmap.Types, long[]>> HEIGHTMAPS_STREAM_CODEC;

    @Shadow
    @Final
    private Map<Heightmap.Types, long[]> heightmaps;

    @Shadow
    @Final
    private byte[] buffer;

    @Shadow
    @Final
    private List<ClientboundLevelChunkPacketData.BlockEntityInfo> blockEntitiesData;

    @Definition(id = "writerIndex", method = "Lnet/minecraft/network/FriendlyByteBuf;writerIndex()I")
    @Definition(id = "buffer", local = @Local(argsOnly = true, ordinal = 0, type = FriendlyByteBuf.class))
    @Definition(id = "capacity", method = "Lnet/minecraft/network/FriendlyByteBuf;capacity()I")
    @Expression("buffer.writerIndex() != buffer.capacity()")
    @ModifyExpressionValue(method = "extractChunkData", at = @At(value = "MIXINEXTRAS:EXPRESSION"))
    private static boolean nebCancelSizeCheck(boolean original) {
        return false;
    }

    @Unique
    private int nebValidBufferSize;

    @Unique
    private ByteBuf nebTempBuf;

    @WrapOperation(method = "<init>(Lnet/minecraft/world/level/chunk/LevelChunk;)V", at = @At(value = "NEW", target = "(Lio/netty/buffer/ByteBuf;)Lnet/minecraft/network/FriendlyByteBuf;"))
    private FriendlyByteBuf nebSaveNewBuffer(ByteBuf source, Operation<FriendlyByteBuf> original) {
        nebTempBuf = source;
        return original.call(source);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/level/chunk/LevelChunk;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/game/ClientboundLevelChunkPacketData;extractChunkData(Lnet/minecraft/network/FriendlyByteBuf;Lnet/minecraft/world/level/chunk/LevelChunk;)V",
                    shift = At.Shift.AFTER))
    private void nebDynamicBufferSize(LevelChunk levelChunk, CallbackInfo ci) {
        nebValidBufferSize = nebTempBuf.readableBytes();
    }

    /**
     * @author USS_Shenzhou
     * @reason No one else should touch it.
     */
    @Overwrite
    public void write(RegistryFriendlyByteBuf output) {
        HEIGHTMAPS_STREAM_CODEC.encode(output, this.heightmaps);
        output.writeVarInt(nebValidBufferSize);
        output.writeBytes(this.buffer, 0, nebValidBufferSize);
        ClientboundLevelChunkPacketData.BlockEntityInfo.LIST_STREAM_CODEC.encode(output, this.blockEntitiesData);
    }
}
