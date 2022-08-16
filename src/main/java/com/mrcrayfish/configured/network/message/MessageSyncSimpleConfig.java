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
public class MessageSyncSimpleConfig implements IMessage<MessageSyncSimpleConfig>
{
    private ResourceLocation id;
    private byte[] data;

    public MessageSyncSimpleConfig() {}

    public MessageSyncSimpleConfig(ResourceLocation id, byte[] data)
    {
        this.id = id;
        this.data = data;
    }

    @Override
    public void encode(MessageSyncSimpleConfig message, FriendlyByteBuf buffer)
    {
        buffer.writeResourceLocation(message.id);
        buffer.writeBytes(message.data);
    }

    @Override
    public MessageSyncSimpleConfig decode(FriendlyByteBuf buffer)
    {
        return new MessageSyncSimpleConfig(buffer.readResourceLocation(), buffer.readByteArray());
    }

    @Override
    public void handle(MessageSyncSimpleConfig message, Supplier<NetworkEvent.Context> supplier)
    {
        if(supplier.get().getDirection() == NetworkDirection.PLAY_TO_SERVER)
        {
            ServerPlayer player = supplier.get().getSender();
            if(player != null)
            {
                IMessage.enqueueTask(supplier, () -> ServerPlayHandler.handleSyncSimpleConfigMessage(player, message));
            }
        }
        else
        {
            IMessage.enqueueTask(supplier, () -> ClientPlayHandler.handleSyncSimpleConfigMessage(message));
        }
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
