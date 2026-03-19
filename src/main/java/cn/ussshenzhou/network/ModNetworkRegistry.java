package cn.ussshenzhou.network;

import cn.ussshenzhou.notenoughbandwidth.ModConstants;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * @author USS_Shenzhou
 */
@EventBusSubscriber
public class ModNetworkRegistry {

    @SubscribeEvent
    public static void networkPacketRegistry(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(ModConstants.MOD_ID);

        registrar.playToServer(StatQuery.TYPE, StreamCodec.ofMember((_, _) -> {
                }, StatQuery::new),
                StatQuery::handle);
        registrar.playToClient(StatRespond.TYPE, StatRespond.STREAM_CODEC, StatRespond::handle);
    }
}
