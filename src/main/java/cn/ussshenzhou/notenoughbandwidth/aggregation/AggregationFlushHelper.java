package cn.ussshenzhou.notenoughbandwidth.aggregation;

/**
 * @author USS_Shenzhou
 */
public class AggregationFlushHelper {
    public static int getFlushPeriodInMilliseconds() {
        return 20;
    }

    public static int getFlushCountInSeconds() {
        return Math.max(1000 / AggregationFlushHelper.getFlushPeriodInMilliseconds(), 1);
    }

    public static int getThresholdCount1s() {
        return 20 * 2;
    }
}
