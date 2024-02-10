package com.mrcrayfish.configured.network.payload;

import com.mrcrayfish.configured.network.ConfiguredPayload;
import com.mrcrayfish.configured.network.message.MessageSessionData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

/**
 * Author: MrCrayfish
 */
public final class SessionDataPayload implements ConfiguredPayload
{
    private final boolean developer;
    private final boolean lan;

    private SessionDataPayload(boolean developer, boolean lan)
    {
        this.developer = developer;
        this.lan = lan;
    }

    public SessionDataPayload(FriendlyByteBuf buf)
    {
        this(buf.readBoolean(), buf.readBoolean());
    }

    @Override
    public void write(FriendlyByteBuf buf)
    {
        MessageSessionData.encode(new MessageSessionData(this.developer, this.lan), buf);
    }

    @Override
    public ResourceLocation id()
    {
        return MessageSessionData.ID;
    }

    public static void handle(SessionDataPayload payload, PlayPayloadContext context)
    {
        MessageSessionData.handle(new MessageSessionData(payload.developer, payload.lan), context.workHandler()::submitAsync);
    }

    public static SessionDataPayload of(boolean developer, boolean lan)
    {
        return new SessionDataPayload(developer, lan);
    }
}
