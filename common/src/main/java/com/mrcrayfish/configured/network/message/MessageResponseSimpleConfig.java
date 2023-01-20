package com.mrcrayfish.configured.network.message;

import com.mrcrayfish.configured.Reference;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Author: MrCrayfish
 */
public class MessageResponseSimpleConfig
{
    public static final ResourceLocation ID = new ResourceLocation(Reference.MOD_ID, "response_simple_config");

    public static FriendlyByteBuf create(ResourceLocation id, byte[] data)
    {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeResourceLocation(id);
        buf.writeByteArray(data);
        return buf;
    }
}
