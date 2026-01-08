package cn.ussshenzhou.notenoughbandwidth.aggregation;

import cn.ussshenzhou.notenoughbandwidth.util.DefaultChannelPipelineHelper;
import cn.ussshenzhou.notenoughbandwidth.util.ResizableCounter;
import com.mojang.logging.LogUtils;
import io.netty.channel.DefaultChannelPipeline;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.Identifier;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author USS_Shenzhou
 */
public class AggregationManager {
    private static final ResizableCounter<Identifier> FREQUENCY_COUNTER = new ResizableCounter<>(AggregationFlushHelper.getFlushCountInSeconds());
    private static final HashSet<Identifier> WHITE_LIST = new HashSet<>() {{
        add(Identifier.withDefaultNamespace("level_chunk_with_light"));
        add(Identifier.withDefaultNamespace("custom_payload"));
    }};
    private static final WeakHashMap<Connection, ArrayList<AggregatedEncodePacket>> PACKET_BUFFER = new WeakHashMap<>();
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor();
    private static final ArrayList<ScheduledFuture<?>> TASKS = new ArrayList<>();

    public synchronized static void init() {
        WHITE_LIST.clear();
        PACKET_BUFFER.clear();
        TASKS.forEach(task -> task.cancel(false));
        TASKS.clear();
        TASKS.add(TIMER.scheduleAtFixedRate(AggregationManager::flush, 0, AggregationFlushHelper.getFlushPeriodInMilliseconds(), TimeUnit.MILLISECONDS));
    }

    private static boolean isAggregating(Identifier type) {
        if (WHITE_LIST.contains(type)) {
            return true;
        }
        if (FREQUENCY_COUNTER.count(type) >= AggregationFlushHelper.getThresholdCount1s()) {
            FREQUENCY_COUNTER.remove(type);
            WHITE_LIST.add(type);
            LogUtils.getLogger().debug("Aggregating packets of {}", type);
            return true;
        }
        return false;
    }

    public synchronized static boolean takeOver(Packet<?> packet, Connection connection) {
        var type = AggregatedEncodePacket.getTrueType(packet);
        FREQUENCY_COUNTER.increment(type);
        if (isAggregating(type)) {
            PACKET_BUFFER.computeIfAbsent(connection, _ -> new ArrayList<>()).add(new AggregatedEncodePacket(packet, type));
            return true;
        }
        return false;
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
            var flow = connection.getSending();
            connection.send(flow == PacketFlow.CLIENTBOUND
                    ? new ClientboundCustomPayloadPacket(new PacketAggregationPacket(sendPackets, encoder.getProtocolInfo(), flow))
                    : new ServerboundCustomPayloadPacket(new PacketAggregationPacket(sendPackets, encoder.getProtocolInfo(), flow))
            );
            packets.clear();
        });
        FREQUENCY_COUNTER.advance(AggregationFlushHelper.getFlushCountInSeconds());
    }
}
