package com.mrcrayfish.configured.network.play;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.network.PacketHandler;
import com.mrcrayfish.configured.network.message.MessageSyncServerConfig;
import com.mrcrayfish.configured.util.ConfigHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fmllegacy.network.PacketDistributor;

import java.io.ByteArrayInputStream;
import java.util.Optional;

/**
 * Author: MrCrayfish
 */
public class ServerPlayHandler
{
    public static void handleSyncServerConfigMessage(ServerPlayer player, MessageSyncServerConfig message)
    {
        if(!player.hasPermissions(player.server.getOperatorUserPermissionLevel()))
        {
            Configured.LOGGER.warn("{} tried to update server config without operator status", player.getName().getString());
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
}
