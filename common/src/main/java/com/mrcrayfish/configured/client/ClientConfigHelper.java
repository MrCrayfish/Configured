package com.mrcrayfish.configured.client;

import com.mrcrayfish.configured.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Author: MrCrayfish
 */
public class ClientConfigHelper
{
    public static boolean isMainMenu()
    {
        return !isPlayingGame();
    }

    public static boolean isPlayingGame()
    {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null || minecraft.level != null;
    }

    public static Player getClientPlayer()
    {
        return Minecraft.getInstance().player;
    }

    public static boolean isIntegratedServer()
    {
        return Minecraft.getInstance().getSingleplayerServer() != null;
    }

    public static boolean isLan()
    {
        return Minecraft.getInstance().getSingleplayerServer() != null && Minecraft.getInstance().getSingleplayerServer().isPublished();
    }

    public static boolean isSingleplayer()
    {
        return Minecraft.getInstance().getSingleplayerServer() != null && !Minecraft.getInstance().getSingleplayerServer().isPublished();
    }

    public static boolean isServerOwnedByPlayer(@Nullable Player player)
    {
        return player != null && Minecraft.getInstance().getSingleplayerServer() != null && Minecraft.getInstance().getSingleplayerServer().isSingleplayerOwner(player.nameAndId());
    }

    public static boolean isPlayingRemotely()
    {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        return listener != null && !listener.getConnection().isMemoryConnection();
    }

    public static boolean isConfiguredInstalledRemotely()
    {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        return listener != null && Services.PLATFORM.isConnectionActive(listener);
    }
}
