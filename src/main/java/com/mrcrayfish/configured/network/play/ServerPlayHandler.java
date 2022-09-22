package com.mrcrayfish.configured.network.play;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.mrcrayfish.configured.Config;
import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.impl.simple.SimpleConfigManager;
import com.mrcrayfish.configured.network.PacketHandler;
import com.mrcrayfish.configured.network.message.MessageRequestSimpleConfig;
import com.mrcrayfish.configured.network.message.MessageResponseSimpleConfig;
import com.mrcrayfish.configured.network.message.MessageSyncForgeConfig;
import com.mrcrayfish.configured.network.message.MessageSyncSimpleConfig;
import com.mrcrayfish.configured.util.ConfigHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.ByteArrayInputStream;

/**
 * Author: MrCrayfish
 */
public class ServerPlayHandler
{
    private static final Joiner DOT_JOINER = Joiner.on(".");

    public static void handleSyncServerConfigMessage(ServerPlayer player, MessageSyncForgeConfig message)
    {
        if(!canEditServerConfigs(player))
            return;

        Configured.LOGGER.debug("Received server config sync from player: {}", player.getName().getString());

        ModConfig config = ConfigHelper.getForgeConfig(message.fileName());
        if(config == null)
        {
            Configured.LOGGER.warn("{} tried to update a config that doesn't exist!", player.getName().getString());
            player.connection.disconnect(new TranslatableComponent("configured.multiplayer.disconnect.bad_config_packet"));
            return;
        }

        if(config.getType() != ModConfig.Type.SERVER)
        {
            Configured.LOGGER.warn("{} tried to update a forge config that isn't a server type", player.getName().getString());
            player.connection.disconnect(new TranslatableComponent("configured.multiplayer.disconnect.bad_config_packet"));
            return;
        }

        if(!(config.getSpec() instanceof ForgeConfigSpec))
        {
            Configured.LOGGER.warn("Unable to process server config update due to unknown spec for config: {}", message.fileName());
            player.connection.disconnect(new TranslatableComponent("configured.multiplayer.disconnect.bad_config_packet"));
            return;
        }

        try
        {
            CommentedConfig data = TomlFormat.instance().createParser().parse(new ByteArrayInputStream(message.data()));
            int result = ((ForgeConfigSpec) config.getSpec()).correct(data,
                    (action, path, incorrectValue, correctedValue) ->
                            Configured.LOGGER.warn("Incorrect key {} was corrected from {} to its default, {}. {}", DOT_JOINER.join(path), incorrectValue, correctedValue, incorrectValue == correctedValue ? "This seems to be an error." : ""),
                    (action, path, incorrectValue, correctedValue) ->
                            Configured.LOGGER.debug("The comment on key {} does not match the spec. This may create a backup.", DOT_JOINER.join( path )));
            if(result != 0)
            {
                Configured.LOGGER.info("Config data sent from {} needed to be corrected", player.getName().getString());
            }
            config.getConfigData().putAll(data);
        }
        catch(ParsingException e)
        {
            Configured.LOGGER.warn("{} sent malformed config data to the server", player.getName().getString());
            player.connection.disconnect(new TranslatableComponent("configured.multiplayer.disconnect.invalid_config_packet"));
            sendMessageToOperators(new TranslatableComponent("configured.chat.malformed_config_data", player.getName(), new TextComponent(config.getFileName()).withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.RED));
            return;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return;
        }

        Configured.LOGGER.debug("Successfully processed config update for '" + message.fileName() + "'");

        PacketHandler.getPlayChannel().send(PacketDistributor.ALL.noArg(), new MessageSyncForgeConfig(message.fileName(), message.data()));
        sendMessageToOperators(new TranslatableComponent("configured.chat.config_updated", player.getName(), config.getFileName()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    public static void handleSyncSimpleConfigMessage(NetworkEvent.Context context, ServerPlayer player, MessageSyncSimpleConfig message)
    {
        if(!canEditServerConfigs(player))
            return;

        Configured.LOGGER.debug("Received server config sync from player: {}", player.getName().getString());

        SimpleConfigManager.SimpleConfigImpl config = SimpleConfigManager.getInstance().getConfig(message.id());
        if(config == null)
        {
            Configured.LOGGER.error("Client sent data for a config that doesn't exist: {}", message.id());
            context.getNetworkManager().disconnect(new TranslatableComponent("configured.multiplayer.disconnect.bad_config_packet"));
            return;
        }

        if(config.isReadOnly())
        {
            Configured.LOGGER.error("Client sent data for a read-only config '{}'", message.id());
            context.getNetworkManager().disconnect(new TranslatableComponent("configured.multiplayer.disconnect.bad_config_packet"));
            return;
        }

        if(!config.getType().isServer() || config.getType() == ConfigType.DEDICATED_SERVER)
        {
            Configured.LOGGER.error("Client sent data for a config is not supposed to be updated '{}'", message.id());
            context.getNetworkManager().disconnect(new TranslatableComponent("configured.multiplayer.disconnect.bad_config_packet"));
            return;
        }

        if(!config.isLoaded())
        {
            Configured.LOGGER.error("Client tried to perform sync update on an unloaded config. Something went wrong...");
            context.getNetworkManager().disconnect(new TranslatableComponent("configured.multiplayer.disconnect.bad_config_packet"));
            return;
        }

        if(SimpleConfigManager.getInstance().processSyncData(message, true))
        {
            if(config.getType().isSync())
            {
                PacketHandler.getPlayChannel().send(PacketDistributor.ALL.noArg(), new MessageSyncSimpleConfig(message.id(), message.data()));
            }
            sendMessageToOperators(new TranslatableComponent("configured.chat.config_updated", player.getName(), config.getFileName()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        else
        {
            context.getNetworkManager().disconnect(new TranslatableComponent("configured.multiplayer.disconnect.bad_config_packet"));
        }
    }

    private static void sendMessageToOperators(Component message)
    {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        Preconditions.checkNotNull(server, "The server was null when broadcasting config changes. This should not be possible...");
        for(ServerPlayer serverPlayer : server.getPlayerList().getPlayers())
        {
            if(server.getPlayerList().isOp(serverPlayer.getGameProfile()))
            {
                serverPlayer.sendMessage(message, Util.NIL_UUID);
            }
        }
    }

    public static void handleRequestSimpleConfigMessage(ServerPlayer player, MessageRequestSimpleConfig message, NetworkEvent.Context context)
    {
        if(!canEditServerConfigs(player))
            return;

        Configured.LOGGER.debug("Received config request from player: {}", player.getName().getString());

        SimpleConfigManager.SimpleConfigImpl config = SimpleConfigManager.getInstance().getConfig(message.id());
        if(config == null)
        {
            Configured.LOGGER.warn("{} tried to request server config which does not exist!", player.getName().getString());
            player.connection.disconnect(new TranslatableComponent("configured.multiplayer.disconnect.no_permission"));
            return;
        }

        if(!config.getType().isServer() || config.getType() == ConfigType.DEDICATED_SERVER)
        {
            Configured.LOGGER.warn("{} tried to request an invalid config from the server", player.getName().getString());
            player.connection.disconnect(new TranslatableComponent("configured.multiplayer.disconnect.no_permission"));
            return;
        }

        try
        {
            ResourceLocation key = config.getName();
            byte[] data = config.getData();
            PacketHandler.getPlayChannel().reply(new MessageResponseSimpleConfig(key, data), context);
            Configured.LOGGER.debug("Sending request reply back to player");
        }
        catch(Exception e)
        {
            Configured.LOGGER.warn("An exception occurred to read server config: {}", config.getFilePath());
            PacketHandler.getPlayChannel().reply(new MessageResponseSimpleConfig(config.getName(), new byte[]{}), context);
        }
    }

    private static boolean canEditServerConfigs(ServerPlayer player)
    {
        MinecraftServer server = player.getServer();
        if(server == null || !server.isDedicatedServer() || !Config.DEVELOPER.enabled.get())
        {
            Configured.LOGGER.warn("{} tried to request or update a server config, however developer mode is not enabled", player.getName().getString());
            player.connection.disconnect(new TranslatableComponent("configured.multiplayer.disconnect.unauthorized_request"));
            sendMessageToOperators(new TranslatableComponent("configured.chat.authorized_player").withStyle(ChatFormatting.RED));
            return false;
        }

        if(!Config.DEVELOPER.developers.get().contains(player.getStringUUID()) || !server.getPlayerList().isOp(player.getGameProfile()))
        {
            Configured.LOGGER.warn("{} tried to request or update a server config, however they are not a developer", player.getName().getString());
            player.connection.disconnect(new TranslatableComponent("configured.multiplayer.disconnect.unauthorized_request"));
            sendMessageToOperators(new TranslatableComponent("configured.chat.authorized_player").withStyle(ChatFormatting.RED));
            return false;
        }

        return true;
    }
}
