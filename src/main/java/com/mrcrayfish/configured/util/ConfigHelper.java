package com.mrcrayfish.configured.util;

import com.electronwill.nightconfig.core.AbstractConfig;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.file.FileWatcher;
import com.electronwill.nightconfig.core.utils.UnmodifiableConfigWrapper;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.common.collect.ImmutableList;
import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.network.PacketHandler;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.IConfigEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author: MrCrayfish
 */
public class ConfigHelper
{
    private static final Set<Path> WATCHED_PATHS = new HashSet<>();
    private static final Method MOD_CONFIG_SET_CONFIG_DATA = ObfuscationReflectionHelper.findMethod(ModConfig.class, "setConfigData", CommentedConfig.class);
    private static final Method MOD_CONFIG_FIRE_EVENT = ObfuscationReflectionHelper.findMethod(ModConfig.class, "fireEvent", IConfigEvent.class);

    /**
     * Gathers all the config entries with a deep search. Used for deep searches
     */
    public static List<IConfigEntry> gatherAllConfigEntries(IConfigEntry entry)
    {
        List<IConfigEntry> entries = new ObjectArrayList<>();
        Queue<IConfigEntry> queue = new ArrayDeque<>(entry.getChildren());
        while(!queue.isEmpty())
        {
            IConfigEntry e = queue.poll();
            entries.add(e);
            if(!e.isLeaf())
            {
                queue.addAll(e.getChildren());
            }
        }
        return entries;
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
    	gatherValuesFromForgeConfig(entry, values);
    	return ImmutableList.copyOf(values);
    }

    /**
     * Gathers all the config values from the given Forge config and adds it's to the provided list.
     * This will search deeper if it finds another config and recursively call itself.
     */
    private static void gatherValuesFromForgeConfig(IConfigEntry entry, List<IConfigValue<?>> values)
    {
    	if(entry.isLeaf())
    	{
    		IConfigValue<?> value = entry.getValue();
    		if(value != null) values.add(value);
    		return;
    	}
    	for(IConfigEntry children : entry.getChildren())
    	{
    		gatherValuesFromForgeConfig(children, values);
    	}
    }

    /**
     * Gathers all the Forge config values with a deep search. Used for resetting defaults
     */
    public static List<Pair<ForgeConfigSpec.ConfigValue<?>, ForgeConfigSpec.ValueSpec>> gatherAllForgeConfigValues(UnmodifiableConfig config, UnmodifiableConfig spec)
    {
        List<Pair<ForgeConfigSpec.ConfigValue<?>, ForgeConfigSpec.ValueSpec>> values = new ArrayList<>();
        gatherValuesFromForgeConfig(config, spec, values);
        return ImmutableList.copyOf(values);
    }

    /**
     * Gathers all the config values from the given Forge config and adds it's to the provided list.
     * This will search deeper if it finds another config and recursively call itself.
     */
    private static void gatherValuesFromForgeConfig(UnmodifiableConfig config, UnmodifiableConfig spec, List<Pair<ForgeConfigSpec.ConfigValue<?>, ForgeConfigSpec.ValueSpec>> values)
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

    public static boolean isWorldConfig(IModConfig config)
    {
        return config.getType() == ConfigType.WORLD || config.getType() == ConfigType.WORLD_SYNC;
    }

    public static boolean isServerConfig(IModConfig config)
    {
        return config.getType().isServer() && !isWorldConfig(config);
    }

    /* Client only */
    public static boolean isConfiguredInstalledOnServer()
    {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        return listener != null && PacketHandler.getPlayChannel().isRemotePresent(listener.getConnection());
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

    // Client only
    public static boolean isPlayingGame()
    {
        return FMLEnvironment.dist.isDedicatedServer() || DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> Minecraft.getInstance().level != null);
    }

    public static boolean isServerOwnedByPlayer(@Nullable Player player)
    {
        return player != null && player.getServer() != null && !player.getServer().isDedicatedServer() && player.getServer().isSingleplayerOwner(player.getGameProfile());
    }

