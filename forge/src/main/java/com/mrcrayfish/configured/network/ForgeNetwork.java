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
    private static SimpleChannel channel;
    private static @Nullable WeakReference<RegistryAccess> access = null;

    public static void init()
    {
        channel = ChannelBuilder.named(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "play"))
            .clientAcceptedVersions((status, version) -> true)
            .serverAcceptedVersions((status, version) -> true)
            .networkProtocolVersion(1)
            .simpleChannel();

        channel.play(protocol -> {
            protocol.clientbound(flow -> {
                flow.add(MessageSessionData.class, MessageSessionData.STREAM_CODEC, (message, ctx) -> {
                    MessageSessionData.handle(message, ctx::enqueueWork);
                    ctx.setPacketHandled(true);
                });
            });
            protocol.bidirectional(flow -> {
                flow.add(MessageSyncForgeConfig.class, MessageSyncForgeConfig.STREAM_CODEC, MessageSyncForgeConfig::handle);
            });
        });

        if(ModList.get().isLoaded("framework"))
        {
            channel.play(protocol -> {
                protocol.serverbound(flow -> {
                    flow.add(MessageFramework.Sync.class, MessageFramework.Sync.STREAM_CODEC, (message, ctx) -> {
                        MessageFramework.Sync.handle(message, ctx::enqueueWork, ctx.getSender(), ctx.getConnection()::disconnect);
                        ctx.setPacketHandled(true);
                    });
                    flow.add(MessageFramework.Request.class, MessageFramework.Request.STREAM_CODEC, (message, ctx) -> {
                        MessageFramework.Request.handle(message, ctx::enqueueWork, ctx.getSender(), ctx.getConnection()::disconnect);
                        ctx.setPacketHandled(true);
                    });
                });
                protocol.clientbound(flow -> {
                   flow.add(MessageFramework.Response.class, MessageFramework.Response.STREAM_CODEC, (message, ctx) -> {
                       MessageFramework.Response.handle(message, ctx::enqueueWork, ctx.getSender(), ctx.getConnection()::disconnect);
                       ctx.setPacketHandled(true);
                   });
                });
            });
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

    public static SimpleChannel getChannel()
    {
        return channel;
    }
}
