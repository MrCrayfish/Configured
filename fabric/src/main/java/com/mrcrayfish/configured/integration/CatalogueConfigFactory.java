package com.mrcrayfish.configured.integration;

import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.api.ModContext;
import com.mrcrayfish.configured.api.util.ConfigScreenHelper;
import com.mrcrayfish.configured.client.ClientHandler;
import com.mrcrayfish.configured.platform.Services;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;

/**
 * Provides a config screen factory and provider to Catalogue (Fabric)
 *
 * Author: MrCrayfish
 */
public final class CatalogueConfigFactory
{
    // Do not change signature
    public static Screen createConfigScreen(Screen currentScreen, ModContainer container)
    {
        String modId = container.getMetadata().getId();
        Map<ConfigType, Set<IModConfig>> modConfigMap = ClientHandler.createConfigMap(new ModContext(modId));
        if(modConfigMap.isEmpty())
            return null;
        ResourceLocation backgroundTexture = Services.CONFIG.getBackgroundTexture(modId);
        return ConfigScreenHelper.createSelectionScreen(currentScreen, Component.literal(container.getMetadata().getName()), modConfigMap, backgroundTexture);
    }
}
