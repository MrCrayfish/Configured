package com.mrcrayfish.configured;

import com.mrcrayfish.configured.network.Network;
import com.mrcrayfish.configured.network.message.play.S2CMessageSessionData;
import com.mrcrayfish.framework.api.Environment;
import com.mrcrayfish.framework.api.event.PlayerEvents;
import com.mrcrayfish.framework.api.util.EnvironmentHelper;
import net.minecraft.server.level.ServerPlayer;

/**
 * Author: MrCrayfish
 */
public class Events
{
    public static void init()
    {
        PlayerEvents.LOGGED_IN.register(player ->
        {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            boolean developer = EnvironmentHelper.getEnvironment() == Environment.DEDICATED_SERVER && Config.DEVELOPER.enabled.get() && Config.DEVELOPER.developers.get().contains(serverPlayer.getStringUUID());
            boolean lanServer = serverPlayer.getServer() != null && !serverPlayer.getServer().isDedicatedServer();
            Network.getPlay().sendToPlayer(() -> serverPlayer, new S2CMessageSessionData(developer, lanServer));
        });
    }
}
