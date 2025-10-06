package com.mrcrayfish.configured.util;

import com.google.common.collect.ImmutableList;
import com.mrcrayfish.configured.Config;
import com.mrcrayfish.configured.api.Environment;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.client.ClientConfigHelper;
import com.mrcrayfish.configured.client.ClientSessionData;
import com.mrcrayfish.configured.platform.Services;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
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

    public static boolean isServerConfig(IModConfig config)
    {
        return config.getType().isServer(); // TODO && !isWorldConfig(config);
    }

    public static boolean isConfiguredInstalledOnServer()
    {
        if(Services.PLATFORM.getEnvironment() == Environment.DEDICATED_SERVER)
        {
            return true;
        }
        return ClientConfigHelper.isConfiguredInstalledRemotely();
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
        if(Services.PLATFORM.getEnvironment() != Environment.CLIENT)
            return false;

        return ClientConfigHelper.isPlayingGame();
    }

    public static boolean isPlayingLan()
    {
        if(Services.PLATFORM.getEnvironment() != Environment.CLIENT)
            return false;

        return ClientConfigHelper.isLan() || !isIntegratedServer() && ClientSessionData.isLan();
    }

    public static boolean isSingleplayer()
    {
        if(Services.PLATFORM.getEnvironment() == Environment.DEDICATED_SERVER)
            return false;

        return ClientConfigHelper.isSingleplayer();
    }

    public static boolean isServerOwnedByPlayer(@Nullable Player player)
    {
        if(Services.PLATFORM.getEnvironment() == Environment.DEDICATED_SERVER)
        {
            return false;
        }
        return ClientConfigHelper.isServerOwnedByPlayer(player);
    }

    public static boolean isOperator(@Nullable Player player)
    {
        if(player != null)
        {
            if(Services.PLATFORM.getEnvironment() == Environment.DEDICATED_SERVER)
            {
                MinecraftServer server = player.level().getServer();
                return server != null && server.getPlayerList().isOp(player.nameAndId());
            }
            return player.hasPermissions(Commands.LEVEL_OWNERS);
        }
        return false;
    }

    public static Player getClientPlayer()
    {
        if(Services.PLATFORM.getEnvironment() != Environment.CLIENT)
            return null;

        return ClientConfigHelper.getClientPlayer();
    }

    public static boolean isIntegratedServer()
    {
        if(Services.PLATFORM.getEnvironment() != Environment.CLIENT)
            return false;

        return ClientConfigHelper.isIntegratedServer();
    }

    public static boolean isDeveloper(@Nullable Player player)
    {
        if(player != null)
        {
            if(Services.PLATFORM.getEnvironment() == Environment.DEDICATED_SERVER)
            {
                return Config.isDeveloperEnabled() && Config.getDevelopers().contains(player.getUUID());
            }
            return player.isLocalPlayer() && ClientSessionData.isDeveloper() || isServerOwnedByPlayer(player);
        }
        return false;
    }

    public static boolean isPlayingOnRemoteServer()
    {
        if(Services.PLATFORM.getEnvironment() == Environment.DEDICATED_SERVER)
        {
            return true;
        }
        return ClientConfigHelper.isPlayingRemotely();
    }

    public static boolean canRestoreConfig(IModConfig config, Player player)
    {
        return config.restoreDefaultsTask().isPresent() && config.canPlayerEdit(player).asBoolean();
    }
}
