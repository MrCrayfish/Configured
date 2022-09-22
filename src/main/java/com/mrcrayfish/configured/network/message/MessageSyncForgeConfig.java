package com.mrcrayfish.configured.network.message;

import com.mrcrayfish.configured.network.MessageHelper;
import com.mrcrayfish.configured.network.play.ClientPlayHandler;
import com.mrcrayfish.configured.network.play.ServerPlayHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 */
public record MessageSyncForgeConfig(String fileName, byte[] data)
{
    public static void encode(MessageSyncForgeConfig message, FriendlyByteBuf buffer)
    {
        buffer.writeUtf(message.fileName);
        buffer.writeByteArray(message.data);
    }

    public static MessageSyncForgeConfig decode(FriendlyByteBuf buffer)
    {
        return new MessageSyncForgeConfig(buffer.readUtf(), buffer.readByteArray());
    }

    public static void handle(MessageSyncForgeConfig message, Supplier<NetworkEvent.Context> supplier)
    {
        if(supplier.get().getDirection() == NetworkDirection.PLAY_TO_SERVER)
        {
            ServerPlayer player = supplier.get().getSender();
            if(player != null)
            {
                MessageHelper.enqueueTask(supplier, () -> ServerPlayHandler.handleSyncServerConfigMessage(player, message));
            }
        }
        else
        {
            MessageHelper.enqueueTask(supplier, () -> ClientPlayHandler.handleSyncServerConfigMessage(supplier.get(), message));
        }
    }
}
