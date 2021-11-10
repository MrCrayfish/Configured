package com.mrcrayfish.configured.util;

import com.electronwill.nightconfig.core.AbstractConfig;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.common.collect.ImmutableList;
import com.mrcrayfish.configured.client.screen.ModConfigSelectionScreen;
import com.mrcrayfish.configured.network.PacketHandler;
import com.mrcrayfish.configured.network.message.MessageSyncServerConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author: MrCrayfish
 */
public class ConfigHelper
{
    private static final Method MOD_CONFIG_SET_CONFIG_DATA = ObfuscationReflectionHelper.findMethod(ModConfig.class, "setConfigData", CommentedConfig.class);
    private static final Method MOD_CONFIG_FIRE_EVENT = ObfuscationReflectionHelper.findMethod(ModConfig.class, "fireEvent", ModConfig.ModConfigEvent.class);
    private static final Constructor<ModConfig.Reloading> MOD_CONFIG_RELOADING = ObfuscationReflectionHelper.findConstructor(ModConfig.Reloading.class);

    /**
     * Determines if the given ModConfig differs compared to it's default values.
     *
     * @param config the mod config to test
     * @return true if the config is different
     */
    public static boolean isModified(ModConfig config)
    {
        return gatherAllConfigValues(config).stream().anyMatch(pair -> !pair.getLeft().get().equals(pair.getRight().getDefault()));
    }

    /**
     * Gathers all the config values with a deep search. Used for resetting defaults
     */
    public static List<Pair<ForgeConfigSpec.ConfigValue<?>, ForgeConfigSpec.ValueSpec>> gatherAllConfigValues(ModConfig config)
    {
        return gatherAllConfigValues(config.getSpec().getValues(), config.getSpec());
    }

    /**
     * Gathers all the config values with a deep search. Used for resetting defaults
     */
    public static List<Pair<ForgeConfigSpec.ConfigValue<?>, ForgeConfigSpec.ValueSpec>> gatherAllConfigValues(UnmodifiableConfig config, ForgeConfigSpec spec)
    {
        List<Pair<ForgeConfigSpec.ConfigValue<?>, ForgeConfigSpec.ValueSpec>> values = new ArrayList<>();
        gatherValuesFromConfig(config, spec, values);
        return ImmutableList.copyOf(values);
    }

    /**
     * Gathers all the config values from the given config and adds it's to the provided list. This
     * will search deeper if it finds another config and recursively call itself.
     */
    private static void gatherValuesFromConfig(UnmodifiableConfig config, ForgeConfigSpec spec, List<Pair<ForgeConfigSpec.ConfigValue<?>, ForgeConfigSpec.ValueSpec>> values)
    {
        config.valueMap().forEach((s, o) ->
        {
            if(o instanceof AbstractConfig)
            {
                gatherValuesFromConfig((UnmodifiableConfig) o, spec, values);
            }
            else if(o instanceof ForgeConfigSpec.ConfigValue<?>)
            {
                ForgeConfigSpec.ConfigValue<?> configValue = (ForgeConfigSpec.ConfigValue<?>) o;
                ForgeConfigSpec.ValueSpec valueSpec = spec.getRaw(configValue.getPath());
                values.add(Pair.of(configValue, valueSpec));
            }
        });
    }

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

    @Nullable
    public static ModConfig getModConfig(String fileName)
    {
        ConcurrentHashMap<String, ModConfig> configMap = ObfuscationReflectionHelper.getPrivateValue(ConfigTracker.class, ConfigTracker.INSTANCE, "fileMap");
        return configMap != null ? configMap.get(fileName) : null;
    }

    public static void fireEvent(ModConfig config, ModConfig.ModConfigEvent event)
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
    public static ModConfig.Reloading reloadingEvent()
    {
        try
        {
            return MOD_CONFIG_RELOADING.newInstance();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static void sendConfigDataToServer(ModConfig config)
    {
        try
        {
            Minecraft minecraft = Minecraft.getInstance();
            if(config.getType() == ModConfig.Type.SERVER && minecraft.player != null && minecraft.player.hasPermissionLevel(2))
            {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                TomlFormat.instance().createWriter().write(config.getConfigData(), stream);
                PacketHandler.getPlayChannel().sendToServer(new MessageSyncServerConfig(config.getFileName(), stream.toByteArray()));
                stream.close();
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void resetCache(ModConfig config)
    {
        gatherAllConfigValues(config).forEach(pair -> pair.getLeft().clearCache());
    }
}
