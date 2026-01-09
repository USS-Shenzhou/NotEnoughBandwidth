package cn.ussshenzhou.notenoughbandwidth;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author USS_Shenzhou
 */
public class Statistic {

    public static final AtomicLong OUTBOUND_RAW = new AtomicLong();
    public static final AtomicLong OUTBOUND_BAKED = new AtomicLong();
}
