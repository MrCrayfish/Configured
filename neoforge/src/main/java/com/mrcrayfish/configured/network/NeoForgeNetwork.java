package com.mrcrayfish.configured.network;

import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.impl.framework.message.MessageFramework;
import com.mrcrayfish.configured.network.message.MessageSessionData;
import com.mrcrayfish.configured.network.payload.SessionDataPayload;
import com.mrcrayfish.configured.network.payload.SyncNeoForgeConfigPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Author: MrCrayfish
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class NeoForgeNetwork
{
    public static final ResourceLocation ID = new ResourceLocation(Constants.MOD_ID, "play");
    public static final int VERSION = 1;

    @SubscribeEvent
    private static void onRegisterPayloadHandler(RegisterPayloadHandlerEvent event)
    {
        final IPayloadRegistrar registrar = event.registrar(Constants.MOD_ID).optional().versioned(Integer.toString(VERSION));
        registrar.play(MessageSessionData.ID, SessionDataPayload::new, builder -> builder.client(SessionDataPayload::handle));
        registrar.play(SyncNeoForgeConfigPayload.ID, SyncNeoForgeConfigPayload::new, builder -> builder.client(SyncNeoForgeConfigPayload::handle));

        if(ModList.get().isLoaded("framework"))
        {
            registerPlayPayload(registrar, MessageFramework.Sync.ID, MessageFramework.Sync::encode, MessageFramework.Sync::decode, MessageFramework.Sync::handle, PacketFlow.SERVERBOUND);
            registerPlayPayload(registrar, MessageFramework.Request.ID, MessageFramework.Request::encode, MessageFramework.Request::decode, MessageFramework.Request::handle, PacketFlow.SERVERBOUND);
            registerPlayPayload(registrar, MessageFramework.Response.ID, MessageFramework.Response::encode, MessageFramework.Response::decode, MessageFramework.Response::handle, PacketFlow.CLIENTBOUND);
        }
    }

    private static <T> void registerPlayPayload(IPayloadRegistrar registrar, ResourceLocation id, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, PayloadHandler<T> handler, @Nullable PacketFlow flow)
    {
        PayloadEncoder<T> payloadEncoder = new PayloadEncoder<>(id, encoder);
        registrar.play(payloadEncoder.id, buf -> new PayloadWrapper<>(decoder.apply(buf), payloadEncoder), builder -> {
            if(flow == null || flow.isClientbound()) {
                builder.client((payload, context) -> handler.accept(payload.instance, context.workHandler()::execute, context.player().orElse(null), context.packetHandler()::disconnect));
            }
            if(flow == null || flow.isServerbound()) {
                builder.server((payload, context) -> handler.accept(payload.instance, context.workHandler()::execute, context.player().orElse(null), context.packetHandler()::disconnect));
            }
        });
    }

    private record PayloadEncoder<T>(ResourceLocation id, BiConsumer<T, FriendlyByteBuf> encoder) {}

    private record PayloadWrapper<T>(T instance, PayloadEncoder<T> handler) implements CustomPacketPayload
    {
        @Override
        public void write(FriendlyByteBuf buf)
        {
            this.handler.encoder.accept(this.instance, buf);
        }

        @Override
        public ResourceLocation id()
        {
            return this.handler.id;
        }
    }

    @FunctionalInterface
    private interface PayloadHandler<T>
    {
        void accept(T instance, Consumer<Runnable> executor, @Nullable Player player, Consumer<Component> disconnect);
    }
}
