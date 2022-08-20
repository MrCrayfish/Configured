package com.mrcrayfish.configured.network.play;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.config.ConfigManager;
import com.mrcrayfish.configured.network.PacketHandler;
import com.mrcrayfish.configured.network.message.MessageSyncServerConfig;
import com.mrcrayfish.configured.network.message.MessageSyncSimpleConfig;
import com.mrcrayfish.configured.util.ConfigHelper;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.loading.FMLEnvironment;
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
        Optional.ofNullable(ConfigHelper.getModConfig(message.getFileName())).ifPresent(config ->
        {
            CommentedConfig data = TomlFormat.instance().createParser().parse(new ByteArrayInputStream(message.getData()));
            config.getConfigData().putAll(data);
            ConfigHelper.resetCache(config);
            PacketHandler.getPlayChannel().send(PacketDistributor.ALL.with(() -> null), new MessageSyncServerConfig(message.getFileName(), message.getData()));
        });
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
        ConfigManager.getInstance().processSyncData(message);
    }
}
