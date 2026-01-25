package cn.ussshenzhou.notenoughbandwidth;

import cn.ussshenzhou.notenoughbandwidth.aggregation.PacketAggregationPacket;
import cn.ussshenzhou.notenoughbandwidth.config.ConfigHelper;
import cn.ussshenzhou.notenoughbandwidth.config.TConfig;
import com.google.gson.annotations.Expose;
import net.minecraft.util.Mth;

import java.util.HashSet;

/**
 * @author USS_Shenzhou
 */
public class NotEnoughBandwidthConfig implements TConfig {

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

    @Expose(serialize = false, deserialize = false)
    public static final HashSet<String> COMMON_BLOCK_LIST = new HashSet<>() {{
        add("minecraft:finish_configuration");
        add(PacketAggregationPacket.TYPE.id().toString());
        add("minecraft:login");
    }};

    public static NotEnoughBandwidthConfig get() {
        return ConfigHelper.getConfigRead(NotEnoughBandwidthConfig.class);
    }

    public static boolean skipType(String type) {
        var cfg = get();
        return COMMON_BLOCK_LIST.contains(type) || (cfg.compatibleMode && cfg.blackList.contains(type));
    }

    public int getContextLevel() {
        return Mth.clamp(contextLevel, 21, 25);
    }
}
