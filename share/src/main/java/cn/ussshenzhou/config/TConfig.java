package cn.ussshenzhou.config;

/**
 * @author USS_Shenzhou
 */
public interface TConfig {
    default String getChildDirName() {
        return "";
    }
}
