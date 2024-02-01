package com.mrcrayfish.configured.client;

import net.minecraft.client.Minecraft;

/**
 * Author: MrCrayfish
 */
public class ClientConfigHelper
{
    public static boolean isPlayingGame()
    {
        return Minecraft.getInstance().level != null;
    }
}
