package com.mrcrayfish.configured.client;

import com.mrcrayfish.configured.util.ConfigHelper;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.Nullable;

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
}
