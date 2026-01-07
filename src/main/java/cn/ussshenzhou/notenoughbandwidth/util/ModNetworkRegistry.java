package cn.ussshenzhou.notenoughbandwidth.util;

import cn.ussshenzhou.notenoughbandwidth.ModConstants;
import cn.ussshenzhou.notenoughbandwidth.aggregation.PacketAggregationPacket;
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

        registrar.commonBidirectional(PacketAggregationPacket.TYPE, StreamCodec.ofMember(PacketAggregationPacket::encode, PacketAggregationPacket::new), PacketAggregationPacket::handler, PacketAggregationPacket::handler);
    }
}
