package cn.ussshenzhou.notenoughbandwidth.stat;

/**
 * @author USS_Shenzhou
 */
public class SimpleStatManager {
    public static final SimpleStatData LOCAL = new SimpleStatData();

    public static void inBaked(int size) {
        LOCAL.inboundBytesBaked().addAndGet(size);
        LOCAL.inboundSpeedBaked().put(size);
    }

    public static void inRaw(int size) {
        LOCAL.inboundBytesRaw().addAndGet(size);
        LOCAL.inboundSpeedRaw().put(size);
    }

    public static void outBaked(int size) {
        LOCAL.outboundBytesBaked().addAndGet(size);
        LOCAL.outboundSpeedBaked().put(size);
    }

    public static void outRaw(int size) {
        LOCAL.outboundBytesRaw().addAndGet(size);
        LOCAL.outboundSpeedRaw().put(size);
    }

    public static long inboundBytesBakedServer;
    public static long inboundBytesRawServer;
    public static long outboundBytesBakedServer;
    public static long outboundBytesRawServer;
    public static double inboundSpeedBakedServer;
    public static double inboundSpeedRawServer;
    public static double outboundSpeedBakedServer;
    public static double outboundSpeedRawServer;
}
