package cn.ussshenzhou.notenoughbandwidth.util;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.util.Util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author USS_Shenzhou
 */
public class TimeCounter {
    private final Long2IntOpenHashMap container = new Long2IntOpenHashMap();
    private final int windowsSizeMs;

    public TimeCounter(int windowsSizeMs) {
        this.windowsSizeMs = windowsSizeMs;
    }

    public TimeCounter() {
        this(2000);
    }

    private synchronized void update() {
        long now = Util.getMillis();
        container.keySet().removeIf(then -> now - then > windowsSizeMs);
    }

    public synchronized void put(int value) {
        update();
        container.put(Util.getMillis(), value);
    }

    public synchronized double averageIn1s() {
        return container.values().intStream().sum() / (double) windowsSizeMs * 1000;
    }
}
