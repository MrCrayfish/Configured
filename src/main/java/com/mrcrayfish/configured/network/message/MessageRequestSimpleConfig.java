package com.mrcrayfish.configured.network.message;

import com.mrcrayfish.configured.network.MessageHelper;
import com.mrcrayfish.configured.network.play.ServerPlayHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 */
public record MessageRequestSimpleConfig(ResourceLocation id)
{
    public static void encode(MessageRequestSimpleConfig message, FriendlyByteBuf buffer)
    {
        buffer.writeResourceLocation(message.id);
    }

    public static MessageRequestSimpleConfig decode(FriendlyByteBuf buffer)
    {
        return new MessageRequestSimpleConfig(buffer.readResourceLocation());
    }

    public static void handle(MessageRequestSimpleConfig message, Supplier<NetworkEvent.Context> supplier)
    {
        MessageHelper.enqueueTask(supplier, () ->
        {
            ServerPlayer player = supplier.get().getSender();
            if(player != null)
            {
                ServerPlayHandler.handleRequestSimpleConfigMessage(player, message, supplier.get());
            }
        });
    }
}
