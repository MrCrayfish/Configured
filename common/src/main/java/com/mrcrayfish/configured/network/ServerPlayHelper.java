package com.mrcrayfish.configured.network;

import com.google.common.base.Preconditions;
import com.mrcrayfish.configured.Config;
import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.api.ExecutionContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Author: MrCrayfish
 */
public class ServerPlayHelper
{
    public static void sendMessageToOperators(Component message, ServerPlayer player)
    {
        MinecraftServer server = player.level().getServer();
        Preconditions.checkNotNull(server, "The server was null when broadcasting config changes. This should not be possible...");
        for(ServerPlayer serverPlayer : server.getPlayerList().getPlayers())
        {
            if(server.getPlayerList().isOp(serverPlayer.nameAndId()))
            {
                serverPlayer.sendSystemMessage(message);
            }
        }
    }

    public static boolean canEditServerConfigs(ServerPlayer player)
    {
        ExecutionContext context = new ExecutionContext(player);
        if(context.isClient())
        {
            if(!context.isIntegratedServerOwnedByPlayer())
            {
                Constants.LOG.warn("{} tried to request or update a server config, however is not the owner the integrated server", player.getName().getString());
                player.connection.disconnect(Component.translatable("configured.multiplayer.disconnect.unauthorized_request"));
                return false;
            }
            Constants.LOG.debug("{} was given access to edit server configs as they are the owner of the integrated server", player.getName().getString());
            return true;
        }
        else if(context.isDedicatedServer())
        {
            if(!Config.isDeveloperEnabled())
            {
                Constants.LOG.warn("{} tried to request or update a server config, however developer mode is not enabled", player.getName().getString());
                player.connection.disconnect(Component.translatable("configured.multiplayer.disconnect.unauthorized_request"));
                sendMessageToOperators(Component.translatable("configured.chat.authorized_player").withStyle(ChatFormatting.RED), player);
                return false;
            }
            if(!context.isPlayerAnOperator() || !context.isDeveloperPlayer())
            {
                Constants.LOG.warn("{} tried to request or update a server config, however they are not a developer", player.getName().getString());
                player.connection.disconnect(Component.translatable("configured.multiplayer.disconnect.unauthorized_request"));
                sendMessageToOperators(Component.translatable("configured.chat.authorized_player").withStyle(ChatFormatting.RED), player);
                return false;
            }
            return true;
        }
        return false;
    }
}
