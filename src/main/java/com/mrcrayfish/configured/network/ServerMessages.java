package com.mrcrayfish.configured.network;

import com.mrcrayfish.configured.network.message.MessageRequestSimpleConfig;
import com.mrcrayfish.configured.network.message.MessageSyncSimpleConfig;
import com.mrcrayfish.configured.network.play.ServerPlayHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;

/**
 * Author: MrCrayfish
 */
public class ServerMessages
{
    public static void register()
    {
        ServerPlayNetworking.registerGlobalReceiver(MessageRequestSimpleConfig.ID, (server, player, handler, buf, sender) -> {
            ResourceLocation id = buf.readResourceLocation();
            server.execute(() -> ServerPlayHandler.handleRequestSimpleConfigMessage(player, id));
        });

        ServerPlayNetworking.registerGlobalReceiver(MessageSyncSimpleConfig.ID, (server, player, handler, buf, sender) -> {
            ResourceLocation id = buf.readResourceLocation();
            byte[] data = buf.readByteArray();
            server.execute(() -> ServerPlayHandler.handleSyncSimpleConfigMessage(handler.connection, player, id, data));
        });
    }
}
