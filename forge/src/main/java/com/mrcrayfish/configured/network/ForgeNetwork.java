package com.mrcrayfish.configured.network;

import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.impl.framework.message.MessageFramework;
import com.mrcrayfish.configured.network.message.MessageSessionData;
import com.mrcrayfish.configured.network.message.play.MessageSyncForgeConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Author: MrCrayfish
 */
public class ForgeNetwork
{
    private static SimpleChannel play;

    public static void init()
    {
        play = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(Constants.MOD_ID, "play"))
            .clientAcceptedVersions(a -> true)
            .serverAcceptedVersions(a -> true)
            .networkProtocolVersion(() -> "1")
            .simpleChannel();

        play.messageBuilder(MessageSessionData.class, 1, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(MessageSessionData::encode)
            .decoder(MessageSessionData::decode)
            .consumerNetworkThread((message, contextSupplier) -> {
                MessageSessionData.handle(message, contextSupplier.get()::enqueueWork);
                contextSupplier.get().setPacketHandled(true);
            }).add();

        play.messageBuilder(MessageSyncForgeConfig.class, 2)
            .encoder(MessageSyncForgeConfig::encode)
            .decoder(MessageSyncForgeConfig::decode)
            .consumerNetworkThread(MessageSyncForgeConfig::handle)
            .add();

        if(ModList.get().isLoaded("framework"))
        {
            play.messageBuilder(MessageFramework.Sync.class, 10, NetworkDirection.PLAY_TO_SERVER)
                .encoder(MessageFramework.Sync::encode)
                .decoder(MessageFramework.Sync::decode)
                .consumerNetworkThread((message, supplier) -> {
                    NetworkEvent.Context ctx = supplier.get();
                    MessageFramework.Sync.handle(message, ctx::enqueueWork, ctx.getSender(), ctx.getNetworkManager()::disconnect);
                    supplier.get().setPacketHandled(true);
                }).add();

            play.messageBuilder(MessageFramework.Request.class, 11, NetworkDirection.PLAY_TO_SERVER)
                .encoder(MessageFramework.Request::encode)
                .decoder(MessageFramework.Request::decode)
                .consumerNetworkThread((message, supplier) -> {
                    NetworkEvent.Context ctx = supplier.get();
                    MessageFramework.Request.handle(message, ctx::enqueueWork, ctx.getSender(), ctx.getNetworkManager()::disconnect);
                    supplier.get().setPacketHandled(true);
                }).add();

            play.messageBuilder(MessageFramework.Response.class, 12, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MessageFramework.Response::encode)
                .decoder(MessageFramework.Response::decode)
                .consumerNetworkThread((message, supplier) -> {
                    NetworkEvent.Context ctx = supplier.get();
                    MessageFramework.Response.handle(message, ctx::enqueueWork, ctx.getNetworkManager()::disconnect);
                    supplier.get().setPacketHandled(true);
                }).add();
        }
    }

    public static SimpleChannel getPlay()
    {
        return play;
    }
}
