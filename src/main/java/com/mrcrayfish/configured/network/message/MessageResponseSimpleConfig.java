package com.mrcrayfish.configured.network.message;

import com.mrcrayfish.configured.network.play.ClientPlayHandler;
import com.mrcrayfish.configured.network.play.ServerPlayHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 */
public class MessageResponseSimpleConfig implements IMessage<MessageResponseSimpleConfig>
{
    private ResourceLocation id;
    private byte[] data;

    public MessageResponseSimpleConfig() {}

    public MessageResponseSimpleConfig(ResourceLocation id, byte[] data)
    {
        this.id = id;
        this.data = data;
    }

    @Override
    public void encode(MessageResponseSimpleConfig message, FriendlyByteBuf buffer)
    {
        buffer.writeResourceLocation(message.id);
        buffer.writeByteArray(message.data);
    }

    @Override
    public MessageResponseSimpleConfig decode(FriendlyByteBuf buffer)
    {
        return new MessageResponseSimpleConfig(buffer.readResourceLocation(), buffer.readByteArray());
    }

    @Override
    public void handle(MessageResponseSimpleConfig message, Supplier<NetworkEvent.Context> supplier)
    {
        IMessage.enqueueTask(supplier, () -> ClientPlayHandler.handleResponseSimpleConfigMessage(message));
    }

    public ResourceLocation getId()
    {
        return this.id;
    }

    public byte[] getData()
    {
        return this.data;
    }
}
