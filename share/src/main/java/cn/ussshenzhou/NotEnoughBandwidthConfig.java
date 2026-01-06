package cn.ussshenzhou;

import cn.ussshenzhou.config.ConfigHelper;
import cn.ussshenzhou.config.TConfig;

import java.util.HashSet;

/**
 * @author USS_Shenzhou
 */
public class NotEnoughBandwidthConfig implements TConfig {

    public boolean velocityCompat = false;
    public HashSet<String> velocityBlackList = new HashSet<>() {{
        add("minecraft:brand");
        add("minecraft:register");
        add("minecraft:unregister");
        add("velocity:player_info");
        add("bungeecord:main");
    }};
    public boolean debugLog = false;


    public static boolean skipType(String type) {
        var cfg = ConfigHelper.getConfigRead(NotEnoughBandwidthConfig.class);
        return cfg.velocityCompat && cfg.velocityBlackList.contains(type);
    }
}