    public static boolean hasPermissionToEdit(@Nullable Player player, IModConfig config)
    {
        return !config.getType().isServer() || player != null && (player.hasPermissions(4) || isServerOwnedByPlayer(player));
    }

    public static boolean isOperator(@Nullable Player player)
    {
        return player != null && player.hasPermissions(4);
    }

    public static boolean isRunningLocalServer()
    {
        return FMLEnvironment.dist.isClient() && DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> Minecraft.getInstance().hasSingleplayerServer());
    }

    public static boolean isPlayingLocally()
    {
        return FMLEnvironment.dist.isClient() && DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> Minecraft.getInstance().getSingleplayerServer() != null);
    }

    public static boolean isPlayingRemotely()
    {
        return FMLEnvironment.dist.isClient() && DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> {
            ClientPacketListener listener = Minecraft.getInstance().getConnection();
            return listener != null && !listener.getConnection().isMemoryConnection();
        });
    }

    @Nullable
    public static Player getClientPlayer()
    {
        return !FMLEnvironment.dist.isClient() ? null : DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> Minecraft.getInstance().player);
    }

    public static void createBackup(UnmodifiableConfig config)
    {
        if(config instanceof FileConfig fileConfig)
        {
            try
            {
                Path configPath = fileConfig.getNioPath();
                // The length check prevents backing up on initial creation of the config file
                // It also doesn't really make sense to back up an empty file
                if(Files.exists(configPath) && fileConfig.getFile().length() > 0)
                {
                    Path backupPath = configPath.getParent().resolve(fileConfig.getFile().getName() + ".bak");
                    Files.copy(configPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            catch(IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public static void closeConfig(UnmodifiableConfig config)
    {
        if(config instanceof FileConfig fileConfig)
        {
            Path path = fileConfig.getNioPath();
            if(WATCHED_PATHS.contains(path))
            {
                FileWatcher.defaultInstance().removeWatch(path);
                WATCHED_PATHS.remove(path);
            }
            fileConfig.close();
        }
    }

    public static void loadConfig(UnmodifiableConfig config)
    {
        if(config instanceof FileConfig fileConfig)
        {
            try
            {
                fileConfig.load();
            }
            catch(Exception ignored)
            {
                //TODO error handling
            }
        }
    }

    public static void saveConfig(UnmodifiableConfig config)
    {
        if(config instanceof FileConfig fileConfig)
        {
            fileConfig.save();
        }
    }

    public static void watchConfig(UnmodifiableConfig config, Runnable callback)
    {
        if(config instanceof FileConfig fileConfig)
        {
            try
            {
                Path path = fileConfig.getNioPath();
                WATCHED_PATHS.add(path);
                FileWatcher.defaultInstance().setWatch(path, callback);
            }
            catch(IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public static byte[] readBytes(Path path)
    {
        try
        {
            return Files.readAllBytes(path);
        }
        catch(IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static byte[] getBytes(UnmodifiableConfig config)
    {
        if(config instanceof FileConfig fc)
        {
            return readBytes(fc.getNioPath());
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        TomlFormat.instance().createWriter().write(config, stream);
        return stream.toByteArray();
    }

    @Nullable
    public static ForgeConfigSpec findForgeConfigSpec(UnmodifiableConfig config) {
        if (config instanceof ForgeConfigSpec spec) return spec;
        // find ForgeConfigSpec instances that have been wrapped, Night Config provides a commonly used default implementation for this which we use here
        // Night Config also has more config wrapper classes, which all seem to extend this one fortunately
        if (config instanceof UnmodifiableConfigWrapper) {
            try {
                Field field = UnmodifiableConfigWrapper.class.getDeclaredField("config");
                field.setAccessible(true);
                return findForgeConfigSpec((UnmodifiableConfig) MethodHandles.lookup().unreflectGetter(field).invoke(config));
            } catch (Throwable ignored) {

            }
        }
        return null;
    }
}
