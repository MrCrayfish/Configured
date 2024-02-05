package com.mrcrayfish.configured.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * Author: MrCrayfish
 */
public class ClientConfigHelper
{
    public static boolean isPlayingGame()
    {
        return Minecraft.getInstance().level != null;
    }

    public static Player getClientPlayer()
    {
        return Minecraft.getInstance().player;
    }

    public static boolean isRunningLocalServer()
    {
        return Minecraft.getInstance().hasSingleplayerServer();
    }
}
