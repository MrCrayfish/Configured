package com.mrcrayfish.configured.integration;

import com.mrcrayfish.configured.Reference;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.client.ClientHandler;
import com.mrcrayfish.configured.impl.simple.SimpleConfigManager;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public final class ModMenuConfigFactory implements ModMenuApi
{
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory()
    {
        return screen -> {
            return FabricLoader.getInstance().getModContainer(Reference.MOD_ID).map(container -> CatalogueConfigFactory.createConfigScreen(screen, container)).orElse(null);
        };
    }

    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories()
    {
        Map<String, ConfigScreenFactory<?>> modConfigFactories = new HashMap<>();
        Set<String> mods = new HashSet<>();
        FabricLoader.getInstance().getAllMods().forEach(container -> {
            ClientHandler.getProviders().stream().flatMap(provider -> provider.getConfigurationsForMod(container).stream()).forEach(config -> {
                mods.add(config.getModId());
            });
        });
        mods.removeIf(s -> s.equals(Reference.MOD_ID));
        mods.forEach(id -> {
            FabricLoader.getInstance().getModContainer(id).ifPresent(container -> {
                modConfigFactories.put(id, screen -> CatalogueConfigFactory.createConfigScreen(screen, container));
            });
        });
        return modConfigFactories;
    }
}
