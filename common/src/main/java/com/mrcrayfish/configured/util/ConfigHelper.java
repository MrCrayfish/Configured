package com.mrcrayfish.configured.util;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.file.FileWatcher;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.common.collect.ImmutableList;
import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.network.message.MessageRequestSimpleConfig;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.EnvExecutor;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.api.EnvType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Author: MrCrayfish
 */
public class ConfigHelper
{
    private static final Set<Path> WATCHED_PATHS = new HashSet<>();

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
        return NetworkManager.canServerReceive(MessageRequestSimpleConfig.ID);
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
        return Platform.getEnv() == EnvType.SERVER || EnvExecutor.getInEnv(EnvType.CLIENT, () -> () -> Minecraft.getInstance().level != null).get();
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
        return Platform.getEnv() == EnvType.CLIENT && EnvExecutor.getInEnv(EnvType.CLIENT, () -> () -> Minecraft.getInstance().hasSingleplayerServer()).get();
    }

    public static boolean isPlayingLocally()
    {
        return Platform.getEnv() == EnvType.CLIENT && EnvExecutor.getInEnv(EnvType.CLIENT, () -> () -> Minecraft.getInstance().getSingleplayerServer() != null).get();
    }

    public static boolean isPlayingRemotely()
    {
        return Platform.getEnv() == EnvType.CLIENT && EnvExecutor.getInEnv(EnvType.CLIENT, () -> () -> {
            ClientPacketListener listener = Minecraft.getInstance().getConnection();
            return listener != null && !listener.getConnection().isMemoryConnection();
        }).get();
    }

    @Nullable
    public static Player getClientPlayer()
    {
        return Platform.getEnv() != EnvType.CLIENT ? null : EnvExecutor.getInEnv(EnvType.CLIENT, () -> () -> Minecraft.getInstance().player).get();
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
}
