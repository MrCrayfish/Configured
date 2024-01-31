package com.mrcrayfish.configured.network.message.play;

import com.mrcrayfish.configured.network.play.ClientPlayHandler;
import com.mrcrayfish.framework.api.network.MessageContext;
import com.mrcrayfish.framework.api.network.message.PlayMessage;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Author: MrCrayfish
 */
public class S2CMessageSessionData extends PlayMessage<S2CMessageSessionData>
{
    private boolean developer;
    private boolean lan;

    public S2CMessageSessionData() {}

    public S2CMessageSessionData(boolean developer, boolean lan)
    {
        this.developer = developer;
        this.lan = lan;
    }

    @Override
    public void encode(S2CMessageSessionData message, FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(message.developer);
        buffer.writeBoolean(message.lan);
    }

    @Override
    public S2CMessageSessionData decode(FriendlyByteBuf buffer)
    {
        return new S2CMessageSessionData(buffer.readBoolean(), buffer.readBoolean());
    }

    @Override
    public void handle(S2CMessageSessionData message, MessageContext context)
    {
        context.execute(() -> ClientPlayHandler.handleJoinMessage(message));
        context.setHandled(true);
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
