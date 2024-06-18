package com.mrcrayfish.configured.network;

import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.impl.framework.message.MessageFramework;
import com.mrcrayfish.configured.network.message.MessageSessionData;
import com.mrcrayfish.configured.network.payload.SyncNeoForgeConfigPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Author: MrCrayfish
 */
@EventBusSubscriber(modid = Constants.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class NeoForgeNetwork
{
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "play");
    public static final int VERSION = 1;

    @SubscribeEvent
    private static void onRegisterPayloadHandler(RegisterPayloadHandlersEvent event)
    {
        final PayloadRegistrar registrar = event.registrar(Constants.MOD_ID).optional().versioned(Integer.toString(VERSION));
        registrar.playToClient(MessageSessionData.TYPE, MessageSessionData.STREAM_CODEC, (payload, context) -> {
            MessageSessionData.handle(payload, context::enqueueWork);
        });
        registrar.playToClient(SyncNeoForgeConfigPayload.TYPE, SyncNeoForgeConfigPayload.STREAM_CODEC, SyncNeoForgeConfigPayload::handle);

        if(ModList.get().isLoaded("framework"))
        {
            registrar.playToServer(MessageFramework.Sync.TYPE, MessageFramework.Sync.STREAM_CODEC, (payload, context) -> {
                MessageFramework.Sync.handle(payload, context::enqueueWork, context.player(), context::disconnect);
            });
            registrar.playToServer(MessageFramework.Request.TYPE, MessageFramework.Request.STREAM_CODEC, (payload, context) -> {
                MessageFramework.Request.handle(payload, context::enqueueWork, context.player(), context::disconnect);
            });
            registrar.playToClient(MessageFramework.Response.TYPE, MessageFramework.Response.STREAM_CODEC, (payload, context) -> {
                MessageFramework.Response.handle(payload, context::enqueueWork, context.player(), context::disconnect);
            });
        }
    }
}
