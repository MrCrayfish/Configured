package com.mrcrayfish.configured.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.IntSupplier;

/**
 * Author: MrCrayfish
 */
public class HandshakeMessages
{
    public static class S2CConfigData
    {
        private final ResourceLocation key;
        private final byte[] data;

        public S2CConfigData(ResourceLocation key, byte[] data)
        {
            this.key = key;
            this.data = data;
        }

        public void encode(FriendlyByteBuf buffer)
        {
            buffer.writeResourceLocation(this.key);
            buffer.writeByteArray(this.data);
        }

        public static S2CConfigData decode(FriendlyByteBuf buffer)
        {
            ResourceLocation key = buffer.readResourceLocation();
            byte[] data = buffer.readByteArray();
            return new S2CConfigData(key, data);
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
}
