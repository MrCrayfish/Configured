package com.mrcrayfish.configured.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Author: MrCrayfish
 */
public class ConfiguredCodecs
{
    public static final StreamCodec<FriendlyByteBuf, byte[]> BYTE_ARRAY = new StreamCodec<>()
    {
        @Override
        public byte[] decode(FriendlyByteBuf buf)
        {
            return buf.readByteArray();
        }

        @Override
        public void encode(FriendlyByteBuf buf, byte[] data)
        {
            buf.writeByteArray(data);
        }
    };
}
