package com.mrcrayfish.configured.network.handler;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.concurrent.SynchronizedConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.api.ActionResult;
import com.mrcrayfish.configured.impl.neoforge.NeoForgeConfig;
import com.mrcrayfish.configured.network.ServerPlayHelper;
import com.mrcrayfish.configured.network.payload.SyncNeoForgeConfigPayload;
import com.mrcrayfish.configured.util.NeoForgeConfigHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Author: MrCrayfish
 */
public class NeoForgeServerPlayHandler
{
    public static void handleSyncServerConfigMessage(Player sender, SyncNeoForgeConfigPayload payload)
    {
        if(!(sender instanceof ServerPlayer player))
            return;

        Constants.LOG.debug("Received NeoForge server config sync from player: {}", sender.getName().getString());

        if(!ServerPlayHelper.canEditServerConfigs(player))
            return;

        ModConfig modConfig = NeoForgeConfigHelper.getModConfig(payload.fileName());
        if(modConfig == null)
        {
            Constants.LOG.warn("{} tried to update a NeoForge config that doesn't exist!", player.getName().getString());
            player.connection.disconnect(Component.translatable("configured.multiplayer.disconnect.bad_config_packet"));
            return;
        }

        if(modConfig.getType() != ModConfig.Type.SERVER)
        {
            Constants.LOG.warn("{} tried to update a NeoForge config that isn't a server type", player.getName().getString());
            player.connection.disconnect(Component.translatable("configured.multiplayer.disconnect.bad_config_packet"));
            return;
        }

        // TODO eventually make this automatically checked
        NeoForgeConfig config = new NeoForgeConfig(modConfig);
        ActionResult permission = config.canPlayerEdit(player);
        if(!permission.asBoolean())
        {
            Constants.LOG.warn("{} tried to update the NeoForge config '{}' but didn't have permission", player.getName().getString(), modConfig.getFileName());
            player.connection.disconnect(Component.translatable("configured.multiplayer.disconnect.unauthorized_request"));
            return;
        }

        try
        {
            if(modConfig.getSpec() instanceof ModConfigSpec spec)
            {
                // Read the config data
                SynchronizedConfig updatedConfig = new SynchronizedConfig(TomlFormat.instance(), LinkedHashMap::new);
                updatedConfig.bulkCommentedUpdate(view -> {
                    TomlFormat.instance().createParser().parse(new ByteArrayInputStream(payload.data()), view, ParsingMode.REPLACE);
                });

                // Try and correct the config
                AtomicBoolean malformed = new AtomicBoolean();
                int result = spec.correct(updatedConfig, (action, path, incorrectValue, correctedValue) -> {
                    if(action == ConfigSpec.CorrectionAction.ADD || action == ConfigSpec.CorrectionAction.REMOVE) {
                        malformed.set(true); // We want to know if data was added or removed
                    } else if (action == ConfigSpec.CorrectionAction.REPLACE) {
                        Constants.LOG.warn("The value for path \"{}\" was originally \"{}\" but was corrected to \"{}\"", path, incorrectValue, correctedValue);
                    }
                });

                // If config data is missing or added new entries, reject update
                if(malformed.get())
                {
                    Constants.LOG.warn("{} sent malformed config data when updating a NeoForge config: {}", player.getName().getString(), modConfig.getFileName());
                    player.connection.disconnect(Component.translatable("configured.multiplayer.disconnect.bad_config_packet"));
                    return;
                }

                if(result != 0)
                {
                    Constants.LOG.debug("Config data sent from {} needed to be corrected", player.getName().getString());
                }

                NeoForgeConfigHelper.setConfigData(modConfig, updatedConfig);
            }
        }
        catch(ParsingException e)
        {
            Constants.LOG.warn("{} sent malformed config data to the server", player.getName().getString());
            player.connection.disconnect(Component.translatable("configured.multiplayer.disconnect.bad_config_packet"));
            ServerPlayHelper.sendMessageToOperators(Component.translatable("configured.chat.malformed_config_data", player.getName(), Component.literal(modConfig.getFileName()).withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.RED), player);
            return;
        }
        catch(Exception e)
        {
            Constants.LOG.warn("Failed to process config data sent by {}", player.getName().getString());
            player.connection.disconnect(Component.translatable("configured.multiplayer.disconnect.bad_config_packet"));
            ServerPlayHelper.sendMessageToOperators(Component.translatable("configured.chat.failed_config_update", Component.literal(modConfig.getFileName()).withStyle(ChatFormatting.GRAY), player.getName()).withStyle(ChatFormatting.RED), player);
            return;
        }

        Constants.LOG.debug("Successfully processed config update for '{}'", payload.fileName());
        ServerPlayHelper.sendMessageToOperators(Component.translatable("configured.chat.config_updated", player.getName(), modConfig.getFileName()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), player);

        // Kick all other players and ask them to rejoin
        MinecraftServer server = player.getServer();
        if(server != null)
        {
            server.getPlayerList().getPlayers().forEach(player1 -> {
                if(!player1.equals(player)) {
                    player1.connection.disconnect(Component.translatable("configured.gui.neoforge.server_configs_updated"));
                }
            });
        }

    }
}
