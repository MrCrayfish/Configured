package com.mrcrayfish.configured.network.message.play;

import com.mrcrayfish.configured.network.handler.ForgeClientPlayHandler;
import com.mrcrayfish.configured.network.handler.ForgeServerPlayHandler;
import com.mrcrayfish.configured.network.play.ClientPlayHandler;
import com.mrcrayfish.configured.network.play.ServerPlayHandler;
import com.mrcrayfish.framework.api.network.MessageContext;
import com.mrcrayfish.framework.api.network.MessageDirection;
import com.mrcrayfish.framework.api.network.message.PlayMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * Author: MrCrayfish
 */
public final class MessageSyncForgeConfig extends PlayMessage<MessageSyncForgeConfig>
{
    private String fileName;
    private byte[] data;

    public MessageSyncForgeConfig() {}

    public MessageSyncForgeConfig(String fileName, byte[] data)
    {
        this.fileName = fileName;
        this.data = data;
    }

    @Override
    public void encode(MessageSyncForgeConfig message, FriendlyByteBuf buffer)
    {
        buffer.writeUtf(message.fileName);
        buffer.writeByteArray(message.data);
    }

    @Override
    public MessageSyncForgeConfig decode(FriendlyByteBuf buffer)
    {
        return new MessageSyncForgeConfig(buffer.readUtf(), buffer.readByteArray());
    }

    @Override
    public void handle(MessageSyncForgeConfig message, MessageContext context)
    {
        if(context.getDirection() == MessageDirection.PLAY_SERVER_BOUND)
        {
            ServerPlayer player = context.getPlayer();
            if(player != null)
            {
                context.execute(() -> ForgeServerPlayHandler.handleSyncServerConfigMessage(player, message));
            }
        }
        else
        {
            context.execute(() -> ForgeClientPlayHandler.handleSyncServerConfigMessage(context, message));
        }
        context.setHandled(true);
    }

    public String fileName()
    {
        return this.fileName;
    }

    public byte[] data()
    {
        return this.data;
    }
}
