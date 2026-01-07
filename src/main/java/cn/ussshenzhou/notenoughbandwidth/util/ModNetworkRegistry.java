package cn.ussshenzhou.notenoughbandwidth.util;

import cn.ussshenzhou.notenoughbandwidth.ModConstants;
import cn.ussshenzhou.notenoughbandwidth.aggregation.PacketAggregationPacket;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.HandlerThread;

/**
 * @author USS_Shenzhou
 */
@EventBusSubscriber
public class ModNetworkRegistry {

    @SubscribeEvent
    public static void networkPacketRegistry(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(ModConstants.MOD_ID).executesOn(HandlerThread.NETWORK);

        registrar.commonBidirectional(PacketAggregationPacket.TYPE, StreamCodec.ofMember(PacketAggregationPacket::encode, PacketAggregationPacket::new), PacketAggregationPacket::handler, PacketAggregationPacket::handler);

        registrar.commonBidirectional(TestPacket.TYPE,
                StreamCodec.ofMember(TestPacket::encode, TestPacket::new),
                TestPacket::handler, TestPacket::handler
        );
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Pre event) {
        PacketDistributor.sendToAllPlayers(new TestPacket());
    }

    public static class TestPacket implements CustomPacketPayload {
        public static final Type<TestPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "test_packet"));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public void encode(FriendlyByteBuf buffer) {

        }

        public TestPacket() {
        }

        public TestPacket(FriendlyByteBuf buffer) {
        }

        public void handler(IPayloadContext context) {
            LogUtils.getLogger().info("1");
        }
    }
}
