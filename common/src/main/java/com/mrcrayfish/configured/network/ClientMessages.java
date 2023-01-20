package com.mrcrayfish.configured.network;

import com.mrcrayfish.configured.network.message.MessageResponseSimpleConfig;
import com.mrcrayfish.configured.network.message.MessageSessionData;
import com.mrcrayfish.configured.network.message.MessageSyncSimpleConfig;
import com.mrcrayfish.configured.network.play.ClientPlayHandler;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * Author: MrCrayfish
 */
public class ClientMessages
{
    public static void register()
    {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, MessageSessionData.ID, (buf, packetContext) -> {
            boolean developer = buf.readBoolean();
            boolean lan = buf.readBoolean();
            Minecraft.getInstance().execute(() -> ClientPlayHandler.handleJoinMessage(developer, lan));
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, MessageResponseSimpleConfig.ID, (buf, packetContext) -> {
            ResourceLocation id = buf.readResourceLocation();
            byte[] data = buf.readByteArray();
            Minecraft.getInstance().execute(() -> ClientPlayHandler.handleResponseSimpleConfigMessage(id, data));
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, MessageSyncSimpleConfig.ID, (buf, packetContext) -> {
            ResourceLocation id = buf.readResourceLocation();
            byte[] data = buf.readByteArray();
            Minecraft.getInstance().execute(() -> ClientPlayHandler.handleSyncSimpleConfigMessage(Minecraft.getInstance().getConnection().getConnection(), id, data));
        });
    }
}
