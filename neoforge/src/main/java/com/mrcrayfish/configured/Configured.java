package com.mrcrayfish.configured;

import com.mrcrayfish.configured.client.ClientConfigured;
import com.mrcrayfish.configured.client.ClientHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.IExtensionPoint;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Author: MrCrayfish
 */
@Mod(Constants.MOD_ID)
public class Configured
{
    public Configured(IEventBus bus)
    {
        bus.addListener(this::onCommonSetup);
        bus.addListener(this::onLoadComplete);
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> IExtensionPoint.DisplayTest.IGNORESERVERONLY, (a, b) -> true));
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
    }

    private void onCommonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(Bootstrap::init);
    }

    private void onLoadComplete(FMLLoadCompleteEvent event)
    {
        event.enqueueWork(() ->
        {
            if(FMLLoader.getDist() == Dist.CLIENT)
            {
                ClientHandler.init();
                ClientConfigured.generateConfigFactories();
            }
        });
    }

    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if(event.getEntity() instanceof ServerPlayer player)
        {
            Events.onPlayerLoggedIn(player);
        }
    }
}
