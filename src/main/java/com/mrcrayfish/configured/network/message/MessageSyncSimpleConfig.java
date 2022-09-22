package com.mrcrayfish.configured.network.message;

import com.mrcrayfish.configured.Reference;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

//TODO only send whats needed

/**
 * Author: MrCrayfish
 */
public class MessageSyncSimpleConfig
{
    public static final ResourceLocation ID = new ResourceLocation(Reference.MOD_ID, "sync_simple_config");

    public static FriendlyByteBuf create(ResourceLocation id, byte[] data)
    {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeResourceLocation(id);
        buf.writeByteArray(data);
        return buf;
    }
}
