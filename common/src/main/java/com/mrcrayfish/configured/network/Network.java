package com.mrcrayfish.configured.network;

import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.network.message.play.S2CMessageSessionData;
import com.mrcrayfish.framework.api.FrameworkAPI;
import com.mrcrayfish.framework.api.network.FrameworkNetwork;
import com.mrcrayfish.framework.api.network.MessageDirection;
import net.minecraft.resources.ResourceLocation;

/**
 * Author: MrCrayfish
 */
public class Network
{
    private static FrameworkNetwork handshake;
    private static FrameworkNetwork play;

    public static void init()
    {
        handshake = FrameworkAPI
            .createNetworkBuilder(new ResourceLocation(Constants.MOD_ID, "handshake"), 1)
            .ignoreClient()
            .ignoreServer()
            .build();

        play = FrameworkAPI
            .createNetworkBuilder(new ResourceLocation(Constants.MOD_ID, "play"), 1)
            .registerPlayMessage(S2CMessageSessionData.class, MessageDirection.PLAY_CLIENT_BOUND)
            .ignoreClient()
            .ignoreServer()
            .build();
    }

    public static FrameworkNetwork getHandshake()
    {
        return handshake;
    }

    public static FrameworkNetwork getPlay()
    {
        return play;
    }
}
