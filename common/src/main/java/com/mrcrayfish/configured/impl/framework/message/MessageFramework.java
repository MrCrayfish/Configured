package com.mrcrayfish.configured.impl.framework.message;

import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.impl.framework.handler.FrameworkClientHandler;
import com.mrcrayfish.configured.impl.framework.handler.FrameworkServerHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Author: MrCrayfish
 */
public class MessageFramework
{
    public record Sync(ResourceLocation id, byte[] data)
    {
        public static final ResourceLocation ID = new ResourceLocation(Constants.MOD_ID, "sync_framework_config");

        public static void encode(Sync message, FriendlyByteBuf buffer)
        {
            buffer.writeResourceLocation(message.id);
            buffer.writeByteArray(message.data);
        }

        public static Sync decode(FriendlyByteBuf buffer)
        {
            return new Sync(buffer.readResourceLocation(), buffer.readByteArray());
        }

        public static void handle(Sync message, Consumer<Runnable> executor, @Nullable Player player, Consumer<Component> disconnect)
        {
            if(player instanceof ServerPlayer serverPlayer)
            {
                executor.accept(() -> FrameworkServerHandler.handleServerSync(serverPlayer, message, disconnect));
            }
        }
    }

    public record Request(ResourceLocation id)
    {
        public static final ResourceLocation ID = new ResourceLocation(Constants.MOD_ID, "request_framework_config");

        public static void encode(Request message, FriendlyByteBuf buf)
        {
            buf.writeResourceLocation(message.id());
        }

        public static Request decode(FriendlyByteBuf buf)
        {
            return new Request(buf.readResourceLocation());
        }

        public static void handle(Request message, Consumer<Runnable> executor, @Nullable Player player, Consumer<Component> disconnect)
        {
            if(player instanceof ServerPlayer serverPlayer)
            {
                executor.accept(() -> FrameworkServerHandler.handleRequestConfig(serverPlayer, message, disconnect));
            }
        }
    }

    public record Response(byte[] data)
    {
        public static final ResourceLocation ID = new ResourceLocation(Constants.MOD_ID, "response_framework_config");

        public static void encode(Response message, FriendlyByteBuf buf)
        {
            buf.writeByteArray(message.data());
        }

        public static Response decode(FriendlyByteBuf buf)
        {
            return new Response(buf.readByteArray());
        }

        public static void handle(Response message, Consumer<Runnable> executor, @Nullable Player player, Consumer<Component> disconnect)
        {
            executor.accept(() -> FrameworkClientHandler.handleResponse(message, disconnect));
        }
    }
}
