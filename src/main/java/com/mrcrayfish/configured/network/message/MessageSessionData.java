package com.mrcrayfish.configured.network.message;

import com.mrcrayfish.configured.Reference;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
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
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(developer);
        buf.writeBoolean(lan);
        return buf;
    }
}
