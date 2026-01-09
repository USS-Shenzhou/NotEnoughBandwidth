package cn.ussshenzhou.notenoughbandwidth.aggregation;

import cn.ussshenzhou.notenoughbandwidth.util.DefaultChannelPipelineHelper;
import com.mojang.logging.LogUtils;
import io.netty.channel.DefaultChannelPipeline;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.neoforged.fml.util.thread.EffectiveSide;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author USS_Shenzhou
 */
public class AggregationManager {
    private static final WeakHashMap<Connection, ArrayList<AggregatedEncodePacket>> PACKET_BUFFER = new WeakHashMap<>();
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor();
    private static final ArrayList<ScheduledFuture<?>> TASKS = new ArrayList<>();
    private static volatile boolean initialized = false;

    public synchronized static void init() {
        if (EffectiveSide.get().isServer() && initialized) {
            return;
        }
        initialized = false;
        PACKET_BUFFER.clear();
        TASKS.forEach(task -> task.cancel(false));
        TASKS.clear();
        TASKS.add(TIMER.scheduleAtFixedRate(AggregationManager::flush, 0, AggregationFlushHelper.getFlushPeriodInMilliseconds(), TimeUnit.MILLISECONDS));
        initialized = true;
    }

    public synchronized static void takeOver(Packet<?> packet, Connection connection) {
        var type = AggregatedEncodePacket.getTrueType(packet);
        PACKET_BUFFER.computeIfAbsent(connection, _ -> new ArrayList<>()).add(new AggregatedEncodePacket(packet, type));
    }

    public synchronized static void flush() {
        PACKET_BUFFER.forEach((connection, packets) -> {
            var encoder = DefaultChannelPipelineHelper.getPacketEncoder((DefaultChannelPipeline) connection.channel().pipeline());
            if (encoder == null) {
                LogUtils.getLogger().error("Failed to get PacketEncoder of connection {} {}.", connection.getDirection(), connection.getRemoteAddress());
                return;
            }
            if (packets.isEmpty()) {
                return;
            }
            var sendPackets = new ArrayList<>(packets);
            connection.send(connection.getSending() == PacketFlow.CLIENTBOUND
                    ? new ClientboundCustomPayloadPacket(new PacketAggregationPacket(sendPackets, encoder.getProtocolInfo(), connection))
                    : new ServerboundCustomPayloadPacket(new PacketAggregationPacket(sendPackets, encoder.getProtocolInfo(), connection))
            );
            packets.clear();
        });
    }
}
