package com.mrcrayfish.configured.network;

import com.mrcrayfish.configured.network.message.MessageResponseSimpleConfig;
import com.mrcrayfish.configured.network.message.MessageSessionData;
import com.mrcrayfish.configured.network.message.MessageSyncSimpleConfig;
import com.mrcrayfish.configured.network.play.ClientPlayHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.ResourceLocation;

/**
 * Author: MrCrayfish
 */
public class ClientMessages
{
    public static void register()
    {
        ClientPlayNetworking.registerGlobalReceiver(MessageSessionData.ID, (client, handler, buf, sender) -> {
            boolean developer = buf.readBoolean();
            boolean lan = buf.readBoolean();
            client.execute(() -> ClientPlayHandler.handleJoinMessage(developer, lan));
        });

        ClientPlayNetworking.registerGlobalReceiver(MessageResponseSimpleConfig.ID, (client, handler, buf, sender) -> {
            ResourceLocation id = buf.readResourceLocation();
            byte[] data = buf.readByteArray();
            client.execute(() -> ClientPlayHandler.handleResponseSimpleConfigMessage(id, data));
        });

        ClientPlayNetworking.registerGlobalReceiver(MessageSyncSimpleConfig.ID, (client, handler, buf, sender) -> {
            ResourceLocation id = buf.readResourceLocation();
            byte[] data = buf.readByteArray();
            client.execute(() -> ClientPlayHandler.handleSyncSimpleConfigMessage(handler.getConnection(), id, data));
        });
    }
}
