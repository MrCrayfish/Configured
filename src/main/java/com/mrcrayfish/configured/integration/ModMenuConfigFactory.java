package com.mrcrayfish.configured.integration;

import com.mrcrayfish.configured.Reference;
import com.mrcrayfish.configured.impl.simple.SimpleFactory;
import com.mrcrayfish.configured.impl.simple.SimpleConfigManager;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Author: MrCrayfish
 */
public final class ModMenuConfigFactory implements ModMenuApi
{
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory()
    {
        return screen -> {
            return FabricLoader.getInstance().getModContainer(Reference.MOD_ID).map(container -> SimpleFactory.createConfigScreen(screen, container)).orElse(null);
        };
    }

    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories()
    {
        Map<String, ConfigScreenFactory<?>> modConfigFactories = new HashMap<>();
        Set<String> mods = new HashSet<>();
        SimpleConfigManager.getInstance().getConfigs().forEach(config -> mods.add(config.getModId()));
        mods.removeIf(s -> s.equals(Reference.MOD_ID));
        mods.forEach(id -> {
            FabricLoader.getInstance().getModContainer(id).ifPresent(container -> {
                modConfigFactories.put(id, screen -> SimpleFactory.createConfigScreen(screen, container));
            });
        });
        return modConfigFactories;
    }
}
