package com.mrcrayfish.configured.network.handler;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.common.base.Joiner;
import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.network.ServerPlayHelper;
import com.mrcrayfish.configured.network.payload.SyncNeoForgeConfigPayload;
import com.mrcrayfish.configured.util.NeoForgeConfigHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.ByteArrayInputStream;

/**
 * Author: MrCrayfish
 */
public class NeoForgeServerPlayHandler
{
    private static final Joiner DOT_JOINER = Joiner.on(".");

    public static void handleSyncServerConfigMessage(ServerPlayer player, SyncNeoForgeConfigPayload payload)
    {
        if(!ServerPlayHelper.canEditServerConfigs(player))
            return;

        Constants.LOG.debug("Received server config sync from player: {}", player.getName().getString());

        ModConfig config = NeoForgeConfigHelper.getModConfig(payload.fileName());
        if(config == null)
        {
            Constants.LOG.warn("{} tried to update a config that doesn't exist!", player.getName().getString());
            player.connection.disconnect(Component.translatable("configured.multiplayer.disconnect.bad_config_packet"));
            return;
        }

        if(config.getType() != ModConfig.Type.SERVER)
        {
            Constants.LOG.warn("{} tried to update a forge config that isn't a server type", player.getName().getString());
            player.connection.disconnect(Component.translatable("configured.multiplayer.disconnect.bad_config_packet"));
            return;
        }

        if(!(config.getSpec() instanceof ModConfigSpec))
        {
            Constants.LOG.warn("Unable to process server config update due to unknown spec for config: {}", payload.fileName());
            player.connection.disconnect(Component.translatable("configured.multiplayer.disconnect.bad_config_packet"));
            return;
        }

        try
        {
            CommentedConfig data = TomlFormat.instance().createParser().parse(new ByteArrayInputStream(payload.data()));
            int result = ((ModConfigSpec) config.getSpec()).correct(data,
                    (action, path, incorrectValue, correctedValue) ->
                            Constants.LOG.warn("Incorrect key {} was corrected from {} to its default, {}. {}", DOT_JOINER.join(path), incorrectValue, correctedValue, incorrectValue == correctedValue ? "This seems to be an error." : ""),
                    (action, path, incorrectValue, correctedValue) ->
                            Constants.LOG.debug("The comment on key {} does not match the spec. This may create a backup.", DOT_JOINER.join( path )));
            if(result != 0)
            {
                Constants.LOG.info("Config data sent from {} needed to be corrected", player.getName().getString());
            }
            config.getConfigData().putAll(data);
        }
        catch(ParsingException e)
        {
            Constants.LOG.warn("{} sent malformed config data to the server", player.getName().getString());
            player.connection.disconnect(Component.translatable("configured.multiplayer.disconnect.invalid_config_packet"));
            ServerPlayHelper.sendMessageToOperators(Component.translatable("configured.chat.malformed_config_data", player.getName(), Component.literal(config.getFileName()).withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.RED), player);
            return;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return;
        }

        Constants.LOG.debug("Successfully processed config update for '" + payload.fileName() + "'");

        PacketDistributor.sendToAllPlayers(new SyncNeoForgeConfigPayload(payload.fileName(), payload.data()));
        ServerPlayHelper.sendMessageToOperators(Component.translatable("configured.chat.config_updated", player.getName(), config.getFileName()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), player);
    }
}
