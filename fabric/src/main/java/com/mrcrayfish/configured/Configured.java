package com.mrcrayfish.configured;

import com.mrcrayfish.configured.impl.framework.message.MessageFramework;
import com.mrcrayfish.configured.network.message.MessageSessionData;
import com.mrcrayfish.configured.platform.Services;
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
        PayloadTypeRegistry.serverboundPlay().register(MessageFramework.Sync.TYPE, MessageFramework.Sync.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MessageFramework.Sync.TYPE, MessageFramework.Sync.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(MessageFramework.Sync.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            MinecraftServer server = context.server();
            if(server != null) {
                MessageFramework.Sync.handle(payload, server::execute, player, context.responseSender()::disconnect);
            }
        });
        PayloadTypeRegistry.serverboundPlay().register(MessageFramework.Request.TYPE, MessageFramework.Request.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MessageFramework.Request.TYPE, MessageFramework.Request.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(MessageFramework.Request.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            MinecraftServer server = context.server();
            if(server != null) {
                MessageFramework.Request.handle(payload, server::execute, player, context.responseSender()::disconnect);
            }
        });

        PayloadTypeRegistry.serverboundPlay().register(MessageSessionData.TYPE, MessageSessionData.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MessageSessionData.TYPE, MessageSessionData.STREAM_CODEC);

        if(Services.PLATFORM.isModLoaded("framework"))
        {
            PayloadTypeRegistry.serverboundPlay().register(MessageFramework.Response.TYPE, MessageFramework.Response.STREAM_CODEC);
            PayloadTypeRegistry.clientboundPlay().register(MessageFramework.Response.TYPE, MessageFramework.Response.STREAM_CODEC);
        }
    }
}
