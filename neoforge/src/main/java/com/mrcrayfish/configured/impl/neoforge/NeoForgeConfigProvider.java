package com.mrcrayfish.configured.impl.neoforge;

import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.api.IModConfigProvider;
import com.mrcrayfish.configured.api.ModContext;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public class NeoForgeConfigProvider implements IModConfigProvider
{
    @Override
    public Set<IModConfig> getConfigurationsForMod(ModContext context)
    {
        // Add Forge configurations
        Set<IModConfig> configs = new HashSet<>();
        addForgeConfigSetToMap(context, ModConfig.Type.CLIENT, configs::add);
        addForgeConfigSetToMap(context, ModConfig.Type.COMMON, configs::add);
        addForgeConfigSetToMap(context, ModConfig.Type.SERVER, configs::add);
        addForgeConfigSetToMap(context, ModConfig.Type.STARTUP, configs::add);
        return configs;
    }

    private static void addForgeConfigSetToMap(ModContext context, ModConfig.Type type, Consumer<IModConfig> consumer)
    {
        Set<ModConfig> configSet = ConfigTracker.INSTANCE.configSets().get(type);
        Set<IModConfig> filteredConfigSets = configSet.stream()
                .filter(config -> config.getModId().equals(context.modId()) && config.getSpec() instanceof ModConfigSpec)
                .map(NeoForgeConfig::new)
                .collect(Collectors.toSet());
        filteredConfigSets.forEach(consumer);
    }
}
