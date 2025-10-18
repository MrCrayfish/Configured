package com.mrcrayfish.configured.util;

import com.electronwill.nightconfig.core.AbstractConfig;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.concurrent.SynchronizedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.google.common.collect.ImmutableList;
import com.mrcrayfish.configured.Constants;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

/**
 * Author: MrCrayfish
 */
@SuppressWarnings("UnstableApiUsage")
public class NeoForgeConfigHelper
{
    private static final Method MOD_CONFIG_OPEN_CONFIG = ObfuscationReflectionHelper.findMethod(ConfigTracker.class, "openConfig", ModConfig.class, Path.class, Path.class);
    private static final Method MOD_CONFIG_CLOSE_CONFIG = ObfuscationReflectionHelper.findMethod(ConfigTracker.class, "unloadConfig", ModConfig.class);
    private static final Field MOD_CONFIG_LOADED_CONFIG = ObfuscationReflectionHelper.findField(ModConfig.class, "loadedConfig");
    private static final Field MOD_CONFIG_LOCK = ObfuscationReflectionHelper.findField(ModConfig.class, "lock");

    static
    {
        MOD_CONFIG_OPEN_CONFIG.setAccessible(true);
        MOD_CONFIG_CLOSE_CONFIG.setAccessible(true);
        MOD_CONFIG_LOADED_CONFIG.setAccessible(true);
        MOD_CONFIG_LOCK.setAccessible(true);
    }

    /**
     * Gathers all the Forge config values with a deep search. Used for resetting defaults
     */
    public static List<Pair<ModConfigSpec.ConfigValue<?>, ModConfigSpec.ValueSpec>> gatherAllConfigValues(UnmodifiableConfig config, ModConfigSpec spec)
    {
        List<Pair<ModConfigSpec.ConfigValue<?>, ModConfigSpec.ValueSpec>> values = new ArrayList<>();
        gatherValuesFromModConfig(config, spec, values);
        return ImmutableList.copyOf(values);
    }

    /**
     * Gathers all the config values from the given Forge config and adds it's to the provided list.
     * This will search deeper if it finds another config and recursively call itself.
     */
    private static void gatherValuesFromModConfig(UnmodifiableConfig config, ModConfigSpec spec, List<Pair<ModConfigSpec.ConfigValue<?>, ModConfigSpec.ValueSpec>> values)
    {
        config.valueMap().forEach((s, o) ->
        {
            if(o instanceof AbstractConfig)
            {
                gatherValuesFromModConfig((UnmodifiableConfig) o, spec, values);
            }
            else if(o instanceof ModConfigSpec.ConfigValue<?> configValue)
            {
                ModConfigSpec.ValueSpec valueSpec = spec.getSpec().getRaw(configValue.getPath());
                values.add(Pair.of(configValue, valueSpec));
            }
        });
    }

    @Nullable
    public static IConfigSpec.ILoadedConfig getLoadedConfig(ModConfig config)
    {
        try
        {
            return (IConfigSpec.ILoadedConfig) MOD_CONFIG_LOADED_CONFIG.get(config);
        }
        catch(IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public static CommentedConfig getConfigData(ModConfig config)
    {
        IConfigSpec.ILoadedConfig data = getLoadedConfig(config);
        if(data != null)
        {
            return data.config();
        }
        return null;
    }

    private static void wrapLock(ModConfig config, Runnable task)
    {
        Lock lock;
        try
        {
            lock = (Lock) MOD_CONFIG_LOCK.get(config);
        }
        catch(IllegalAccessException e)
        {
            Constants.LOG.error("Failed to acquire lock when setting config data");
            return;
        }

        lock.lock();
        try
        {
            task.run();
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Since ModConfig#setConfigData is not visible, this is a helper method to reflectively call the method
     *
     * @param config     the config to update
     * @param configData the new data for the config
     */
    public static void setConfigData(ModConfig config, @Nullable CommentedConfig configData)
    {
        IConfigSpec.ILoadedConfig loadedConfig = getLoadedConfig(config);
        if(loadedConfig != null)
        {
            CommentedConfig data = loadedConfig.config();
            wrapLock(config, () -> {
                data.putAll(configData);
                correctConfig(config, data);
            });
            resetConfigCache(config);
            loadedConfig.save(); // Saves and sends the reload event
        }
    }

    /**
     * Gets the mod config for the given file name. Uses reflection to obtain the config map.
     *
     * @param fileName the file name of the config
     * @return the mod config instance for the file name or null if it doesn't exist
     */
    @Nullable
    public static ModConfig getModConfig(String fileName)
    {
        ConcurrentHashMap<String, ModConfig> configMap = ObfuscationReflectionHelper.getPrivateValue(ConfigTracker.class, ConfigTracker.INSTANCE, "fileMap");
        return configMap != null ? configMap.get(fileName) : null;
    }

    /**
     * Gathers all the config values with a deep search. Used for resetting defaults
     */
    public static List<Pair<ModConfigSpec.ConfigValue<?>, ModConfigSpec.ValueSpec>> gatherAllConfigValues(ModConfig config)
    {
        return gatherAllConfigValues(((ModConfigSpec) config.getSpec()).getValues(), (ModConfigSpec) config.getSpec());
    }

    public static void closeConfig(ModConfig config)
    {
        try
        {
            MOD_CONFIG_CLOSE_CONFIG.invoke(ConfigTracker.INSTANCE, config);
        }
        catch(InvocationTargetException | IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void openConfig(ModConfig config, Path path)
    {
        try
        {
            MOD_CONFIG_OPEN_CONFIG.invoke(ConfigTracker.INSTANCE, config, path, null);
        }
        catch(InvocationTargetException | IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void saveConfig(ModConfig config)
    {
        IConfigSpec.ILoadedConfig loadedConfig = getLoadedConfig(config);
        if(loadedConfig != null)
        {
            loadedConfig.save();
        }
    }

    public static void resetConfigCache(ModConfig config)
    {
        if(config.getSpec() instanceof ModConfigSpec spec)
        {
            spec.afterReload();
        }
    }

    public static void correctConfig(ModConfig config, CommentedConfig data)
    {
        IConfigSpec spec = config.getSpec();
        if(!spec.isCorrect(data))
        {
            spec.correct(data);
        }
    }
}
