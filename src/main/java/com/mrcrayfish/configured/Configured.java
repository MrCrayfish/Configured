package com.mrcrayfish.configured;

import com.mrcrayfish.configured.client.ClientHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.gui.screen.ModListScreen;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Author: MrCrayfish
 */
@Mod(value = "configured")
public class Configured
{
    public Configured()
    {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.clientSpec);
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::onClientSetup);
        bus.addListener(this::onConstructEvent);
    }

    private void onClientSetup(FMLCommonSetupEvent event)
    {
        ClientHandler.setup();
    }

    private void onConstructEvent(FMLConstructModEvent event)
    {
        ModList.get().forEachModContainer((modId, container) ->
        {
            container.registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY, () -> (minecraft, screen) -> new ModListScreen(screen));
        });
    }
}
