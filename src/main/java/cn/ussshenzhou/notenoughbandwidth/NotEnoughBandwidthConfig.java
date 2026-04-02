package cn.ussshenzhou.notenoughbandwidth;

import cn.ussshenzhou.notenoughbandwidth.aggregation.PacketAggregationPacket;
import cn.ussshenzhou.notenoughbandwidth.config.TConfig;
import com.google.gson.annotations.Expose;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.payload.*;

import java.util.HashSet;
import java.util.Set;

/**
 * @author USS_Shenzhou
 */
public class NotEnoughBandwidthConfig implements TConfig {

    @Expose(serialize = false, deserialize = false)
    private static NotEnoughBandwidthConfig CACHE;

    @SuppressWarnings("UnstableApiUsage")
    @Expose(serialize = false, deserialize = false)
    private static final Set<String> COMMON_BLOCK_LIST = Set.of(
            "minecraft:finish_configuration",
            PacketAggregationPacket.TYPE.id().toString(),
            "minecraft:login",
            MinecraftRegisterPayload.ID.toString(),
            MinecraftUnregisterPayload.ID.toString(),
            ModdedNetworkQueryPayload.ID.toString(),
            ModdedNetworkPayload.ID.toString(),
            ModdedNetworkSetupFailedPayload.ID.toString(),
            CommonVersionPayload.ID.toString(),
            CommonRegisterPayload.ID.toString());

    @Expose(serialize = false, deserialize = false)
    private static final HashSet<String> BLOCK_LIST = new HashSet<>();

    public boolean optimizeOptional = false;
    public boolean compatibleMode = false;
    public HashSet<String> blackList = new HashSet<>() {{
        add("minecraft:command_suggestion");
        add("minecraft:command_suggestions");
        add("minecraft:commands");
        add("minecraft:chat_command");
        add("minecraft:chat_command_signed");
        add("minecraft:player_info_update");
        add("minecraft:player_info_remove");
    }};
    public boolean debugLog = false;
    public int contextLevel = 23;
    public int dccSizeLimit = 60;
    public int dccDistance = 5;
    public int dccTimeout = 60;

    public static NotEnoughBandwidthConfig get() {
        return CACHE;
    }

    public static boolean skipType(String type) {
        return BLOCK_LIST.contains(type);
    }

    public int getContextLevel() {
        return Mth.clamp(contextLevel, 21, 25);
    }

    @Override
    public void onLoad() {
        CACHE = this;
        BLOCK_LIST.clear();
        BLOCK_LIST.addAll(COMMON_BLOCK_LIST);
        if (compatibleMode) {
            BLOCK_LIST.addAll(blackList);
        }
    }
}
