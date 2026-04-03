package cn.ussshenzhou.notenoughbandwidth.config;

/**
 * @author USS_Shenzhou
 */
public interface TConfig {
    default String getChildDirName() {
        return "";
    }
}
