package com.mrcrayfish.configured.network.play;

import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.client.SessionData;
import com.mrcrayfish.configured.client.screen.RequestScreen;
import com.mrcrayfish.configured.impl.simple.SimpleConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Author: MrCrayfish
 */
public class ClientPlayHandler
{
    public static void handleSyncSimpleConfigMessage(Connection connection, ResourceLocation id, byte[] data)
    {
        // Avoid updating config if packet was sent to self
        if(Minecraft.getInstance().isLocalServer())
            return;

        Configured.LOGGER.debug("Received simple config sync from server");

        SimpleConfigManager.SimpleConfigImpl config = SimpleConfigManager.getInstance().getConfig(id);
        if(config == null)
        {
            Configured.LOGGER.error("Server sent data for a config that doesn't exist: {}", id);
            connection.disconnect(Component.translatable("configured.multiplayer.disconnect.process_config"));
            return;
        }

        if(config.isReadOnly())
        {
            Configured.LOGGER.error("Server sent data for a read-only config '{}'. This should not happen!", id);
            connection.disconnect(Component.translatable("configured.multiplayer.disconnect.process_config"));
            return;
        }

        if(!config.getType().isSync())
        {
            Configured.LOGGER.error("Server sent data for non-sync config '{}'. This should not happen!", id);
            connection.disconnect(Component.translatable("configured.multiplayer.disconnect.process_config"));
            return;
        }

        if(!config.isLoaded())
        {
            Configured.LOGGER.error("Tried to perform sync update on an unloaded config. Something went wrong...");
            connection.disconnect(Component.translatable("configured.multiplayer.disconnect.process_config"));
            return;
        }

        if(!SimpleConfigManager.getInstance().processSyncData(id, data, false))
        {
            connection.disconnect(Component.translatable("configured.multiplayer.disconnect.process_config"));
        }
    }

    public static void handleResponseSimpleConfigMessage(ResourceLocation id, byte[] data)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if(minecraft.screen instanceof RequestScreen screen)
        {
            SimpleConfigManager.SimpleConfigImpl entry = SimpleConfigManager.getInstance().getConfig(id);
            if(entry == null || screen.getActiveConfig() != entry)
            {
                screen.handleResponse(null, Component.translatable("configured.gui.request.invalid_config"));
                return;
            }

            // Don't load since already loaded on local server
            if(!Minecraft.getInstance().isLocalServer())
            {
                if(!SimpleConfigManager.getInstance().processResponseData(id, data))
                {
                    screen.handleResponse(null, Component.translatable("configured.gui.request.process_error"));
                    return;
                }
            }

            screen.handleResponse(entry, null);
        }
    }

    public static void handleJoinMessage(boolean developer, boolean lan)
    {
        SessionData.setDeveloper(developer);
        SessionData.setLan(lan);
    }
}
