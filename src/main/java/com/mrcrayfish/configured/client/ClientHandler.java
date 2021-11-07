package com.mrcrayfish.configured.client;

import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.client.screen.ConfigScreen;
import com.mrcrayfish.configured.client.util.OptiFineHelper;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.forgespi.language.IModInfo;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

            List<ConfigScreen.ConfigFileEntry> clientConfigFileEntries = getConfigFileEntries(modId, ModConfig.Type.CLIENT);
            List<ConfigScreen.ConfigFileEntry> commonConfigFileEntries = getConfigFileEntries(modId, ModConfig.Type.COMMON);
            if(clientConfigFileEntries != null || commonConfigFileEntries != null) // Only add if at least one config exists
            {
                Configured.LOGGER.info("Registering config factory for mod {} (client: {}, common: {})", modId, clientConfigFileEntries != null, commonConfigFileEntries != null);
                String displayName = container.getModInfo().getDisplayName();
                ResourceLocation backgroundTexture = getBackgroundTexture(container.getModInfo());
                container.registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY, () -> (mc, screen) -> new ConfigScreen(screen, displayName, clientConfigFileEntries, commonConfigFileEntries, backgroundTexture));
            }
        });
    }

    private static EnumMap<ModConfig.Type, Set<ModConfig>> getConfigSets()
    {
        return ObfuscationReflectionHelper.getPrivateValue(ConfigTracker.class, ConfigTracker.INSTANCE, "configSets");
    }

    @Nullable
    private static List<ConfigScreen.ConfigFileEntry> getConfigFileEntries(String modId, ModConfig.Type type)
    {
        Set<ModConfig> configSet = getConfigSets().getOrDefault(type, Collections.emptySet()).stream().filter(config -> config.getModId().equals(modId)).collect(Collectors.toSet());

        /* Optifine basically breaks Forge's client config, so it's simply not added */
        if(type == ModConfig.Type.CLIENT && OptiFineHelper.isLoaded() && modId.equals("forge"))
        {
            Configured.LOGGER.info("Ignoring Forge's client config since OptiFine was detected");
            return null;
        }

        return configSet.size() > 0 ? configSet.stream().map(config -> new ConfigScreen.ConfigFileEntry(config.getSpec(), config.getSpec().getValues())).collect(Collectors.toList()) : null;
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
