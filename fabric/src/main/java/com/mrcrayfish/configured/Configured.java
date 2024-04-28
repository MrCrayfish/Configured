package com.mrcrayfish.configured;

import com.mrcrayfish.configured.impl.framework.message.MessageFramework;
import com.mrcrayfish.configured.network.message.MessageSessionData;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Author: MrCrayfish
 */
public class Configured implements ModInitializer
{
    @Override
    public void onInitialize()
    {
        Bootstrap.init();

        // Yeah, I don't care that this is ugly
        PayloadTypeRegistry.playC2S().register(MessageFramework.Sync.TYPE, MessageFramework.Sync.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(MessageFramework.Sync.TYPE, MessageFramework.Sync.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(MessageFramework.Sync.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            MinecraftServer server = player.server;
            MessageFramework.Sync.handle(payload, server::execute, player, context.responseSender()::disconnect);
        });
        PayloadTypeRegistry.playC2S().register(MessageFramework.Request.TYPE, MessageFramework.Request.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(MessageFramework.Request.TYPE, MessageFramework.Request.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(MessageFramework.Request.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            MinecraftServer server = player.server;
            MessageFramework.Request.handle(payload, server::execute, player, context.responseSender()::disconnect);
        });
    }
}
