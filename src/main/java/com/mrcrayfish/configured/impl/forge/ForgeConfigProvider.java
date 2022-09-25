package com.mrcrayfish.configured.impl.forge;

import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.api.IConfigProvider;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.client.util.OptiFineHelper;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public class ForgeConfigProvider implements IConfigProvider
{
    @Override
    public Set<IModConfig> getConfigurationsForMod(ModContainer container)
    {
        // Add Forge configurations
        Set<IModConfig> configs = new HashSet<>();
        addForgeConfigSetToMap(container, ModConfig.Type.CLIENT, configs::add);
        addForgeConfigSetToMap(container, ModConfig.Type.COMMON, configs::add);
        addForgeConfigSetToMap(container, ModConfig.Type.SERVER, configs::add);
        return configs;
    }

    private static void addForgeConfigSetToMap(ModContainer container, ModConfig.Type type, Consumer<IModConfig> consumer)
    {
        /* Optifine basically breaks Forge's client config, so it's simply not added */
        if(type == ModConfig.Type.CLIENT && OptiFineHelper.isLoaded() && container.getModId().equals("forge"))
        {
            Configured.LOGGER.info("Ignoring Forge's client config since OptiFine was detected");
            return;
        }

        Set<ModConfig> configSet = getForgeConfigSets().get(type);
        Set<IModConfig> filteredConfigSets = configSet.stream()
                .filter(config -> config.getModId().equals(container.getModId()) && config.getSpec() instanceof ForgeConfigSpec)
                .map(ForgeConfig::new)
                .collect(Collectors.toSet());
        filteredConfigSets.forEach(consumer);
    }

    private static EnumMap<ModConfig.Type, Set<ModConfig>> getForgeConfigSets()
    {
        return ObfuscationReflectionHelper.getPrivateValue(ConfigTracker.class, ConfigTracker.INSTANCE, "configSets");
    }
}
