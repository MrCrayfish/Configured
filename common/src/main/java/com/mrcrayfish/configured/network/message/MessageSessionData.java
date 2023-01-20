package com.mrcrayfish.configured.network.message;

import com.mrcrayfish.configured.Reference;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Author: MrCrayfish
 */
public class MessageSessionData
{
    public static final ResourceLocation ID = new ResourceLocation(Reference.MOD_ID, "session_data");

    public static FriendlyByteBuf create(boolean developer, boolean lan)
    {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBoolean(developer);
        buf.writeBoolean(lan);
        return buf;
    }
}
