package com.mrcrayfish.configured;

import com.mrcrayfish.configured.client.screen.ConfigScreen;
import com.mrcrayfish.configured.client.util.OptiFineHelper;
import com.mrcrayfish.configured.mixin.ModContainerMixin;
import net.minecraftforge.common.ForgeConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumMap;

/**
 * Author: MrCrayfish
 */
@Mod(value = "configured")
public class Configured
{
    public static final Logger LOGGER = LogManager.getLogger("configured");

    public Configured()
    {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.clientSpec);
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::onConstructEvent);
    }

    private void onConstructEvent(FMLLoadCompleteEvent event)
    {
        LOGGER.info("Creating config GUI factories...");
        ModList.get().forEachModContainer((modId, container) ->
        {
            // Ignore mods that already implement their own custom factory
            if(container.getCustomExtension(ExtensionPoint.CONFIGGUIFACTORY).isPresent())
                return;

            EnumMap<ModConfig.Type, ModConfig> configs = ((ModContainerMixin) container).getConfigs();
            ModConfig clientConfig = configs.get(ModConfig.Type.CLIENT);

            /* Optifine basically breaks Forge's client config, so it's simply not added */
            if(OptiFineHelper.isLoaded() && modId.equals("forge"))
            {
                LOGGER.info("Ignoring Forge's client config since OptiFine was detected");
                clientConfig = null;
            }

            ModConfig commonConfig = configs.get(ModConfig.Type.COMMON);
            ForgeConfigSpec clientSpec = clientConfig != null ? clientConfig.getSpec() : null;
            ForgeConfigSpec commonSpec = commonConfig != null ? commonConfig.getSpec() : null;
            if(clientSpec != null || commonSpec != null) // Only add if at least one config exists
            {
                LOGGER.info("Registering config factory for mod {} (client: {}, common: {})", modId, clientSpec != null, commonSpec != null);
                String displayName = container.getModInfo().getDisplayName();
                container.registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY, () -> (mc, screen) -> new ConfigScreen(screen, displayName, clientSpec, commonSpec));
            }
        });
    }
}
