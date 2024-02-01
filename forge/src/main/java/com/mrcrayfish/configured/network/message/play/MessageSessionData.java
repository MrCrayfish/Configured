package com.mrcrayfish.configured.network.message.play;

import com.mrcrayfish.configured.network.handler.ForgeClientPlayHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 */
public record MessageSessionData(boolean developer, boolean lan)
{
    public static void encode(MessageSessionData message, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(message.developer);
        buffer.writeBoolean(message.lan);
    }

    public static MessageSessionData decode(FriendlyByteBuf buffer)
    {
        return new MessageSessionData(buffer.readBoolean(), buffer.readBoolean());
    }

    public static void handle(MessageSessionData message, Supplier<NetworkEvent.Context> context)
    {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> ForgeClientPlayHandler.handleSessionData(message));
        ctx.setPacketHandled(true);
    }
}
