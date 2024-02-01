package com.mrcrayfish.configured.network;

import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.network.message.play.MessageSessionData;
import com.mrcrayfish.configured.network.message.play.MessageSyncForgeConfig;
import net.minecraft.resources.ResourceLocation;
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

        play.messageBuilder(MessageSessionData.class, 1)
                .encoder(MessageSessionData::encode)
                .decoder(MessageSessionData::decode)
                .consumerNetworkThread(MessageSessionData::handle)
                .add();

        play.messageBuilder(MessageSyncForgeConfig.class, 2)
            .encoder(MessageSyncForgeConfig::encode)
            .decoder(MessageSyncForgeConfig::decode)
            .consumerNetworkThread(MessageSyncForgeConfig::handle)
            .add();
    }

    public static SimpleChannel getPlay()
    {
        return play;
    }
}
