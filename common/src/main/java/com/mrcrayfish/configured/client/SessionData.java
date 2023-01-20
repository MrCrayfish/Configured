package com.mrcrayfish.configured.client;

import com.mrcrayfish.configured.util.ConfigHelper;
import dev.architectury.event.events.client.ClientPlayerEvent;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/**
 * Holds if the client is a developer. Server will still validate regardless of this value.
 * This is used simply to change what is seen on the mod config selection screen.
 *
 * Author: MrCrayfish
 */
public class SessionData
{
    private static boolean developer;
    private static boolean lan;

    public static void setDeveloper(boolean enabled)
    {
        SessionData.developer = enabled;
    }

    public static boolean isDeveloper(@Nullable Player player)
    {
        return developer || ConfigHelper.isServerOwnedByPlayer(player);
    }

    public static void setLan(boolean lan)
    {
        SessionData.lan = lan;
    }

    public static boolean isLan()
    {
        return lan;
    }

    public static void registerEvents()
    {
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register((player -> {
            developer = false;
        }));
    }
}
