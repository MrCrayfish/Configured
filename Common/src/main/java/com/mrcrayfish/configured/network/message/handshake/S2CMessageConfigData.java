package com.mrcrayfish.configured.network.message.handshake;

import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.impl.simple.SimpleConfigManager;
import com.mrcrayfish.configured.network.Network;
import com.mrcrayfish.framework.api.network.MessageContext;
import com.mrcrayfish.framework.api.network.message.HandshakeMessage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.CountDownLatch;

/**
 * Author: MrCrayfish
 */
public class S2CMessageConfigData extends HandshakeMessage<S2CMessageConfigData>
{
    private ResourceLocation key;
    private byte[] data;

    public S2CMessageConfigData() {}

    public S2CMessageConfigData(ResourceLocation key, byte[] data)
    {
        this.key = key;
        this.data = data;
    }

    @Override
    public void encode(S2CMessageConfigData message, FriendlyByteBuf buffer)
    {
        buffer.writeResourceLocation(message.key);
        buffer.writeByteArray(message.data);
    }

    @Override
    public S2CMessageConfigData decode(FriendlyByteBuf buffer)
    {
        ResourceLocation key = buffer.readResourceLocation();
        byte[] data = buffer.readByteArray();
        return new S2CMessageConfigData(key, data);
    }

    @Override
    public void handle(S2CMessageConfigData message, MessageContext context)
    {
        Constants.LOG.debug("Received config data from server");
        CountDownLatch block = new CountDownLatch(1);
        context.execute(() -> {
            if(!SimpleConfigManager.getInstance().processConfigData(message)) {
                context.getNetworkManager().disconnect(Component.translatable("configured.gui.handshake_process_failed"));
            }
            block.countDown();
        });
        try
        {
            block.await();
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }
        context.setHandled(true);
        context.reply(new Acknowledge());
    }

    public ResourceLocation getKey()
    {
        return this.key;
    }

    public byte[] getData()
    {
        return this.data;
    }
}
