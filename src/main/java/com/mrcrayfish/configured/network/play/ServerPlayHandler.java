package com.mrcrayfish.configured.network.play;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.common.base.Preconditions;
import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.config.ConfigManager;
import com.mrcrayfish.configured.network.PacketHandler;
import com.mrcrayfish.configured.network.message.MessageSyncServerConfig;
import com.mrcrayfish.configured.network.message.MessageSyncSimpleConfig;
import com.mrcrayfish.configured.util.ConfigHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.network.PacketDistributor;

import java.io.ByteArrayInputStream;
import java.util.Optional;

/**
 * Author: MrCrayfish
 */
public class ServerPlayHandler
{
    public static void handleSyncServerConfigMessage(ServerPlayer player, MessageSyncServerConfig message)
    {
        if(!player.hasPermissions(player.server.getOperatorUserPermissionLevel()) && !ConfigHelper.isServerOwnedByPlayer(player))
        {
            Configured.LOGGER.warn("{} tried to update server config without operator status", player.getName().getString());
            player.connection.disconnect(new TranslatableComponent("configured.multiplayer.disconnect.no_permission"));
            return;
        }

        Configured.LOGGER.debug("Received server config sync from player: {}", player.getName().getString());

        ModConfig config = ConfigHelper.getModConfig(message.getFileName());
        if(config == null)
        {
            Configured.LOGGER.warn("{} tried to update a config that doesn't exist!", player.getName().getString());
            return;
        }

        CommentedConfig data = TomlFormat.instance().createParser().parse(new ByteArrayInputStream(message.getData()));
        config.getConfigData().putAll(data);
        ConfigHelper.resetCache(config);

        Configured.LOGGER.debug("Successfully processed config update for '" + message.getFileName() + "'");

        PacketHandler.getPlayChannel().send(PacketDistributor.ALL.noArg(), new MessageSyncServerConfig(message.getFileName(), message.getData()));
        sendMessageToOperators(player, new TranslatableComponent("configured.chat.config_updated", player.getName(), config.getFileName()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    public static void handleSyncSimpleConfigMessage(ServerPlayer player, MessageSyncSimpleConfig message)
    {
        if(!player.hasPermissions(player.server.getOperatorUserPermissionLevel()) && !ConfigHelper.isServerOwnedByPlayer(player))
        {
            Configured.LOGGER.warn("{} tried to update server config without operator status", player.getName().getString());
            player.connection.disconnect(new TranslatableComponent("configured.multiplayer.disconnect.no_permission"));
            return;
        }

        Configured.LOGGER.debug("Received server config sync from player: {}", player.getName().getString());
        ConfigManager.getInstance().processSyncData(message, entry ->
        {
            if(entry.isPresent())
            {
                PacketHandler.getPlayChannel().send(PacketDistributor.ALL.noArg(), new MessageSyncSimpleConfig(message.getId(), message.getData()));
                sendMessageToOperators(player, new TranslatableComponent("configured.chat.config_updated", player.getName(), entry.get().getFileName()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
            else
            {
                player.sendMessage(new TranslatableComponent("configured.chat.config_update_error").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), Util.NIL_UUID);
            }
        });
    }

    private static void sendMessageToOperators(ServerPlayer player, Component message)
    {
        //TODO config option to disable this
        MinecraftServer server = player.getServer();
        Preconditions.checkNotNull(server, "The server was null when broadcasting config changes. This should not be possible...");
        for(ServerPlayer serverPlayer : server.getPlayerList().getPlayers())
        {
            if(serverPlayer != player && server.getPlayerList().isOp(serverPlayer.getGameProfile()))
            {
                serverPlayer.sendMessage(message, Util.NIL_UUID);
            }
        }
    }
}
