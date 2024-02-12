package com.mrcrayfish.configured.util;

import com.electronwill.nightconfig.core.AbstractConfig;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.utils.UnmodifiableConfigWrapper;
import com.google.common.collect.ImmutableList;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.IConfigEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author: MrCrayfish
 */
public class ForgeConfigHelper
{
    private static final Method MOD_CONFIG_SET_CONFIG_DATA = ObfuscationReflectionHelper.findMethod(ModConfig.class, "setConfigData", CommentedConfig.class);
    private static final Method MOD_CONFIG_FIRE_EVENT = ObfuscationReflectionHelper.findMethod(ModConfig.class, "fireEvent", IConfigEvent.class);

    /**
     * Gathers all the Forge config values with a deep search. Used for resetting defaults
     */
    public static List<Pair<ForgeConfigSpec.ConfigValue<?>, ForgeConfigSpec.ValueSpec>> gatherAllForgeConfigValues(UnmodifiableConfig config, ForgeConfigSpec spec)
    {
        List<Pair<ForgeConfigSpec.ConfigValue<?>, ForgeConfigSpec.ValueSpec>> values = new ArrayList<>();
        gatherValuesFromForgeConfig(config, spec, values);
        return ImmutableList.copyOf(values);
    }

    /**
     * Gathers all the config values from the given Forge config and adds it's to the provided list.
     * This will search deeper if it finds another config and recursively call itself.
     */
    private static void gatherValuesFromForgeConfig(UnmodifiableConfig config, ForgeConfigSpec spec, List<Pair<ForgeConfigSpec.ConfigValue<?>, ForgeConfigSpec.ValueSpec>> values)
    {
        config.valueMap().forEach((s, o) ->
        {
            if(o instanceof AbstractConfig)
            {
                gatherValuesFromForgeConfig((UnmodifiableConfig) o, spec, values);
            }
            else if(o instanceof ForgeConfigSpec.ConfigValue<?> configValue)
            {
                ForgeConfigSpec.ValueSpec valueSpec = spec.getRaw(configValue.getPath());
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
    public static void setForgeConfigData(ModConfig config, @Nullable CommentedConfig configData)
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
    public static ModConfig getForgeConfig(String fileName)
    {
        return ConfigTracker.INSTANCE.fileMap().get(fileName);
    }

    /**
     * Gathers all the config values with a deep search. Used for resetting defaults
     */
    public static List<Pair<ForgeConfigSpec.ConfigValue<?>, ForgeConfigSpec.ValueSpec>> gatherAllForgeConfigValues(ModConfig config)
    {
        return gatherAllForgeConfigValues(((ForgeConfigSpec) config.getSpec()).getValues(), (ForgeConfigSpec) config.getSpec());
    }

    /**
     * A helper method to fire config event. Since Forge has hidden these calls (which is fine), the
     * only way to call them is to call them is by using reflection.
     *
     * @param config the config to fire the event for
     * @param event  the event
     */
    public static void fireForgeConfigEvent(ModConfig config, ModConfigEvent event)
    {
        try
        {
            MOD_CONFIG_FIRE_EVENT.invoke(config, event);
        }
        catch(InvocationTargetException | IllegalAccessException e)
        {
            e.printStackTrace();
        }
    }

    @Nullable
    public static ForgeConfigSpec findConfigSpec(UnmodifiableConfig config)
    {
        if(config instanceof ForgeConfigSpec spec)
            return spec;

        /* Find ForgeConfigSpec instances that have been wrapped, Night Config provides a commonly
         * used default implementation for this which we use here Night Config also has more config
         * wrapper classes, which all seem to extend this one fortunately */
        if(config instanceof UnmodifiableConfigWrapper)
        {
            try
            {
                Field field = UnmodifiableConfigWrapper.class.getDeclaredField("config");
                field.setAccessible(true);
                return findConfigSpec((UnmodifiableConfig) MethodHandles.lookup().unreflectGetter(field).invoke(config));
            }
            catch(Throwable ignored) {}
        }
        return null;
    }
}
