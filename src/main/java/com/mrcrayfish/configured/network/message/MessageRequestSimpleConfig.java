package com.mrcrayfish.configured.network.message;

import com.mrcrayfish.configured.network.play.ClientPlayHandler;
import com.mrcrayfish.configured.network.play.ServerPlayHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 */
public class MessageRequestSimpleConfig implements IMessage<MessageRequestSimpleConfig>
{
    private ResourceLocation id;

    public MessageRequestSimpleConfig() {}

    public MessageRequestSimpleConfig(ResourceLocation id)
    {
        this.id = id;
    }

    @Override
    public void encode(MessageRequestSimpleConfig message, FriendlyByteBuf buffer)
    {
        buffer.writeResourceLocation(message.id);
    }

    @Override
    public MessageRequestSimpleConfig decode(FriendlyByteBuf buffer)
    {
        return new MessageRequestSimpleConfig(buffer.readResourceLocation());
    }

    @Override
    public void handle(MessageRequestSimpleConfig message, Supplier<NetworkEvent.Context> supplier)
    {
        IMessage.enqueueTask(supplier, () ->
        {
            ServerPlayer player = supplier.get().getSender();
            if(player != null)
            {
                ServerPlayHandler.handleRequestSimpleConfigMessage(player, message, supplier.get());
            }
        });
    }

    public ResourceLocation getId()
    {
        return this.id;
    }
}
