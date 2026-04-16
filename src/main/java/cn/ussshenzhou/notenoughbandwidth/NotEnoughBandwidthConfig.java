package cn.ussshenzhou.notenoughbandwidth;

import cn.ussshenzhou.notenoughbandwidth.aggregation.PacketAggregationPacket;
import cn.ussshenzhou.notenoughbandwidth.config.ConfigHelper;
import cn.ussshenzhou.notenoughbandwidth.config.TConfig;
import com.google.gson.annotations.Expose;
import com.mojang.logging.LogUtils;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.payload.*;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import java.util.HashSet;
import java.util.regex.Pattern;

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
    public int dccSizeLimit = 60;
    public int dccDistance = 5;
    public int dccTimeout = 60;
    public String maxPacketSize = "4MB";
    public HashSet<String> playersDoNotUseContext = new HashSet<>() {{
        add("00000000-0000-0000-0000-000000000000");
    }};

    @SuppressWarnings("UnstableApiUsage")
    @Expose(serialize = false, deserialize = false)
    public static final HashSet<String> COMMON_BLOCK_LIST = new HashSet<>() {{
        add("minecraft:finish_configuration");
        add(PacketAggregationPacket.TYPE.id().toString());
        add("minecraft:login");
        add(MinecraftRegisterPayload.ID.toString());
        add(MinecraftUnregisterPayload.ID.toString());
        add(ModdedNetworkQueryPayload.ID.toString());
        add(ModdedNetworkPayload.ID.toString());
        add(ModdedNetworkSetupFailedPayload.ID.toString());
        add(CommonVersionPayload.ID.toString());
        add(CommonRegisterPayload.ID.toString());
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

    @Expose(serialize = false, deserialize = false)
    private int maxPacketSizeByte = -1;

    public int getMaxPacketSize() {
        if (maxPacketSizeByte == -1) {
            maxPacketSizeByte = parseByteSize(maxPacketSize);
            int min = parseByteSize("2MB");
            int max = parseByteSize("64MB");
            if (maxPacketSizeByte < min || maxPacketSizeByte > max) {
                LogUtils.getLogger().error("maxPacketSize should be between 2MB and 64MB");
            }
            maxPacketSizeByte = Mth.clamp(maxPacketSizeByte, min, max);
        }
        return maxPacketSizeByte;
    }

    private static int parseByteSize(String s) {
        var matcher = Pattern.compile("^([\\d.]+)\\s*(B|KB|MB)?$", Pattern.CASE_INSENSITIVE).matcher(s.trim());
        if (!matcher.matches()) {
            LogUtils.getLogger().error("NEB: Invalid packet size: {} , use default 4MB instead.", s);
            return parseByteSize("4MB");
        }
        double value = Double.parseDouble(matcher.group(1));
        String unit = matcher.group(2);
        if (unit == null || "B".equalsIgnoreCase(unit)) {
            return (int) value;
        }
        return (int) switch (unit.toUpperCase()) {
            case "KB" -> value * 1024;
            case "MB" -> value * 1024 * 1024;
            default -> {
                LogUtils.getLogger().error("NEB: Invalid packet size: {} , use default 4MB instead.", s);
                yield parseByteSize("4MB");
            }
        };
    }
}
