package com.mrcrayfish.configured.network.message;

import com.mrcrayfish.configured.Reference;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Author: MrCrayfish
 */
public class MessageRequestSimpleConfig
{
    public static final ResourceLocation ID = new ResourceLocation(Reference.MOD_ID, "request_simple_config");

    public static FriendlyByteBuf create(ResourceLocation id)
    {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeResourceLocation(id);
        return buf;
    }
}
