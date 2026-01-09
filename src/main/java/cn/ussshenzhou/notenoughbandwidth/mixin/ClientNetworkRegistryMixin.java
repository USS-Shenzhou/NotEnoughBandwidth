package cn.ussshenzhou.notenoughbandwidth.mixin;

import cn.ussshenzhou.notenoughbandwidth.aggregation.AggregationManager;
import cn.ussshenzhou.notenoughbandwidth.indextype.NamespaceIndexManager;
import net.minecraft.network.ConnectionProtocol;
import net.neoforged.neoforge.client.network.registration.ClientNetworkRegistry;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

/**
 * @author USS_Shenzhou
 */
@SuppressWarnings("UnstableApiUsage")
@Mixin(ClientNetworkRegistry.class)
public class ClientNetworkRegistryMixin extends NetworkRegistry {

    // client init
    @Inject(method = "setup", at = @At("TAIL"))
    private static void nebwGetAllPacketIdentifier(CallbackInfo ci) {
        NamespaceIndexManager.init(new ArrayList<>(PAYLOAD_REGISTRATIONS.get(ConnectionProtocol.PLAY).keySet()));
        AggregationManager.init();
    }
}
