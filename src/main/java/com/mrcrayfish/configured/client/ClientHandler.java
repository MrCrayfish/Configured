package com.mrcrayfish.configured.client;

import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.client.screen.ConfigScreen;
import com.mrcrayfish.configured.client.util.OptiFineHelper;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;

import java.util.EnumMap;
import java.util.Optional;

/**
 * Author: MrCrayfish
 */
@OnlyIn(Dist.CLIENT)
public class ClientHandler
{
    // This is where the magic happens
    public static void onFinishedLoading()
    {
        Configured.LOGGER.info("Creating config GUI factories...");
        ModList.get().forEachModContainer((modId, container) ->
        {
            // Ignore mods that already implement their own custom factory
            if(container.getCustomExtension(ExtensionPoint.CONFIGGUIFACTORY).isPresent())
                return;

            EnumMap<ModConfig.Type, ModConfig> configs = getConfigMap(container);
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
                ResourceLocation background = AbstractGui.BACKGROUND_LOCATION;
                if(container.getModInfo() instanceof ModInfo)
                {
                    String configBackground = (String) container.getModInfo().getModProperties().get("configuredBackground");
                    if(configBackground == null)
                    {
                        // Fallback to old method to getting config background (since mods might not have updated)
                        Optional<String> optional = ((ModInfo) container.getModInfo()).getConfigElement("configBackground");
                        if(optional.isPresent())
                        {
                            configBackground = optional.get();
                        }
                    }
                    if(configBackground != null)
                    {
                        background = new ResourceLocation(configBackground);
                    }
                }
                String displayName = container.getModInfo().getDisplayName();
                final ResourceLocation finalBackground = background;
                container.registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY, () -> (mc, screen) -> new ConfigScreen(screen, displayName, clientSpec, commonSpec, finalBackground));
            }
        });
    }

    private static EnumMap<ModConfig.Type, ModConfig> getConfigMap(ModContainer container)
    {
        return ObfuscationReflectionHelper.getPrivateValue(ModContainer.class, container, "configs");
    }
}
