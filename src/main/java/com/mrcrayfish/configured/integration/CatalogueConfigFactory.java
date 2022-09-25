package com.mrcrayfish.configured.integration;

import com.google.common.collect.ImmutableMap;
import com.mrcrayfish.configured.Reference;
import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.util.ConfigScreenHelper;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.client.ClientHandler;
import com.mrcrayfish.configured.impl.simple.SimpleConfigManager;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.minecraft.ResourceLocationException;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Provides a config screen factory and provider to Catalogue (Fabric)
 *
 * Author: MrCrayfish
 */
public final class CatalogueConfigFactory
{
    public static Screen createConfigScreen(Screen currentScreen, ModContainer container)
    {
        Map<ConfigType, Set<IModConfig>> modConfigMap = new HashMap<>();
        Set<IModConfig> configs = ClientHandler.getProviders().stream().flatMap(provider -> provider.getConfigurationsForMod(container).stream()).collect(Collectors.toSet());
        configs.forEach(entry -> modConfigMap.computeIfAbsent(entry.getType(), type -> new LinkedHashSet<>()).add(entry));
        ResourceLocation backgroundTexture = getBackgroundTexture(container);
        return ConfigScreenHelper.createSelectionScreen(currentScreen, Component.literal(container.getMetadata().getName()), modConfigMap, backgroundTexture);
    }

    public static Map<String, BiFunction<Screen, ModContainer, Screen>> createConfigProvider()
    {
        Map<String, BiFunction<Screen, ModContainer, Screen>> providers = new HashMap<>();
        SimpleConfigManager.getInstance().getConfigs().stream().map(SimpleConfigManager.SimpleConfigImpl::getModId).distinct().forEach(s -> {
            if(!s.equals(Reference.MOD_ID)) providers.put(s, CatalogueConfigFactory::createConfigScreen);
        });
        return ImmutableMap.copyOf(providers);
    }

    private static ResourceLocation getBackgroundTexture(ModContainer container)
    {
        CustomValue value = container.getMetadata().getCustomValue("configured");
        if(value != null && value.getType() == CustomValue.CvType.OBJECT)
        {
            CustomValue.CvObject configuredObj = value.getAsObject();
            CustomValue backgroundValue = configuredObj.get("background");
            if(backgroundValue != null && backgroundValue.getType() == CustomValue.CvType.STRING)
            {
                try
                {
                    return new ResourceLocation(backgroundValue.getAsString());
                }
                catch(ResourceLocationException var2)
                {
                    return Screen.BACKGROUND_LOCATION;
                }
            }
        }
        return Screen.BACKGROUND_LOCATION;
    }
}
