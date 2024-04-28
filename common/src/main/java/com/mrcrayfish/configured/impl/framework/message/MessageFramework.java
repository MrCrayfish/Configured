package com.mrcrayfish.configured.impl.framework.message;

import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.impl.framework.handler.FrameworkClientHandler;
import com.mrcrayfish.configured.impl.framework.handler.FrameworkServerHandler;
import com.mrcrayfish.configured.network.ConfiguredCodecs;
import com.mrcrayfish.configured.network.message.MessageSessionData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
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
    public record Sync(ResourceLocation id, byte[] data) implements CustomPacketPayload
    {
        public static final CustomPacketPayload.Type<Sync> TYPE = new Type<>(new ResourceLocation(Constants.MOD_ID, "sync_framework_config"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            Sync::id,
            ConfiguredCodecs.BYTE_ARRAY,
            Sync::data,
            Sync::new
        );

        public static void handle(Sync message, Consumer<Runnable> executor, @Nullable Player player, Consumer<Component> disconnect)
        {
            if(player instanceof ServerPlayer serverPlayer)
            {
                executor.accept(() -> FrameworkServerHandler.handleServerSync(serverPlayer, message, disconnect));
            }
        }

        @Override
        public Type<Sync> type()
        {
            return TYPE;
        }
    }

    public record Request(ResourceLocation id) implements CustomPacketPayload
    {
        public static final CustomPacketPayload.Type<Request> TYPE = new CustomPacketPayload.Type<>(new ResourceLocation(Constants.MOD_ID, "request_framework_config"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Request> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            Request::id,
            Request::new
        );

        public static void handle(Request message, Consumer<Runnable> executor, @Nullable Player player, Consumer<Component> disconnect)
        {
            if(player instanceof ServerPlayer serverPlayer)
            {
                executor.accept(() -> FrameworkServerHandler.handleRequestConfig(serverPlayer, message, disconnect));
            }
        }

        @Override
        public Type<Request> type()
        {
            return TYPE;
        }
    }

    public record Response(byte[] data) implements CustomPacketPayload
    {
        public static final CustomPacketPayload.Type<Response> TYPE = new CustomPacketPayload.Type<>(new ResourceLocation(Constants.MOD_ID, "response_framework_config"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Response> STREAM_CODEC = StreamCodec.composite(
            ConfiguredCodecs.BYTE_ARRAY,
            Response::data,
            Response::new
        );

        public static void handle(Response message, Consumer<Runnable> executor, @Nullable Player player, Consumer<Component> disconnect)
        {
            executor.accept(() -> FrameworkClientHandler.handleResponse(message, disconnect));
        }

        @Override
        public Type<Response> type()
        {
            return TYPE;
        }
    }
}
