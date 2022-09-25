package com.mrcrayfish.configured.network.play;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.client.SessionData;
import com.mrcrayfish.configured.client.screen.RequestScreen;
import com.mrcrayfish.configured.impl.simple.SimpleConfigManager;
import com.mrcrayfish.configured.network.message.MessageResponseSimpleConfig;
import com.mrcrayfish.configured.network.message.MessageSessionData;
import com.mrcrayfish.configured.network.message.MessageSyncForgeConfig;
import com.mrcrayfish.configured.network.message.MessageSyncSimpleConfig;
import com.mrcrayfish.configured.util.ConfigHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.network.NetworkEvent;

import java.io.ByteArrayInputStream;

/**
 * Author: MrCrayfish
 */
public class ClientPlayHandler
{
    public static void handleSyncServerConfigMessage(NetworkEvent.Context context, MessageSyncForgeConfig message)
    {
        // Avoid updating config if packet was sent to self
        if(Minecraft.getInstance().isLocalServer())
            return;

        Configured.LOGGER.info("Received forge config sync from server");

        ModConfig config = ConfigHelper.getForgeConfig(message.fileName());
        if(config == null)
        {
            Configured.LOGGER.error("Server sent data for a forge config that doesn't exist: {}", message.fileName());
            context.getNetworkManager().disconnect(new TranslatableComponent("configured.multiplayer.disconnect.process_config"));
            return;
        }

        if(config.getType() != ModConfig.Type.SERVER)
        {
            Configured.LOGGER.error("Server sent data for a config that isn't a server type: {}", message.fileName());
            context.getNetworkManager().disconnect(new TranslatableComponent("configured.multiplayer.disconnect.process_config"));
            return;
        }

        try
        {
            CommentedConfig data = TomlFormat.instance().createParser().parse(new ByteArrayInputStream(message.data()));
            config.getSpec().acceptConfig(data);
            ConfigHelper.fireForgeConfigEvent(config, new ModConfigEvent.Reloading(config));
        }
        catch(ParsingException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void handleSyncSimpleConfigMessage(NetworkEvent.Context context, MessageSyncSimpleConfig message)
    {
        // Avoid updating config if packet was sent to self
        if(Minecraft.getInstance().isLocalServer())
            return;

        Configured.LOGGER.debug("Received simple config sync from server");

        SimpleConfigManager.SimpleConfigImpl config = SimpleConfigManager.getInstance().getConfig(message.id());
        if(config == null)
        {
            Configured.LOGGER.error("Server sent data for a config that doesn't exist: {}", message.id());
            context.getNetworkManager().disconnect(new TranslatableComponent("configured.multiplayer.disconnect.process_config"));
            return;
        }

        if(config.isReadOnly())
        {
            Configured.LOGGER.error("Server sent data for a read-only config '{}'. This should not happen!", message.id());
            context.getNetworkManager().disconnect(new TranslatableComponent("configured.multiplayer.disconnect.process_config"));
            return;
        }

        if(!config.getType().isSync())
        {
            Configured.LOGGER.error("Server sent data for non-sync config '{}'. This should not happen!", message.id());
            context.getNetworkManager().disconnect(new TranslatableComponent("configured.multiplayer.disconnect.process_config"));
            return;
        }

        if(!config.isLoaded())
        {
            Configured.LOGGER.error("Tried to perform sync update on an unloaded config. Something went wrong...");
            context.getNetworkManager().disconnect(new TranslatableComponent("configured.multiplayer.disconnect.process_config"));
            return;
        }

        if(!SimpleConfigManager.getInstance().processSyncData(message, false))
        {
            context.getNetworkManager().disconnect(new TranslatableComponent("configured.multiplayer.disconnect.process_config"));
        }
    }

    public static void handleResponseSimpleConfigMessage(MessageResponseSimpleConfig message)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if(minecraft.screen instanceof RequestScreen screen)
        {
            SimpleConfigManager.SimpleConfigImpl entry = SimpleConfigManager.getInstance().getConfig(message.id());
            if(entry == null || screen.getActiveConfig() != entry)
            {
                screen.handleResponse(null, new TranslatableComponent("configured.gui.request.invalid_config"));
                return;
            }

            // Don't load since already loaded on local server
            if(!Minecraft.getInstance().isLocalServer())
            {
                if(!SimpleConfigManager.getInstance().processResponseData(message))
                {
                    screen.handleResponse(null, new TranslatableComponent("configured.gui.request.process_error"));
                    return;
                }
            }

            screen.handleResponse(entry, null);
        }
    }

    public static void handleJoinMessage(MessageSessionData data)
    {
        SessionData.setDeveloper(data.isDeveloper());
        SessionData.setLan(data.isLan());
    }
}
