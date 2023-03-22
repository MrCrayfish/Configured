package com.mrcrayfish.configured.network.message.play;

import com.mrcrayfish.configured.network.play.ClientPlayHandler;
import com.mrcrayfish.configured.network.play.ServerPlayHandler;
import com.mrcrayfish.framework.api.network.MessageContext;
import com.mrcrayfish.framework.api.network.MessageDirection;
import com.mrcrayfish.framework.api.network.message.PlayMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

//TODO only send whats needed

/**
 * Author: MrCrayfish
 */
public final class MessageSyncSimpleConfig extends PlayMessage<MessageSyncSimpleConfig>
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
        buffer.writeByteArray(message.data);
    }

    @Override
    public MessageSyncSimpleConfig decode(FriendlyByteBuf buffer)
    {
        return new MessageSyncSimpleConfig(buffer.readResourceLocation(), buffer.readByteArray());
    }

    @Override
    public void handle(MessageSyncSimpleConfig message, MessageContext context)
    {
        if(context.getDirection() == MessageDirection.PLAY_SERVER_BOUND)
        {
            ServerPlayer player = context.getPlayer();
            if(player != null)
            {
                context.execute(() -> ServerPlayHandler.handleSyncSimpleConfigMessage(context, player, message));
            }
        }
        else
        {
            context.execute(() -> ClientPlayHandler.handleSyncSimpleConfigMessage(context, message));
        }
        context.setHandled(true);
    }

    public ResourceLocation id()
    {
        return id;
    }

    public byte[] data()
    {
        return data;
    }

    @Override
    public boolean equals(Object obj)
    {
        if(obj == this) return true;
        if(obj == null || obj.getClass() != this.getClass()) return false;
        var that = (MessageSyncSimpleConfig) obj;
        return Objects.equals(this.id, that.id) && Objects.equals(this.data, that.data);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id, data);
    }

    @Override
    public String toString()
    {
        return "MessageSyncSimpleConfig[" + "id=" + id + ", " + "data=" + data + ']';
    }

}
