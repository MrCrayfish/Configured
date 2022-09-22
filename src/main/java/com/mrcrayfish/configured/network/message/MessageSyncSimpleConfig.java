package com.mrcrayfish.configured.network.message;

import com.mrcrayfish.configured.network.MessageHelper;
import com.mrcrayfish.configured.network.play.ClientPlayHandler;
import com.mrcrayfish.configured.network.play.ServerPlayHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

//TODO only send whats needed

/**
 * Author: MrCrayfish
 */
public record MessageSyncSimpleConfig(ResourceLocation id, byte[] data)
{
    public static void encode(MessageSyncSimpleConfig message, FriendlyByteBuf buffer)
    {
        buffer.writeResourceLocation(message.id);
        buffer.writeByteArray(message.data);
    }

    public static MessageSyncSimpleConfig decode(FriendlyByteBuf buffer)
    {
        return new MessageSyncSimpleConfig(buffer.readResourceLocation(), buffer.readByteArray());
    }

    public static void handle(MessageSyncSimpleConfig message, Supplier<NetworkEvent.Context> supplier)
    {
        if(supplier.get().getDirection() == NetworkDirection.PLAY_TO_SERVER)
        {
            ServerPlayer player = supplier.get().getSender();
            if(player != null)
            {
                MessageHelper.enqueueTask(supplier, () -> ServerPlayHandler.handleSyncSimpleConfigMessage(supplier.get(), player, message));
            }
        }
        else
        {
            MessageHelper.enqueueTask(supplier, () -> ClientPlayHandler.handleSyncSimpleConfigMessage(supplier.get(), message));
        }
    }
}
