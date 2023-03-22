package com.mrcrayfish.configured.integration;

import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.api.ModContext;
import com.mrcrayfish.configured.platform.Services;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 */
public final class ModMenuConfigFactory implements ModMenuApi
{
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory()
    {
        return screen -> {
            return FabricLoader.getInstance().getModContainer(Constants.MOD_ID).map(container -> CatalogueConfigFactory.createConfigScreen(screen, container)).orElse(null);
        };
    }

    @Override
    public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories()
    {
        Map<String, ConfigScreenFactory<?>> modConfigFactories = new HashMap<>();
        Set<String> mods = new HashSet<>();
        FabricLoader.getInstance().getAllMods().forEach(container -> {
            ModContext context = new ModContext(container.getMetadata().getId());
            Services.CONFIG.getProviders().stream().flatMap(p -> p.getConfigurationsForMod(context).stream()).forEach(config -> {
                mods.add(config.getModId());
            });
            Services.CONFIG.getLegacyProviders().stream().map(p -> p.apply(context)).map(Supplier::get).flatMap(Collection::stream).forEach(config -> {
                mods.add(config.getModId());
            });
        });
        mods.removeIf(s -> s.equals(Constants.MOD_ID));
        mods.forEach(id -> {
            FabricLoader.getInstance().getModContainer(id).ifPresent(container -> {
                modConfigFactories.put(id, screen -> CatalogueConfigFactory.createConfigScreen(screen, container));
            });
        });
        return modConfigFactories;
    }
}
