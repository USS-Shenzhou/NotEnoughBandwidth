package cn.ussshenzhou.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * @author USS_Shenzhou
 */
public class MultiInstanceConfigHelper {
    private static final File CONFIG_DIR = Paths.get("config").toFile();
    private static final ConcurrentHashMap<Class<? extends TMultiInstanceConfig>, ConcurrentHashMap<String, TMultiInstanceConfig>> CACHE = new ConcurrentHashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static void checkDir(File dir) {
        if (!dir.isDirectory()) {
            dir.mkdir();
        }
    }

    private static File checkChildDir(TMultiInstanceConfig config) {
        return checkChildDir(config.getClass(), config.getChildDirName());
    }

    private static File checkChildDir(Class<? extends TMultiInstanceConfig> clazz, String childDirName) {
        checkDir(CONFIG_DIR);
        checkDir(CONFIG_DIR.toPath().resolve(childDirName).toFile());
        File f = CONFIG_DIR.toPath().resolve(childDirName).resolve(clazz.getSimpleName()).toFile();
        checkDir(f);
        return f;
    }

    public static void loadConfigInstances(TMultiInstanceConfig newInstance) {
        loadConfigInstances(newInstance.getClass(), newInstance.getChildDirName());
    }

    public static void loadConfigInstances(Class<? extends TMultiInstanceConfig> clazz, String childDirName) {
        File childDir = checkChildDir(clazz, childDirName);
        File[] instances = childDir.listFiles();
        if (instances == null) {
            return;
        }
        for (File f : instances) {
            try {
                TMultiInstanceConfig instance = GSON.fromJson(FileUtils.readFileToString(f, StandardCharsets.UTF_8), clazz);
                putCache(instance);
                saveConfig(instance);
            } catch (IOException ignored) {
                LogUtils.getLogger().error("Failed to load config {}. Things may not work well.", f);
            }
        }
    }


    private static <T extends TMultiInstanceConfig> void putCache(T config) {
        if (CACHE.containsKey(config.getClass())) {
            CACHE.get(config.getClass()).put(config.getFileName(), config);
        } else {
            ConcurrentHashMap<String, TMultiInstanceConfig> children = new ConcurrentHashMap<>();
            children.put(config.getFileName(), config);
            CACHE.put(config.getClass(), children);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends TMultiInstanceConfig> ConcurrentHashMap<String, T> getConfigInstancesRead(Class<T> configClass) {
        return (ConcurrentHashMap<String, T>) CACHE.get(configClass);
    }

    @SuppressWarnings("unchecked")
    public static <T extends TMultiInstanceConfig> T getConfigInstanceRead(Class<T> configClass, String childName) {
        return (T) CACHE.get(configClass).get(childName);
    }

    @SuppressWarnings("unchecked")
    public static <T extends TMultiInstanceConfig> void getConfigInstancesWrite(Class<T> configClass, Consumer<ConcurrentHashMap<String, T>> setter) {
        ConcurrentHashMap<String, T> configInstances = (ConcurrentHashMap<String, T>) CACHE.get(configClass);
        setter.accept(configInstances);
        configInstances.values().forEach(MultiInstanceConfigHelper::saveConfig);

    }

    @SuppressWarnings("unchecked")
    public static <T extends TMultiInstanceConfig> void getConfigInstanceWrite(Class<T> configClass, String childName, Consumer<T> setter) {
        T configInstances = (T) CACHE.get(configClass).get(childName);
        setter.accept(configInstances);
        saveConfig(configInstances);
    }

    public static <T extends TMultiInstanceConfig> void addNewConfigInstance(T config) {
        putCache(config);
        saveConfig(config);
    }

    public static <T extends TMultiInstanceConfig> void removeConfig(T config) {
        CACHE.get(config.getClass()).remove(config.getFileName());
        File configFile = checkChildDir(config).toPath().resolve(config.getFileName() + ".json").toFile();
        configFile.delete();
    }

    public static <T extends TMultiInstanceConfig> void saveConfig(T config) {
        File configFile = checkChildDir(config).toPath().resolve(config.getFileName() + ".json").toFile();
        CompletableFuture.runAsync(()->{
            try {
                FileUtils.write(configFile, GSON.toJson(config), StandardCharsets.UTF_8);
            } catch (IOException ignored) {
                LogUtils.getLogger().error("Failed to save config {}. Things may not work well.", config.getClass());
            }
        });
    }
}
