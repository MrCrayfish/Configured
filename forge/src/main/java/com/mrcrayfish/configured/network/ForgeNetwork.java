package com.mrcrayfish.configured.network;

import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.impl.framework.message.MessageFramework;
import com.mrcrayfish.configured.network.message.MessageSessionData;
import com.mrcrayfish.configured.network.message.play.MessageSyncForgeConfig;
import com.mrcrayfish.framework.api.Environment;
import com.mrcrayfish.framework.api.util.TaskRunner;
import com.mrcrayfish.framework.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.SimpleChannel;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Optional;

/**
 * Author: MrCrayfish
 */
public class ForgeNetwork
{
    private static SimpleChannel play;
    private static @Nullable WeakReference<RegistryAccess> access = null;

    public static void init()
    {
        play = ChannelBuilder.named(new ResourceLocation(Constants.MOD_ID, "play"))
            .clientAcceptedVersions((status, version) -> true)
            .serverAcceptedVersions((status, version) -> true)
            .networkProtocolVersion(1)
            .simpleChannel();

        play.messageBuilder(MessageSessionData.class, 1, NetworkDirection.PLAY_TO_CLIENT)
            .encoder((msg, buf) -> MessageSessionData.STREAM_CODEC.encode(wrapBuf(buf), msg))
            .decoder(buf -> MessageSessionData.STREAM_CODEC.decode(wrapBuf(buf)))
            .consumerNetworkThread((message, ctx) -> {
                MessageSessionData.handle(message, ctx::enqueueWork);
                ctx.setPacketHandled(true);
            }).add();

        play.messageBuilder(MessageSyncForgeConfig.class, 2)
            .encoder(MessageSyncForgeConfig::encode)
            .decoder(MessageSyncForgeConfig::decode)
            .consumerNetworkThread(MessageSyncForgeConfig::handle)
            .add();

        if(ModList.get().isLoaded("framework"))
        {
            play.messageBuilder(MessageFramework.Sync.class, 10, NetworkDirection.PLAY_TO_SERVER)
                .encoder((msg, buf) -> MessageFramework.Sync.STREAM_CODEC.encode(wrapBuf(buf), msg))
                .decoder(buf -> MessageFramework.Sync.STREAM_CODEC.decode(wrapBuf(buf)))
                .consumerNetworkThread((message, ctx) -> {
                    MessageFramework.Sync.handle(message, ctx::enqueueWork, ctx.getSender(), ctx.getConnection()::disconnect);
                    ctx.setPacketHandled(true);
                }).add();

            play.messageBuilder(MessageFramework.Request.class, 11, NetworkDirection.PLAY_TO_SERVER)
                .encoder((msg, buf) -> MessageFramework.Request.STREAM_CODEC.encode(wrapBuf(buf), msg))
                .decoder(buf -> MessageFramework.Request.STREAM_CODEC.decode(wrapBuf(buf)))
                .consumerNetworkThread((message, ctx) -> {
                    MessageFramework.Request.handle(message, ctx::enqueueWork, ctx.getSender(), ctx.getConnection()::disconnect);
                    ctx.setPacketHandled(true);
                }).add();

            play.messageBuilder(MessageFramework.Response.class, 12, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((msg, buf) -> MessageFramework.Response.STREAM_CODEC.encode(wrapBuf(buf), msg))
                .decoder(buf -> MessageFramework.Response.STREAM_CODEC.decode(wrapBuf(buf)))
                .consumerNetworkThread((message, ctx) -> {
                    MessageFramework.Response.handle(message, ctx::enqueueWork, ctx.getSender(), ctx.getConnection()::disconnect);
                    ctx.setPacketHandled(true);
                }).add();
        }

        MinecraftForge.EVENT_BUS.addListener((ServerStartingEvent event) -> {
            ForgeNetwork.access = new WeakReference<>(event.getServer().registryAccess());
        });
        MinecraftForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
            ForgeNetwork.access = null;
        });
    }

    private static RegistryFriendlyByteBuf wrapBuf(FriendlyByteBuf buf)
    {
        return RegistryFriendlyByteBuf.decorator(getRegistryAccess()).apply(buf);
    }

    private static RegistryAccess getRegistryAccess()
    {
        return Utils.or(Optional.ofNullable(access).map(Reference::get).orElse(null), TaskRunner.callIf(Environment.CLIENT, () -> () -> {
            Minecraft mc = Minecraft.getInstance();
            return mc.level != null ? mc.level.registryAccess() : null;
        })).orElseThrow(() -> new RuntimeException("Failed to retrieve registry access"));
    }

    public static SimpleChannel getPlay()
    {
        return play;
    }
}
