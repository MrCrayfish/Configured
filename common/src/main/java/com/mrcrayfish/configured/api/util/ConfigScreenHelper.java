package com.mrcrayfish.configured.api.util;

import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.client.screen.ConfigScreen;
import com.mrcrayfish.configured.client.screen.ModConfigSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Set;

/**
 * @author Speiger
 * <p>
 * Simple Helper class that allows you to easier find the starting point of how to add your own configured screen.
 */
public class ConfigScreenHelper
{
    /**
     * Multi Config Screen that allows you to select multiple configs at once.
     *
     * @param parent     screen that should be returned to after this new gui was closed
     * @param title      of the ConfigScreen that should be applied.
     * @param configs    the configs that should be in the screen
     * @param background of the config screen
     * @return a new screen with config selection included
     */
    public static Screen createSelectionScreen(Screen parent, Component title, Map<ConfigType, Set<IModConfig>> configs, ResourceLocation background)
    {
        return new ModConfigSelectionScreen(parent, title, background, configs);
    }

    /**
     * Simple single config screen generator.
     * Automatically picks the previous screen
     *
     * @param title      of the ConfigScreen that should be applied.
     * @param config     that should be displayed
     * @param background of the config screen
     * @return Screen for 1 single config file
     */
    public static Screen createSelectionScreen(Component title, IModConfig config, ResourceLocation background)
    {
        return createSelectionScreen(Minecraft.getInstance().screen, title, config, background);
    }

    /**
     * Simple single config screen generator.
     *
     * @param parent     screen that should be returned to after this new gui was closed
     * @param title      of the ConfigScreen that should be applied.
     * @param config     that should be displayed
     * @param background of the config screen
     * @return Screen for 1 single config file
     */
    public static Screen createSelectionScreen(Screen parent, Component title, IModConfig config, ResourceLocation background)
    {
        return new ConfigScreen(parent, title, config, background);
    }
}
