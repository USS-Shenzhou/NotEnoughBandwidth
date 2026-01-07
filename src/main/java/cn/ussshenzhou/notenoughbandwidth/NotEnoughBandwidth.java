package cn.ussshenzhou.notenoughbandwidth;

import cn.ussshenzhou.notenoughbandwidth.config.ConfigHelper;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * @author USS_Shenzhou
 */
@Mod(ModConstants.MOD_ID)
public class NotEnoughBandwidth {
    private static final Logger LOGGER = LogUtils.getLogger();

    public NotEnoughBandwidth(IEventBus modEventBus, ModContainer modContainer) {
        ConfigHelper.loadConfig(new NotEnoughBandwidthConfig());
    }


}
