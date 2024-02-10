package com.mrcrayfish.configured.util;

import com.electronwill.nightconfig.core.AbstractConfig;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.google.common.collect.ImmutableList;
import net.neoforged.fml.config.ConfigFileTypeHandler;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author: MrCrayfish
 */
public class NeoForgeConfigHelper
{
    private static final Method MOD_CONFIG_SET_CONFIG_DATA = ObfuscationReflectionHelper.findMethod(ModConfig.class, "setConfigData", CommentedConfig.class);

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
                ModConfigSpec.ValueSpec valueSpec = spec.getRaw(configValue.getPath());
                values.add(Pair.of(configValue, valueSpec));
            }
        });
    }

    /**
     * Since ModConfig#setConfigData is not visible, this is a helper method to reflectively call the method
     *
     * @param config     the config to update
     * @param configData the new data for the config
     */
    public static void setConfigData(ModConfig config, @Nullable CommentedConfig configData)
    {
        try
        {
            MOD_CONFIG_SET_CONFIG_DATA.invoke(config, configData);
            if(configData instanceof FileConfig)
            {
                config.save();
            }
        }
        catch(InvocationTargetException | IllegalAccessException e)
        {
            e.printStackTrace();
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

    public static void unload(ModConfig config)
    {
        if(config.getConfigData() != null)
        {
            ConfigFileTypeHandler.TOML.unload(config);
            config.save();
            setConfigData(config, null);
        }
    }
}
