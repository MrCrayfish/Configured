package com.mrcrayfish.configured.client;

import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.client.screen.ConfigScreen;
import com.mrcrayfish.configured.client.util.OptiFineHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.fmlclient.ConfigGuiHandler;

import javax.annotation.Nullable;
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
    public static void onFinishedLoading()
    {
        // Constructs a map to get all configs registered by a mod
        Map<String, Map<ModConfig.Type, Set<ModConfig>>> idToConfigs = new HashMap<>();
        getConfigSets().forEach((type, modConfigs) ->
        {
            modConfigs.forEach(modConfig ->
            {
                Map<ModConfig.Type, Set<ModConfig>> typeToConfigSet = idToConfigs.computeIfAbsent(modConfig.getModId(), s -> new HashMap<>());
                Set<ModConfig> configSet = typeToConfigSet.computeIfAbsent(type, t -> new HashSet<>());
                configSet.add(modConfig);
            });
        });

        Configured.LOGGER.info("Creating config GUI factories...");
        ModList.get().forEachModContainer((modId, container) ->
        {
            // Ignore mods that already implement their own custom factory
            if(container.getCustomExtension(ConfigGuiHandler.ConfigGuiFactory.class).isPresent())
                return;

            Map<ModConfig.Type, Set<ModConfig>> typeToConfigSet = idToConfigs.get(modId);
            if(typeToConfigSet == null)
                return;

            Set<ModConfig> clientConfigs = typeToConfigSet.get(ModConfig.Type.CLIENT);
            Set<ModConfig> commonConfigs = typeToConfigSet.get(ModConfig.Type.COMMON);

            /* Optifine basically breaks Forge's client config, so it's simply not added */
            if(OptiFineHelper.isLoaded() && modId.equals("forge"))
            {
                Configured.LOGGER.info("Ignoring Forge's client config since OptiFine was detected");
                clientConfigs = null;
            }

            List<ConfigScreen.ConfigFileEntry> clientConfigFileEntries = getConfigFileEntries(clientConfigs);
            List<ConfigScreen.ConfigFileEntry> commonConfigFileEntries = getConfigFileEntries(commonConfigs);
            if(clientConfigFileEntries != null || commonConfigFileEntries != null) // Only add if at least one config exists
            {
                Configured.LOGGER.info("Registering config factory for mod {} (client: {}, common: {})", modId, clientConfigFileEntries != null, commonConfigFileEntries != null);
                ResourceLocation background = Screen.BACKGROUND_LOCATION;
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
                container.registerExtensionPoint(ConfigGuiHandler.ConfigGuiFactory.class, () -> new ConfigGuiHandler.ConfigGuiFactory((mc, screen) -> new ConfigScreen(screen, displayName, clientConfigFileEntries, commonConfigFileEntries, finalBackground)));
            }
        });
    }

    /* Since ModConfig#getSpec now returns a generic interface, I need to cast check for ForgeConfigSpec */
    @Nullable
    private static List<ConfigScreen.ConfigFileEntry> getConfigFileEntries(@Nullable Set<ModConfig> configs)
    {
        if(configs != null && configs.size() > 0)
        {
            return configs.stream().filter(config -> config.getSpec() instanceof ForgeConfigSpec).map(config -> {
                ForgeConfigSpec spec = (ForgeConfigSpec) config.getSpec();
                return new ConfigScreen.ConfigFileEntry(spec, spec.getValues());
            }).collect(Collectors.toList());
        }
        return null;
    }

    private static EnumMap<ModConfig.Type, Set<ModConfig>> getConfigSets()
    {
        return ObfuscationReflectionHelper.getPrivateValue(ConfigTracker.class, ConfigTracker.INSTANCE, "configSets");
    }
}
