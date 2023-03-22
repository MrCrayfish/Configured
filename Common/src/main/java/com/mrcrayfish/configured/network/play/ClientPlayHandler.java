package com.mrcrayfish.configured.network.play;

import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.client.SessionData;
import com.mrcrayfish.configured.client.screen.RequestScreen;
import com.mrcrayfish.configured.impl.simple.SimpleConfigManager;
import com.mrcrayfish.configured.network.message.play.MessageSyncSimpleConfig;
import com.mrcrayfish.configured.network.message.play.S2CMessageResponseSimpleConfig;
import com.mrcrayfish.configured.network.message.play.S2CMessageSessionData;
import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Author: MrCrayfish
 */
public class ClientPlayHandler
{
    public static void handleSyncSimpleConfigMessage(MessageContext context, MessageSyncSimpleConfig message)
    {
        // Avoid updating config if packet was sent to self
        if(Minecraft.getInstance().isLocalServer())
            return;

        Constants.LOG.debug("Received simple config sync from server");

        SimpleConfigManager.SimpleConfigImpl config = SimpleConfigManager.getInstance().getConfig(message.id());
        if(config == null)
        {
            Constants.LOG.error("Server sent data for a config that doesn't exist: {}", message.id());
            context.getNetworkManager().disconnect(Component.translatable("configured.multiplayer.disconnect.process_config"));
            return;
        }

        if(config.isReadOnly())
        {
            Constants.LOG.error("Server sent data for a read-only config '{}'. This should not happen!", message.id());
            context.getNetworkManager().disconnect(Component.translatable("configured.multiplayer.disconnect.process_config"));
            return;
        }

        if(!config.getType().isSync())
        {
            Constants.LOG.error("Server sent data for non-sync config '{}'. This should not happen!", message.id());
            context.getNetworkManager().disconnect(Component.translatable("configured.multiplayer.disconnect.process_config"));
            return;
        }

        if(!config.isLoaded())
        {
            Constants.LOG.error("Tried to perform sync update on an unloaded config. Something went wrong...");
            context.getNetworkManager().disconnect(Component.translatable("configured.multiplayer.disconnect.process_config"));
            return;
        }

        if(!SimpleConfigManager.getInstance().processSyncData(message, false))
        {
            context.getNetworkManager().disconnect(Component.translatable("configured.multiplayer.disconnect.process_config"));
        }
    }

    public static void handleResponseSimpleConfigMessage(S2CMessageResponseSimpleConfig message)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if(minecraft.screen instanceof RequestScreen screen)
        {
            SimpleConfigManager.SimpleConfigImpl entry = SimpleConfigManager.getInstance().getConfig(message.id());
            if(entry == null || screen.getActiveConfig() != entry)
            {
                screen.handleResponse(null, Component.translatable("configured.gui.request.invalid_config"));
                return;
            }

            // Don't load since already loaded on local server
            if(!Minecraft.getInstance().isLocalServer())
            {
                if(!SimpleConfigManager.getInstance().processResponseData(message))
                {
                    screen.handleResponse(null, Component.translatable("configured.gui.request.process_error"));
                    return;
                }
            }

            screen.handleResponse(entry, null);
        }
    }

    public static void handleJoinMessage(S2CMessageSessionData data)
    {
        SessionData.setDeveloper(data.isDeveloper());
        SessionData.setLan(data.isLan());
    }
}
