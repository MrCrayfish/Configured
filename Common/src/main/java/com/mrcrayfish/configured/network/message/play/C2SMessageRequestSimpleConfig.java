package com.mrcrayfish.configured.network.message.play;

import com.mrcrayfish.configured.network.play.ServerPlayHandler;
import com.mrcrayfish.framework.api.network.MessageContext;
import com.mrcrayfish.framework.api.network.message.PlayMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Author: MrCrayfish
 */
public final class C2SMessageRequestSimpleConfig extends PlayMessage<C2SMessageRequestSimpleConfig>
{
    private ResourceLocation id;

    public C2SMessageRequestSimpleConfig() {}

    public C2SMessageRequestSimpleConfig(ResourceLocation id)
    {
        this.id = id;
    }

    @Override
    public void encode(C2SMessageRequestSimpleConfig message, FriendlyByteBuf buffer)
    {
        buffer.writeResourceLocation(message.id);
    }

    @Override
    public C2SMessageRequestSimpleConfig decode(FriendlyByteBuf buffer)
    {
        return new C2SMessageRequestSimpleConfig(buffer.readResourceLocation());
    }

    @Override
    public void handle(C2SMessageRequestSimpleConfig message, MessageContext context)
    {
        context.execute(() ->
        {
            ServerPlayer player = context.getPlayer();
            if(player != null)
            {
                ServerPlayHandler.handleRequestSimpleConfigMessage(player, message, context);
            }
        });
        context.setHandled(true);
    }

    public ResourceLocation id()
    {
        return this.id;
    }
}
