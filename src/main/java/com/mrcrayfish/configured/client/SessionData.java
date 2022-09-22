package com.mrcrayfish.configured.client;

import com.mrcrayfish.configured.Reference;
import com.mrcrayfish.configured.util.ConfigHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

/**
 * Holds if the client is a developer. Server will still validate regardless of this value.
 * This is used simply to change what is seen on the mod config selection screen.
 *
 * Author: MrCrayfish
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
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

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggedOutEvent event)
    {
        developer = false;
    }
}
