package com.mrcrayfish.configured.client;

import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.client.screen.ModConfigSelectionScreen;
import com.mrcrayfish.configured.client.util.OptiFineHelper;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.forgespi.language.IModInfo;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
@OnlyIn(Dist.CLIENT)
public class ClientHandler
{
    // This is where the magic happens
    public static void generateConfigFactories()
    {
        Configured.LOGGER.info("Creating config GUI factories...");
        ModList.get().forEachModContainer((modId, container) ->
        {
            // Ignore mods that already implement their own custom factory
            if(container.getCustomExtension(ExtensionPoint.CONFIGGUIFACTORY).isPresent())
                return;

            Map<ModConfig.Type, Set<ModConfig>> modConfigMap = createConfigMap(container);
            if(!modConfigMap.isEmpty()) // Only add if at least one config exists
            {
                Configured.LOGGER.info("Registering config factory for mod {}. Found {} client config(s) and {} common config(s)", modId, modConfigMap.getOrDefault(ModConfig.Type.CLIENT, Collections.emptySet()), modConfigMap.getOrDefault(ModConfig.Type.COMMON, Collections.emptySet()));
                String displayName = container.getModInfo().getDisplayName();
                ResourceLocation backgroundTexture = getBackgroundTexture(container.getModInfo());
                container.registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY, () -> (mc, screen) -> new ModConfigSelectionScreen(screen, displayName, backgroundTexture, modConfigMap));
            }
        });
    }

    private static EnumMap<ModConfig.Type, Set<ModConfig>> getConfigSets()
    {
        return ObfuscationReflectionHelper.getPrivateValue(ConfigTracker.class, ConfigTracker.INSTANCE, "configSets");
    }

    private static Map<ModConfig.Type, Set<ModConfig>> createConfigMap(ModContainer container)
    {
        Map<ModConfig.Type, Set<ModConfig>> modConfigMap = new HashMap<>();
        addConfigSetToMap(container, ModConfig.Type.CLIENT, modConfigMap);
        addConfigSetToMap(container, ModConfig.Type.COMMON, modConfigMap);
        addConfigSetToMap(container, ModConfig.Type.SERVER, modConfigMap);
        return modConfigMap;
    }

    private static void addConfigSetToMap(ModContainer container, ModConfig.Type type, Map<ModConfig.Type, Set<ModConfig>> configMap)
    {
        /* Optifine basically breaks Forge's client config, so it's simply not added */
        if(type == ModConfig.Type.CLIENT && OptiFineHelper.isLoaded() && container.getModId().equals("forge"))
        {
            Configured.LOGGER.info("Ignoring Forge's client config since OptiFine was detected");
            return;
        }

        Set<ModConfig> configSet = getConfigSets().getOrDefault(type, Collections.emptySet()).stream().filter(config -> config.getModId().equals(container.getModId())).collect(Collectors.toSet());
        if(!configSet.isEmpty())
        {
            configMap.put(type, configSet);
        }
    }

    private static ResourceLocation getBackgroundTexture(IModInfo info)
    {
        String configBackground = (String) info.getModProperties().get("configuredBackground");

        if(configBackground != null)
        {
            return new ResourceLocation(configBackground);
        }

        if(info instanceof ModInfo)
        {
            // Fallback to old method to getting config background (since mods might not have updated)
            Optional<String> optional = ((ModInfo) info).getConfigElement("configBackground");
            if(optional.isPresent())
            {
                return new ResourceLocation(optional.get());
            }
        }

        return AbstractGui.BACKGROUND_LOCATION;
    }
}
