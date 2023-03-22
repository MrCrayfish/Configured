package com.mrcrayfish.configured.network.play;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.mrcrayfish.configured.Config;
import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.impl.simple.SimpleConfigManager;
import com.mrcrayfish.configured.network.Network;
import com.mrcrayfish.configured.network.message.play.C2SMessageRequestSimpleConfig;
import com.mrcrayfish.configured.network.message.play.MessageSyncSimpleConfig;
import com.mrcrayfish.configured.network.message.play.S2CMessageResponseSimpleConfig;
import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Author: MrCrayfish
 */
public class ServerPlayHandler
{
    public static void handleSyncSimpleConfigMessage(MessageContext context, ServerPlayer player, MessageSyncSimpleConfig message)
    {
        if(!canEditServerConfigs(player))
            return;

        Constants.LOG.debug("Received server config sync from player: {}", player.getName().getString());

        SimpleConfigManager.SimpleConfigImpl config = SimpleConfigManager.getInstance().getConfig(message.id());
        if(config == null)
        {
            Constants.LOG.error("Client sent data for a config that doesn't exist: {}", message.id());
            context.getNetworkManager().disconnect(Component.translatable("configured.multiplayer.disconnect.bad_config_packet"));
            return;
        }

        if(config.isReadOnly())
        {
            Constants.LOG.error("Client sent data for a read-only config '{}'", message.id());
            context.getNetworkManager().disconnect(Component.translatable("configured.multiplayer.disconnect.bad_config_packet"));
            return;
        }

        if(!config.getType().isServer() || config.getType() == ConfigType.DEDICATED_SERVER)
        {
            Constants.LOG.error("Client sent data for a config is not supposed to be updated '{}'", message.id());
            context.getNetworkManager().disconnect(Component.translatable("configured.multiplayer.disconnect.bad_config_packet"));
            return;
        }

        if(!config.isLoaded())
        {
            Constants.LOG.error("Client tried to perform sync update on an unloaded config. Something went wrong...");
            context.getNetworkManager().disconnect(Component.translatable("configured.multiplayer.disconnect.bad_config_packet"));
            return;
        }

        if(SimpleConfigManager.getInstance().processSyncData(message, true))
        {
            if(config.getType().isSync())
            {
                Network.getPlay().sendToAll(new MessageSyncSimpleConfig(message.id(), message.data()));
            }
            sendMessageToOperators(Component.translatable("configured.chat.config_updated", player.getName(), config.getFileName()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), player);
        }
        else
        {
            context.getNetworkManager().disconnect(Component.translatable("configured.multiplayer.disconnect.bad_config_packet"));
        }
    }

    public static void sendMessageToOperators(Component message, ServerPlayer player)
    {
        MinecraftServer server = player.getServer();
        Preconditions.checkNotNull(server, "The server was null when broadcasting config changes. This should not be possible...");
        for(ServerPlayer serverPlayer : server.getPlayerList().getPlayers())
        {
            if(server.getPlayerList().isOp(serverPlayer.getGameProfile()))
            {
                serverPlayer.sendSystemMessage(message);
            }
        }
    }

    public static void handleRequestSimpleConfigMessage(ServerPlayer player, C2SMessageRequestSimpleConfig message, MessageContext context)
    {
        if(!canEditServerConfigs(player))
            return;

        Constants.LOG.debug("Received config request from player: {}", player.getName().getString());

        SimpleConfigManager.SimpleConfigImpl config = SimpleConfigManager.getInstance().getConfig(message.id());
        if(config == null)
        {
            Constants.LOG.warn("{} tried to request server config which does not exist!", player.getName().getString());
            player.connection.disconnect(Component.translatable("configured.multiplayer.disconnect.no_permission"));
            return;
        }

        if(!config.getType().isServer() || config.getType() == ConfigType.DEDICATED_SERVER)
        {
            Constants.LOG.warn("{} tried to request an invalid config from the server", player.getName().getString());
            player.connection.disconnect(Component.translatable("configured.multiplayer.disconnect.no_permission"));
            return;
        }

        try
        {
            ResourceLocation key = config.getName();
            byte[] data = config.getData();
            context.reply(new S2CMessageResponseSimpleConfig(key, data));
            Constants.LOG.debug("Sending request reply back to player");
        }
        catch(Exception e)
        {
            Constants.LOG.warn("An exception occurred to read server config: {}", config.getFilePath());
            context.reply(new S2CMessageResponseSimpleConfig(config.getName(), new byte[]{}));
        }
    }

    public static boolean canEditServerConfigs(ServerPlayer player)
    {
        MinecraftServer server = player.getServer();
        if(server == null || !server.isDedicatedServer() || !Config.DEVELOPER.enabled.get())
        {
            Constants.LOG.warn("{} tried to request or update a server config, however developer mode is not enabled", player.getName().getString());
            player.connection.disconnect(Component.translatable("configured.multiplayer.disconnect.unauthorized_request"));
            sendMessageToOperators(Component.translatable("configured.chat.authorized_player").withStyle(ChatFormatting.RED), player);
            return false;
        }

        if(!Config.DEVELOPER.developers.get().contains(player.getStringUUID()) || !server.getPlayerList().isOp(player.getGameProfile()))
        {
            Constants.LOG.warn("{} tried to request or update a server config, however they are not a developer", player.getName().getString());
            player.connection.disconnect(Component.translatable("configured.multiplayer.disconnect.unauthorized_request"));
            sendMessageToOperators(Component.translatable("configured.chat.authorized_player").withStyle(ChatFormatting.RED), player);
            return false;
        }

        return true;
    }
}
