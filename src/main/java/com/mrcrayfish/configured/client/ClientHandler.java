package com.mrcrayfish.configured.client;

import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.client.screen.ConfigScreen;
import com.mrcrayfish.configured.client.util.OptiFineHelper;
import com.mrcrayfish.configured.mixin.ModContainerMixin;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ModConfig;

import java.util.EnumMap;

/**
 * Author: MrCrayfish
 */
@OnlyIn(Dist.CLIENT)
public class ClientHandler
{
    public static void onFinishedLoading()
    {
        Configured.LOGGER.info("Creating config GUI factories...");
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
                Configured.LOGGER.info("Ignoring Forge's client config since OptiFine was detected");
                clientConfig = null;
            }

            ModConfig commonConfig = configs.get(ModConfig.Type.COMMON);
            ForgeConfigSpec clientSpec = clientConfig != null ? clientConfig.getSpec() : null;
            ForgeConfigSpec commonSpec = commonConfig != null ? commonConfig.getSpec() : null;
            if(clientSpec != null || commonSpec != null) // Only add if at least one config exists
            {
                Configured.LOGGER.info("Registering config factory for mod {} (client: {}, common: {})", modId, clientSpec != null, commonSpec != null);
                String displayName = container.getModInfo().getDisplayName();
                container.registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY, () -> (mc, screen) -> new ConfigScreen(screen, displayName, clientSpec, commonSpec));
            }
        });
    }
}
