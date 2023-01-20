package com.mrcrayfish.configured.network;

import com.mrcrayfish.configured.network.message.MessageRequestSimpleConfig;
import com.mrcrayfish.configured.network.message.MessageSyncSimpleConfig;
import com.mrcrayfish.configured.network.play.ServerPlayHandler;
import dev.architectury.networking.NetworkManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Author: MrCrayfish
 */
public class ServerMessages
{
    public static void register()
    {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, MessageRequestSimpleConfig.ID, (buf, context) -> {
            ResourceLocation id = buf.readResourceLocation();
            MinecraftServer server = context.getPlayer().getServer();
            server.execute(() -> ServerPlayHandler.handleRequestSimpleConfigMessage((ServerPlayer) context.getPlayer(), id));
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, MessageSyncSimpleConfig.ID, (buf, context) -> {
            ResourceLocation id = buf.readResourceLocation();
            byte[] data = buf.readByteArray();
            MinecraftServer server = context.getPlayer().getServer();
            server.execute(() -> ServerPlayHandler.handleSyncSimpleConfigMessage(((ServerPlayer)context.getPlayer()).connection.connection, (ServerPlayer) context.getPlayer(), id, data));
        });
    }
}
