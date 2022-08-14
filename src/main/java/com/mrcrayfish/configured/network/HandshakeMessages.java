package com.mrcrayfish.configured.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.IntSupplier;

/**
 * Author: MrCrayfish
 */
public class HandshakeMessages
{
    static class LoginIndexedMessage implements IntSupplier
    {
        private int loginIndex;

        void setLoginIndex(final int loginIndex)
        {
            this.loginIndex = loginIndex;
        }

        int getLoginIndex()
        {
            return this.loginIndex;
        }

        @Override
        public int getAsInt()
        {
            return this.getLoginIndex();
        }
    }

    static class C2SAcknowledge extends LoginIndexedMessage
    {
        void encode(FriendlyByteBuf buf) {}

        static C2SAcknowledge decode(FriendlyByteBuf buf)
        {
            return new C2SAcknowledge();
        }
    }

    public static class S2CConfigData extends LoginIndexedMessage
    {
        private final ResourceLocation key;
        private final byte[] data;

        public S2CConfigData(ResourceLocation key, byte[] data)
        {
            this.key = key;
            this.data = data;
        }

        void encode(FriendlyByteBuf buffer)
        {
            buffer.writeResourceLocation(this.key);
            buffer.writeBytes(this.data);
        }

        static S2CConfigData decode(FriendlyByteBuf buffer)
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
