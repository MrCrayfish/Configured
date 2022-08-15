package com.mrcrayfish.configured.util;

import com.electronwill.nightconfig.core.AbstractConfig;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.common.collect.ImmutableList;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.network.PacketHandler;
import com.mrcrayfish.configured.network.message.MessageSyncServerConfig;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.IConfigEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author: MrCrayfish
 */
public class ConfigHelper
{
    private static final Method MOD_CONFIG_SET_CONFIG_DATA = ObfuscationReflectionHelper.findMethod(ModConfig.class, "setConfigData", CommentedConfig.class);
    private static final Method MOD_CONFIG_FIRE_EVENT = ObfuscationReflectionHelper.findMethod(ModConfig.class, "fireEvent", IConfigEvent.class);

    /**
     * Determines if the given ModConfig differs compared to it's default values.
     *
     * @param config the mod config to test
     * @return true if the config is different
     */
    public static boolean isModified(IModConfig config)
    {
        return gatherAllConfigValues(config).stream().anyMatch(T -> !T.isDefault());
    }

    /**
     * Gathers all the config values with a deep search. Used for resetting defaults
     */
    public static List<IConfigValue<?>> gatherAllConfigValues(IModConfig config)
    {
    	return gatherAllConfigValues(config.getRoot());
    }

     /**
     * Gathers all the config values with a deep search. Used for resetting defaults
     */
    public static List<IConfigValue<?>> gatherAllConfigValues(IConfigEntry entry)
    {
    	List<IConfigValue<?>> values = new ObjectArrayList<>();
    	gatherValuesFromConfig(entry, values);
    	return ImmutableList.copyOf(values);
    }

    /**
     * Gathers all the config values from the given config and adds it's to the provided list. This
     * will search deeper if it finds another config and recursively call itself.
     */
    private static void gatherValuesFromConfig(IConfigEntry entry, List<IConfigValue<?>> values)
    {
    	if(entry.isLeaf())
    	{
    		IConfigValue<?> value = entry.getValue();
    		if(value != null) values.add(value);
    		return;
    	}
    	for(IConfigEntry children : entry.getChildren())
    	{
    		gatherValuesFromConfig(children, values);
    	}
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
    public static void setModConfigData(ModConfig config, @Nullable CommentedConfig configData)
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
    public static List<Pair<ForgeConfigSpec.ConfigValue<?>, ForgeConfigSpec.ValueSpec>> gatherAllConfigValues(ModConfig config)
    {
        return gatherAllConfigValues(((ForgeConfigSpec) config.getSpec()).getValues(), (ForgeConfigSpec) config.getSpec());
    }

    /**
     * A helper method to fire config event. Since Forge has hidden these calls (which is fine), the
     * only way to call them is to call them is by using reflection.
     *
     * @param config the config to fire the event for
     * @param event  the event
     */
    public static void fireEvent(ModConfig config, ModConfigEvent event)
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

    public static boolean isConfiguredInstalledOnServer()
    {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        if(listener == null)
            return false;
        Connection connection = listener.getConnection();
        return PacketHandler.getPlayChannel().isRemotePresent(connection);
    }

    public static void sendForgeModConfigDataToServer(ModConfig config)
    {
        // Prevents trying to send packet to server if the server doesn't have configured installed
        if(!isConfiguredInstalledOnServer())
            return;

        try
        {
            Minecraft minecraft = Minecraft.getInstance();
            if(config.getType() == ModConfig.Type.SERVER && minecraft.player != null && minecraft.player.hasPermissions(2))
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

    /**
     * Resets the spec cache for the given mod config
     * @param config the mod config to reset
     */
    public static void resetCache(IModConfig config)
    {
        gatherAllConfigValues(config).forEach(IConfigValue::cleanCache);
    }

    /**
     * Resets the spec cache for the given mod config
     * @param config the mod config to reset
     */
    public static void resetCache(ModConfig config)
    {
        gatherAllConfigValues(config).forEach(pair -> pair.getLeft().clearCache());
    }

    public static boolean isEditableServerConfig(IModConfig config)
    {
        return config.getType().isServer() && config.getType().getDist().orElse(null) != Dist.DEDICATED_SERVER;
    }

    /**
     * Performs a deep search of a config entry and returns all the config values that have changed.
     *
     * @param entry the root entry to perform the search
     * @return a set of config values that have changed or an empty set if nothing changed
     */
    public static Set<IConfigValue<?>> getChangedValues(IConfigEntry entry)
    {
        Set<IConfigValue<?>> changed = new HashSet<>();
        Queue<IConfigEntry> found = new ArrayDeque<>();
        found.add(entry);
        while(!found.isEmpty())
        {
            IConfigEntry toSave = found.poll();
            if(!toSave.isLeaf())
            {
                found.addAll(toSave.getChildren());
                continue;
            }

            IConfigValue<?> value = toSave.getValue();
            if(value != null && value.isChanged())
            {
                changed.add(value);
            }
        }
        return changed;
    }

    public static boolean isPlayingGame()
    {
        return Minecraft.getInstance().player != null;
    }
}
