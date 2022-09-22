package com.mrcrayfish.configured.network.message;

import com.mrcrayfish.configured.network.MessageHelper;
import com.mrcrayfish.configured.network.play.ClientPlayHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 */
public record MessageResponseSimpleConfig(ResourceLocation id, byte[] data)
{
    public static void encode(MessageResponseSimpleConfig message, FriendlyByteBuf buffer)
    {
        buffer.writeResourceLocation(message.id);
        buffer.writeByteArray(message.data);
    }

    public static MessageResponseSimpleConfig decode(FriendlyByteBuf buffer)
    {
        return new MessageResponseSimpleConfig(buffer.readResourceLocation(), buffer.readByteArray());
    }

    public static void handle(MessageResponseSimpleConfig message, Supplier<NetworkEvent.Context> supplier)
    {
        MessageHelper.enqueueTask(supplier, () -> ClientPlayHandler.handleResponseSimpleConfigMessage(message));
    }
}
