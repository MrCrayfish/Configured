package com.mrcrayfish.configured.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.api.IModConfigProvider;
import com.mrcrayfish.configured.api.ModContext;
import com.mrcrayfish.configured.client.screen.IBackgroundTexture;
import com.mrcrayfish.configured.platform.Services;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClientHandler
{
    //TODO register this on fabric
    public static final KeyMapping KEY_OPEN_MOD_LIST = new KeyMapping("key.configured.open_mod_list", -1, "key.categories.configured");
    private static final Set<IModConfigProvider> PROVIDERS = new HashSet<>();
    private static final List<Function<ModContext, Supplier<Set<IModConfig>>>> LEGACY_PROVIDERS = new ArrayList<>();
    
    public static void init()
    {
        PROVIDERS.addAll(Services.CONFIG.getProviders());
        LEGACY_PROVIDERS.addAll(Services.CONFIG.getLegacyProviders());
    }

    public static Map<ConfigType, Set<IModConfig>> createConfigMap(ModContext context)
    {
        Map<ConfigType, Set<IModConfig>> modConfigMap = new HashMap<>();
        Set<IModConfig> configs = PROVIDERS.stream().flatMap(p -> streamConfigsFromProvider(context, p)).collect(Collectors.toSet());
        configs.addAll(LEGACY_PROVIDERS.stream()
                .map(p -> p.apply(context))
                .map(Supplier::get)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet()));
        configs.forEach(config -> modConfigMap.computeIfAbsent(config.getType(), type -> new LinkedHashSet<>()).add(config));
        return modConfigMap;
    }

    private static Stream<IModConfig> streamConfigsFromProvider(ModContext context, IModConfigProvider provider)
    {
        try
        {
            return provider.getConfigurationsForMod(context).stream();
        }
        catch(Exception e)
        {
            Constants.LOG.error("An error occurred when loading configs from provider: {}", provider.getClass().getName());
            e.printStackTrace();
        }
        return Stream.empty();
    }

    /**
     * Linked via ASM. Do not delete!
     */
    @SuppressWarnings("unused")
    public static void updateAbstractListTexture(AbstractSelectionList<?> list)
    {
        if(list instanceof IBackgroundTexture)
        {
            RenderSystem.setShaderTexture(0, ((IBackgroundTexture) list).getBackgroundTexture());
        }
    }

    /**
     * Linked via ASM. Do not delete!
     */
    @SuppressWarnings("unused")
    public static void updateScreenTexture(Screen screen)
    {
        if(screen instanceof IBackgroundTexture)
        {
            RenderSystem.setShaderTexture(0, ((IBackgroundTexture) screen).getBackgroundTexture());
        }
    }
}
