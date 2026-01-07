package cn.ussshenzhou.notenoughbandwidth.mixin;

import cn.ussshenzhou.notenoughbandwidth.aggregation.AggregationManager;
import cn.ussshenzhou.notenoughbandwidth.indextype.NamespaceIndexManager;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.configuration.ClientConfigurationPacketListener;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.neoforged.neoforge.network.payload.ModdedNetworkQueryComponent;
import net.neoforged.neoforge.network.registration.NetworkPayloadSetup;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Set;

/**
 * @author USS_Shenzhou
 */
@SuppressWarnings("UnstableApiUsage")
@Mixin(NetworkRegistry.class)
public class NetworkRegistryMixin {

    @Inject(method = "initializeNeoForgeConnection(Lnet/minecraft/network/protocol/configuration/ServerConfigurationPacketListener;Ljava/util/Map;)V", at = @At("TAIL"))
    private static void nebwGetAllPacketIdentifier(ServerConfigurationPacketListener listener, Map<ConnectionProtocol, Set<ModdedNetworkQueryComponent>> clientChannels, CallbackInfo ci, @Local NetworkPayloadSetup setup) {
        NamespaceIndexManager.init(setup);
        AggregationManager.init();
    }
}
