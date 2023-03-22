package com.mrcrayfish.configured.network.message.play;

import com.mrcrayfish.configured.network.play.ClientPlayHandler;
import com.mrcrayfish.framework.api.network.MessageContext;
import com.mrcrayfish.framework.api.network.message.PlayMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Author: MrCrayfish
 */
public final class S2CMessageResponseSimpleConfig extends PlayMessage<S2CMessageResponseSimpleConfig>
{
    private ResourceLocation id;
    private byte[] data;

    public S2CMessageResponseSimpleConfig() {}

    public S2CMessageResponseSimpleConfig(ResourceLocation id, byte[] data)
    {
        this.id = id;
        this.data = data;
    }

    @Override
    public void encode(S2CMessageResponseSimpleConfig message, FriendlyByteBuf buffer)
    {
        buffer.writeResourceLocation(message.id);
        buffer.writeByteArray(message.data);
    }

    @Override
    public S2CMessageResponseSimpleConfig decode(FriendlyByteBuf buffer)
    {
        return new S2CMessageResponseSimpleConfig(buffer.readResourceLocation(), buffer.readByteArray());
    }

    @Override
    public void handle(S2CMessageResponseSimpleConfig message, MessageContext context)
    {
        context.execute(() -> ClientPlayHandler.handleResponseSimpleConfigMessage(message));
        context.setHandled(true);
    }

    public ResourceLocation id()
    {
        return this.id;
    }

    public byte[] data()
    {
        return this.data;
    }
}
