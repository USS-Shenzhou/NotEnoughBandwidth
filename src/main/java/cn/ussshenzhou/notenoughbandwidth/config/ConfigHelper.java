package cn.ussshenzhou.notenoughbandwidth.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * @author USS_Shenzhou
 */
public class ConfigHelper {
    private static final File CONFIG_DIR = Paths.get("config").toFile();
    private static final ConcurrentHashMap<Class<? extends TConfig>, TConfig> CACHE = new ConcurrentHashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final File UNIVERSAL_CONFIG_DIR = FileUtils.getUserDirectory().toPath().resolve("MinecraftT88Config").toFile();

    private static void checkDir(File dir) {
        if (!dir.isDirectory()) {
            dir.mkdir();
        }
    }

    private static File checkFile(TConfig config, boolean universal) {
        checkDir(universal ? UNIVERSAL_CONFIG_DIR : CONFIG_DIR);
        String configFileName = config.getClass().getSimpleName();
        Path childDir = (universal ? UNIVERSAL_CONFIG_DIR : CONFIG_DIR).toPath().resolve(config.getChildDirName());
        checkDir(childDir.toFile());
        return childDir.resolve(configFileName + ".json").toFile();
    }

    public static void loadConfig(TConfig newInstance) {
        File configFile = checkFile(newInstance, false);
        loadConfigInternal(newInstance, configFile);
    }

    @SuppressWarnings("unchecked")
    public static <T extends TConfig> T getConfigRead(Class<T> configClass) {
        return (T) CACHE.get(configClass);
    }

    @SuppressWarnings("unchecked")
    public static <T extends TConfig> void getConfigWrite(Class<T> configClass, Consumer<T> setter) {
        T config = (T) CACHE.get(configClass);
        setter.accept(config);
        saveConfig(config);
    }

    public static <T extends TConfig> void saveConfig(T config) {
        File configFile = checkFile(config, false);
        saveConfigInternal(config, configFile);
    }

    private static void loadConfigInternal(TConfig newInstance, File configFile) {
        try {
            if (configFile.isFile()) {
                newInstance = GSON.fromJson(FileUtils.readFileToString(configFile, StandardCharsets.UTF_8), newInstance.getClass());
            } else {
                FileUtils.write(configFile, GSON.toJson(newInstance), StandardCharsets.UTF_8);
            }
            CACHE.put(newInstance.getClass(), newInstance);
            saveConfigInternal(newInstance, configFile);
        } catch (IOException ignored) {
            LogUtils.getLogger().error("Failed to load config {}. Things may not work well.", newInstance.getClass());
        }
    }

    private static <T extends TConfig> void saveConfigInternal(T config, File configFile) {
        CompletableFuture.runAsync(() -> {
            try {
                FileUtils.write(configFile, GSON.toJson(config), StandardCharsets.UTF_8);
            } catch (IOException ignored) {
                LogUtils.getLogger().error("Failed to save config {}. Things may not work well.", config.getClass());
            }
        });
    }

    public static class Universal extends ConfigHelper {

        public static void loadConfig(TConfig newInstance) {
            File configFile = checkFile(newInstance, true);
            loadConfigInternal(newInstance, configFile);
        }

        @SuppressWarnings("unchecked")
        public static <T extends TConfig> void getConfigWrite(Class<T> configClass, Consumer<T> setter) {
            T config = (T) CACHE.get(configClass);
            setter.accept(config);
            Universal.saveConfig(config);
        }

        public static <T extends TConfig> void saveConfig(T config) {
            File configFile = checkFile(config, true);
            saveConfigInternal(config, configFile);
        }
    }
}
