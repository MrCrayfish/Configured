package com.mrcrayfish.configured;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.mrcrayfish.configured.client.ClientHandler;
import com.mrcrayfish.configured.client.screen.ConfigScreen;
import com.mrcrayfish.configured.mixin.ModContainerMixin;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.gui.screen.ModListScreen;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.EnumMap;

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

    private void onConstructEvent(FMLLoadCompleteEvent event)
    {
        ModList.get().forEachModContainer((modId, container) ->
        {
            // Ignore mods that already implement their own custom factory
            if(container.getCustomExtension(ExtensionPoint.CONFIGGUIFACTORY).isPresent())
                return;
            EnumMap<ModConfig.Type, ModConfig> configs = ((ModContainerMixin) container).getConfigs();
            ModConfig clientConfig = configs.get(ModConfig.Type.CLIENT);
            ModConfig commonConfig = configs.get(ModConfig.Type.COMMON);
            ForgeConfigSpec clientSpec = clientConfig != null ? clientConfig.getSpec() : null;
            ForgeConfigSpec commonSpec = commonConfig != null ? commonConfig.getSpec() : null;
            UnmodifiableConfig clientValues = clientSpec != null ? clientSpec.getValues() : null;
            UnmodifiableConfig commonValues = commonSpec != null ? commonSpec.getValues() : null;
            String displayName = container.getModInfo().getDisplayName();
            if(clientSpec != null || commonSpec != null) // Only add if at least one config exists
            {
                container.registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY, () -> (mc, screen) -> new ConfigScreen(screen, displayName, clientSpec, clientValues, commonSpec, commonValues));
            }
        });
    }
}
