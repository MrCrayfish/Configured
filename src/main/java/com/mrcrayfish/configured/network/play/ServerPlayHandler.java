package com.mrcrayfish.configured.network.play;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.mrcrayfish.configured.Config;
import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.impl.simple.SimpleConfigManager;
import com.mrcrayfish.configured.network.message.MessageResponseSimpleConfig;
import com.mrcrayfish.configured.network.message.MessageSyncSimpleConfig;
import com.mrcrayfish.configured.util.ConfigHelper;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Author: MrCrayfish
 */
public class ServerPlayHandler
{
    private static final Joiner DOT_JOINER = Joiner.on(".");

    public static void handleSyncSimpleConfigMessage(Connection connection, ServerPlayer player, ResourceLocation id, byte[] data)
    {
        if(!canEditServerConfigs(player))
            return;

        Configured.LOGGER.debug("Received server config sync from player: {}", player.getName().getString());

        SimpleConfigManager.SimpleConfigImpl config = SimpleConfigManager.getInstance().getConfig(id);
        if(config == null)
        {
            Configured.LOGGER.error("Client sent data for a config that doesn't exist: {}", id);
            connection.disconnect(Component.translatable("configured.multiplayer.disconnect.bad_config_packet"));
            return;
        }

        if(config.isReadOnly())
        {
            Configured.LOGGER.error("Client sent data for a read-only config '{}'", id);
            connection.disconnect(Component.translatable("configured.multiplayer.disconnect.bad_config_packet"));
            return;
        }

        if(!config.getType().isServer() || config.getType() == ConfigType.DEDICATED_SERVER)
        {
            Configured.LOGGER.error("Client sent data for a config is not supposed to be updated '{}'", id);
            connection.disconnect(Component.translatable("configured.multiplayer.disconnect.bad_config_packet"));
            return;
        }

        if(!config.isLoaded())
        {
            Configured.LOGGER.error("Client tried to perform sync update on an unloaded config. Something went wrong...");
            connection.disconnect(Component.translatable("configured.multiplayer.disconnect.bad_config_packet"));
            return;
        }

        if(SimpleConfigManager.getInstance().processSyncData(id, data, true))
        {
            if(config.getType().isSync())
            {
                PlayerLookup.all(player.server).forEach(player1 -> {
                    ServerPlayNetworking.send(player1, MessageSyncSimpleConfig.ID, MessageSyncSimpleConfig.create(id, data));
                });
            }
            sendMessageToOperators(player.server, Component.translatable("configured.chat.config_updated", player.getName(), config.getFileName()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        else
        {
            connection.disconnect(Component.translatable("configured.multiplayer.disconnect.bad_config_packet"));
        }
    }

    private static void sendMessageToOperators(MinecraftServer server, Component message)
    {
        Preconditions.checkNotNull(server, "The server was null when broadcasting config changes. This should not be possible...");
        for(ServerPlayer serverPlayer : server.getPlayerList().getPlayers())
        {
            if(server.getPlayerList().isOp(serverPlayer.getGameProfile()))
            {
                serverPlayer.sendSystemMessage(message);
            }
        }
    }

    public static void handleRequestSimpleConfigMessage(ServerPlayer player, ResourceLocation id)
    {
        if(!canEditServerConfigs(player))
            return;

        Configured.LOGGER.debug("Received config request from player: {}", player.getName().getString());

        SimpleConfigManager.SimpleConfigImpl config = SimpleConfigManager.getInstance().getConfig(id);
        if(config == null)
        {
            Configured.LOGGER.warn("{} tried to request server config which does not exist!", player.getName().getString());
            player.connection.disconnect(Component.translatable("configured.multiplayer.disconnect.no_permission"));
            return;
        }

        if(!config.getType().isServer() || config.getType() == ConfigType.DEDICATED_SERVER)
        {
            Configured.LOGGER.warn("{} tried to request an invalid config from the server", player.getName().getString());
            player.connection.disconnect(Component.translatable("configured.multiplayer.disconnect.no_permission"));
            return;
        }

        try
        {
            ResourceLocation key = config.getName();
            byte[] data = config.getData();
            ServerPlayNetworking.send(player, MessageResponseSimpleConfig.ID, MessageResponseSimpleConfig.create(key, data));
            Configured.LOGGER.debug("Sending request reply back to player");
        }
        catch(Exception e)
        {
            Configured.LOGGER.warn("An exception occurred to read server config: {}", config.getFilePath());
            ServerPlayNetworking.send(player, MessageResponseSimpleConfig.ID, MessageResponseSimpleConfig.create(config.getName(), new byte[]{}));
        }
    }

    private static boolean canEditServerConfigs(ServerPlayer player)
    {
        MinecraftServer server = player.getServer();
        if(server == null || !server.isDedicatedServer() || !Config.DEVELOPER.enabled.get())
        {
            Configured.LOGGER.warn("{} tried to request or update a server config, however developer mode is not enabled", player.getName().getString());
            player.connection.disconnect(Component.translatable("configured.multiplayer.disconnect.unauthorized_request"));
            sendMessageToOperators(player.server, Component.translatable("configured.chat.authorized_player").withStyle(ChatFormatting.RED));
            return false;
        }

        if(!Config.DEVELOPER.developers.get().contains(player.getStringUUID()) || !server.getPlayerList().isOp(player.getGameProfile()))
        {
            Configured.LOGGER.warn("{} tried to request or update a server config, however they are not a developer", player.getName().getString());
            player.connection.disconnect(Component.translatable("configured.multiplayer.disconnect.unauthorized_request"));
            sendMessageToOperators(player.server, Component.translatable("configured.chat.authorized_player").withStyle(ChatFormatting.RED));
            return false;
        }

        return true;
    }
}
