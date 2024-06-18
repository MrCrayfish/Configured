package com.mrcrayfish.configured.util;

import com.google.common.collect.ImmutableList;
import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.Environment;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.client.ClientConfigHelper;
import com.mrcrayfish.configured.platform.Services;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

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
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        return listener != null && Services.PLATFORM.isConnectionActive(listener);
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
        if(Services.PLATFORM.getEnvironment() != Environment.CLIENT)
            return false;

        return ClientConfigHelper.isPlayingGame();
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

    public static Player getClientPlayer()
    {
        if(Services.PLATFORM.getEnvironment() != Environment.CLIENT)
            return null;

        return ClientConfigHelper.getClientPlayer();
    }

    public static boolean isRunningLocalServer()
    {
        if(Services.PLATFORM.getEnvironment() != Environment.CLIENT)
            return false;

        return ClientConfigHelper.isRunningLocalServer();
    }
}
