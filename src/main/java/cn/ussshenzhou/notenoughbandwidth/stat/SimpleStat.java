package cn.ussshenzhou.notenoughbandwidth.stat;

import cn.ussshenzhou.notenoughbandwidth.util.TimeCounter;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author USS_Shenzhou
 */
public class SimpleStat {
    public static AtomicLong inboundBytesBaked = new AtomicLong(0L);
    public static AtomicLong inboundBytesRaw = new AtomicLong(0L);
    public static AtomicLong outboundBytesBaked = new AtomicLong(0L);
    public static AtomicLong outboundBytesRaw = new AtomicLong(0L);
    public static TimeCounter inboundSpeedBaked = new TimeCounter();
    public static TimeCounter inboundSpeedRaw = new TimeCounter();
    public static TimeCounter outboundSpeedBaked = new TimeCounter();
    public static TimeCounter outboundSpeedRaw = new TimeCounter();

    public static void inBaked(int size) {
        inboundBytesBaked.addAndGet(size);
        inboundSpeedBaked.put(size);
    }

    public static void inRaw(int size) {
        inboundBytesRaw.addAndGet(size);
        inboundSpeedRaw.put(size);
    }

    public static void outBaked(int size) {
        outboundBytesBaked.addAndGet(size);
        outboundSpeedBaked.put(size);
    }

    public static void outRaw(int size) {
        outboundBytesRaw.addAndGet(size);
        outboundSpeedRaw.put(size);
    }
}
