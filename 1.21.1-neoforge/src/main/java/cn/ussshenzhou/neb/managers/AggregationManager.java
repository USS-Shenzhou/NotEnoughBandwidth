package cn.ussshenzhou.neb.managers;

import cn.ussshenzhou.neb.modnetwork.PacketAggregationPacket;
import cn.ussshenzhou.neb.util.AggregationFlushHelper;
import cn.ussshenzhou.neb.util.DefaultChannelPipelineHelper;
import cn.ussshenzhou.util.ResizableCounter;
import com.mojang.logging.LogUtils;
import io.netty.channel.DefaultChannelPipeline;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author USS_Shenzhou
 */
public class AggregationManager {
    private static final ResizableCounter<ResourceLocation> FREQUENCY_COUNTER = new ResizableCounter<>(AggregationFlushHelper.getFlushCountInSeconds());
    private static final HashSet<ResourceLocation> WHITE_LIST = new HashSet<>() {{
        add(ResourceLocation.withDefaultNamespace("level_chunk_with_light"));
        add(ResourceLocation.withDefaultNamespace("custom_payload"));
    }};
    private static final WeakHashMap<Connection, HashMap<ResourceLocation, ArrayList<Packet<?>>>> PACKET_BUFFER = new WeakHashMap<>();
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor();
    private static final ArrayList<ScheduledFuture<?>> TASKS = new ArrayList<>();

    public synchronized static void init() {
        WHITE_LIST.clear();
        PACKET_BUFFER.clear();
        TASKS.forEach(task -> task.cancel(false));
        TASKS.clear();
        TASKS.add(TIMER.scheduleAtFixedRate(AggregationManager::flush, 0, AggregationFlushHelper.getFlushPeriodInMilliseconds(), TimeUnit.MILLISECONDS));
    }

    private static boolean isAggregating(ResourceLocation type) {
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

    public synchronized static boolean aboutToSend(Packet<?> packet, Connection connection) {
        var type = packet.type().id();
        FREQUENCY_COUNTER.increment(type);
        if (isAggregating(type)) {
            PACKET_BUFFER.computeIfAbsent(connection, c -> new HashMap<>())
                    .computeIfAbsent(type, t -> new ArrayList<>())
                    .add(packet);
            return false;
        }
        return true;
    }

    public synchronized static void flush() {
        PACKET_BUFFER.forEach((connection, packetsMap) -> {
            var encoder = DefaultChannelPipelineHelper.getPacketEncoder((DefaultChannelPipeline) connection.channel().pipeline());
            if (encoder == null) {
                LogUtils.getLogger().error("Failed to get PacketEncoder of connection {} {}.", connection.getDirection(), connection.getRemoteAddress());
                return;
            }
            var sendPackets = new HashMap<>(packetsMap);
            connection.send(connection.getSending() == PacketFlow.CLIENTBOUND
                    ? new ClientboundCustomPayloadPacket(new PacketAggregationPacket(sendPackets, encoder.getProtocolInfo()))
                    : new ServerboundCustomPayloadPacket(new PacketAggregationPacket(sendPackets, encoder.getProtocolInfo()))
            );
            packetsMap.clear();
        });
        FREQUENCY_COUNTER.advance(AggregationFlushHelper.getFlushCountInSeconds());
    }
}
