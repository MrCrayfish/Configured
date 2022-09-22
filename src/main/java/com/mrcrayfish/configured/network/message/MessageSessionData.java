package com.mrcrayfish.configured.network.message;

import com.mrcrayfish.configured.network.MessageHelper;
import com.mrcrayfish.configured.network.play.ClientPlayHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 */
public class MessageSessionData
{
    private boolean developer;
    private boolean lan;

    public MessageSessionData() {}

    public MessageSessionData(boolean developer, boolean lan)
    {
        this.developer = developer;
        this.lan = lan;
    }

    public static void encode(MessageSessionData message, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(message.developer);
        buffer.writeBoolean(message.lan);
    }

    public static MessageSessionData decode(FriendlyByteBuf buffer)
    {
        return new MessageSessionData(buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(MessageSessionData message, Supplier<NetworkEvent.Context> supplier)
    {
        MessageHelper.enqueueTask(supplier, () -> ClientPlayHandler.handleJoinMessage(message));
    }

    public boolean isDeveloper()
    {
        return this.developer;
    }

    public boolean isLan()
    {
        return this.lan;
    }
}
