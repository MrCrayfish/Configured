package com.mrcrayfish.configured.network.message;

import com.mrcrayfish.configured.network.play.ClientPlayHandler;
import com.mrcrayfish.configured.network.play.ServerPlayHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fmllegacy.network.NetworkDirection;
import net.minecraftforge.fmllegacy.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 */
public class MessageSyncServerConfig implements IMessage<MessageSyncServerConfig>
{
    private String fileName;
    private byte[] data;

    public MessageSyncServerConfig() {}

    public MessageSyncServerConfig(String fileName, byte[] data)
    {
        this.fileName = fileName;
        this.data = data;
    }

    @Override
    public void encode(MessageSyncServerConfig message, FriendlyByteBuf buffer)
    {
        buffer.writeUtf(message.fileName);
        buffer.writeByteArray(message.data);
    }

    @Override
    public MessageSyncServerConfig decode(FriendlyByteBuf buffer)
    {
        return new MessageSyncServerConfig(buffer.readUtf(), buffer.readByteArray());
    }

    @Override
    public void handle(MessageSyncServerConfig message, Supplier<NetworkEvent.Context> supplier)
    {
        if(supplier.get().getDirection() == NetworkDirection.PLAY_TO_SERVER)
        {
            IMessage.enqueueTask(supplier, () -> ServerPlayHandler.handleSyncServerConfigMessage(supplier.get().getSender(), message));
        }
        else
        {
            IMessage.enqueueTask(supplier, () -> ClientPlayHandler.handleSyncServerConfigMessage(message));
        }
    }

    public String getFileName()
    {
        return this.fileName;
    }

    public byte[] getData()
    {
        return this.data;
    }
}
