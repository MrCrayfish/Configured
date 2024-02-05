package com.mrcrayfish.configured.impl.framework.handler;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.network.ServerPlayHelper;
import com.mrcrayfish.configured.impl.framework.message.MessageFramework;
import com.mrcrayfish.configured.platform.Services;
import com.mrcrayfish.framework.api.config.AbstractProperty;
import com.mrcrayfish.framework.api.config.ConfigType;
import com.mrcrayfish.framework.api.config.event.FrameworkConfigEvents;
import com.mrcrayfish.framework.config.FrameworkConfigManager;
import com.mrcrayfish.framework.network.Network;
import com.mrcrayfish.framework.network.message.play.S2CSyncConfigData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;

/**
 * Author: MrCrayfish
 */
public class FrameworkServerHandler
{
    public static void handleServerSync(ServerPlayer player, MessageFramework.Sync message, Consumer<Component> disconnect)
    {
        if(!ServerPlayHelper.canEditServerConfigs(player))
            return;

        Constants.LOG.debug("Received Framework server config sync from player: {}", player.getName().getString());

        FrameworkConfigManager.FrameworkConfigImpl config = FrameworkConfigManager.getInstance().getConfig(message.id());
        if(config == null)
        {
            Constants.LOG.error("Client sent data for a config that doesn't exist: {}", message.id());
            disconnect.accept(Component.translatable("configured.multiplayer.disconnect.bad_config_packet"));
            return;
        }

        if(config.isReadOnly())
        {
            Constants.LOG.error("Client sent data for a read-only config '{}'", message.id());
            disconnect.accept(Component.translatable("configured.multiplayer.disconnect.bad_config_packet"));
            return;
        }

        if(!config.getType().isServer() || config.getType() == ConfigType.DEDICATED_SERVER)
        {
            Constants.LOG.error("Client sent data for a config is not supposed to be updated '{}'", message.id());
            disconnect.accept(Component.translatable("configured.multiplayer.disconnect.bad_config_packet"));
            return;
        }

        if(!config.isLoaded())
        {
            Constants.LOG.error("Client tried to perform sync update on an unloaded config. Something went wrong...");
            disconnect.accept(Component.translatable("configured.multiplayer.disconnect.bad_config_packet"));
            return;
        }

        if(processSyncData(message, true))
        {
            if(config.getType().isSync())
            {
                // Send back using Framework's network
                Network.getPlayChannel().sendToAll(new S2CSyncConfigData(message.id(), message.data()));
            }
            ServerPlayHelper.sendMessageToOperators(Component.translatable("configured.chat.config_updated", player.getName(), config.getFileName()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), player);
        }
        else
        {
            disconnect.accept(Component.translatable("configured.multiplayer.disconnect.bad_config_packet"));
        }
    }

    private static boolean processSyncData(MessageFramework.Sync message, boolean server)
    {
        FrameworkConfigManager.FrameworkConfigImpl config = FrameworkConfigManager.getInstance().getConfig(message.id());
        if(config == null)
        {
            Constants.LOG.error("No Framework config exists for the id: {}", message.id());
            return false;
        }

        if(server)
        {
            if(!config.getType().isServer())
            {
                Constants.LOG.error("Received sync update for incorrect Framework config: {}", message.id());
                return false;
            }
        }
        else if(!config.getType().isSync())
        {
            Constants.LOG.error("Received sync update for a non-sync Framework config: {}", message.id());
            return false;
        }

        if(config.isReadOnly() || !config.isLoaded())
        {
            Constants.LOG.error("Received sync update for incorrect Framework config: {}", message.id());
            return false;
        }

        try
        {
            CommentedConfig data = TomlFormat.instance().createParser().parse(new ByteArrayInputStream(message.data()));
            if(!config.isCorrect(data))
            {
                Constants.LOG.error("Received incorrect Framework config data");
                return false;
            }

            if(config.getConfig() instanceof Config c)
            {
                c.putAll(data);
                config.getAllProperties().forEach(AbstractProperty::invalidateCache);
                FrameworkConfigEvents.RELOAD.post().handle(config.getSource());
                Constants.LOG.debug("Successfully processed sync update for Framework config: {}", message.id());
                return true;
            }
        }
        catch(ParsingException e)
        {
            Constants.LOG.error("Received malformed Framework config data", e);
        }
        catch(Exception e)
        {
            Constants.LOG.error("An exception was thrown when processing Framework config data", e);
        }
        return false;
    }

    public static void handleRequestConfig(ServerPlayer player, MessageFramework.Request message, Consumer<Component> disconnect)
    {
        if(!ServerPlayHelper.canEditServerConfigs(player))
            return;

        Constants.LOG.debug("Received Framework config request from player: {}", player.getName().getString());

        FrameworkConfigManager.FrameworkConfigImpl config = FrameworkConfigManager.getInstance().getConfig(message.id());
        if(config == null)
        {
            Constants.LOG.error("Client tried requesting a Framework config that doesn't exist: {}", message.id());
            disconnect.accept(Component.translatable("configured.multiplayer.disconnect.bad_config_packet"));
            return;
        }

        if(!config.getType().isServer() || config.getType().isSync() || config.getType() == ConfigType.DEDICATED_SERVER)
        {
            Constants.LOG.error("Client tried requesting a Framework config that is not allowed to be requested: '{}'", message.id());
            disconnect.accept(Component.translatable("configured.multiplayer.disconnect.bad_config_packet"));
            return;
        }

        if(!config.isLoaded())
        {
            Constants.LOG.error("The Framework config '{}' the client was requesting is not loaded. Something went terribly wrong...", message.id());
            disconnect.accept(Component.translatable("configured.multiplayer.disconnect.bad_config_packet"));
            return;
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        TomlFormat.instance().createWriter().write(config.getConfig(), stream);
        Services.PLATFORM.sendFrameworkConfigResponse(player, stream.toByteArray());
    }
}
