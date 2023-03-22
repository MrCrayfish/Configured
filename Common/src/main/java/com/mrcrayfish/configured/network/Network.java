package com.mrcrayfish.configured.network;

import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.impl.simple.SimpleConfigManager;
import com.mrcrayfish.configured.network.message.handshake.S2CMessageConfigData;
import com.mrcrayfish.configured.network.message.play.C2SMessageRequestSimpleConfig;
import com.mrcrayfish.configured.network.message.play.MessageSyncSimpleConfig;
import com.mrcrayfish.configured.network.message.play.S2CMessageResponseSimpleConfig;
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
    private static final FrameworkNetwork HANDSHAKE = FrameworkAPI
            .createNetworkBuilder(new ResourceLocation(Constants.MOD_ID, "handshake"), 1)
            .registerHandshakeMessage(S2CMessageConfigData.class, SimpleConfigManager.getInstance()::getMessagesForLogin)
            .ignoreClient()
            .ignoreServer()
            .build();

    private static final FrameworkNetwork PLAY = FrameworkAPI
            .createNetworkBuilder(new ResourceLocation(Constants.MOD_ID, "play"), 1)
            .registerPlayMessage(S2CMessageResponseSimpleConfig.class, MessageDirection.PLAY_CLIENT_BOUND)
            .registerPlayMessage(S2CMessageSessionData.class, MessageDirection.PLAY_CLIENT_BOUND)
            .registerPlayMessage(C2SMessageRequestSimpleConfig.class, MessageDirection.PLAY_SERVER_BOUND)
            .registerPlayMessage(MessageSyncSimpleConfig.class)
            .ignoreClient()
            .ignoreServer()
            .build();

    public static void init() {}

    public static FrameworkNetwork getHandshake()
    {
        return HANDSHAKE;
    }

    public static FrameworkNetwork getPlay()
    {
        return PLAY;
    }
}
