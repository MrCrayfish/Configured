package com.mrcrayfish.configured.network.payload;

import com.mrcrayfish.configured.impl.framework.message.MessageFramework;
import com.mrcrayfish.configured.network.ConfiguredPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

/**
 * Author: MrCrayfish
 */
public final class FrameworkPayload
{
    public static class Sync implements ConfiguredPayload
    {
        private final ResourceLocation id;
        private final byte[] data;

        private Sync(ResourceLocation id, byte[] data)
        {
            this.id = id;
            this.data = data;
        }

        public Sync(FriendlyByteBuf buf)
        {
            this(buf.readResourceLocation(), buf.readByteArray());
        }

        @Override
        public void write(FriendlyByteBuf buf)
        {
            MessageFramework.Sync.encode(new MessageFramework.Sync(this.id, this.data), buf);
        }

        @Override
        public ResourceLocation id()
        {
            return MessageFramework.Sync.ID;
        }

        public static void handle(FrameworkPayload.Sync payload, PlayPayloadContext context)
        {
            MessageFramework.Sync.handle(new MessageFramework.Sync(payload.id, payload.data), context.workHandler()::submitAsync, context.player().orElse(null), context.packetHandler()::disconnect);
        }

        public static FrameworkPayload.Sync of(ResourceLocation id, byte[] data)
        {
            return new FrameworkPayload.Sync(id, data);
        }
    }

    public static class Request implements ConfiguredPayload
    {
        private final ResourceLocation id;

        private Request(ResourceLocation id)
        {
            this.id = id;
        }

        public Request(FriendlyByteBuf buf)
        {
            this(buf.readResourceLocation());
        }

        @Override
        public void write(FriendlyByteBuf buf)
        {
            MessageFramework.Request.encode(new MessageFramework.Request(this.id), buf);
        }

        @Override
        public ResourceLocation id()
        {
            return MessageFramework.Request.ID;
        }

        public static void handle(FrameworkPayload.Request payload, PlayPayloadContext context)
        {
            MessageFramework.Request.handle(new MessageFramework.Request(payload.id), context.workHandler()::submitAsync, context.player().orElse(null), context.packetHandler()::disconnect);
        }

        public static FrameworkPayload.Request of(ResourceLocation id)
        {
            return new FrameworkPayload.Request(id);
        }
    }

    public static class Response implements ConfiguredPayload
    {
        private final byte[] data;

        private Response(byte[] data)
        {
            this.data = data;
        }

        public Response(FriendlyByteBuf buf)
        {
            this(buf.readByteArray());
        }

        @Override
        public void write(FriendlyByteBuf buf)
        {
            MessageFramework.Response.encode(new MessageFramework.Response(this.data), buf);
        }

        @Override
        public ResourceLocation id()
        {
            return MessageFramework.Response.ID;
        }

        public static void handle(FrameworkPayload.Response payload, PlayPayloadContext context)
        {
            MessageFramework.Response.handle(new MessageFramework.Response(payload.data), context.workHandler()::submitAsync, context.player().orElse(null), context.packetHandler()::disconnect);
        }

        public static FrameworkPayload.Response of(byte[] data)
        {
            return new FrameworkPayload.Response(data);
        }
    }
}
