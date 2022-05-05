package com.mrcrayfish.configured.api;

import com.mrcrayfish.configured.client.ClientHandler;
import com.mrcrayfish.configured.client.screen.ConfigScreen;
import com.mrcrayfish.configured.client.screen.ModConfigSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.config.ModConfig;

import java.util.Map;
import java.util.Set;

/**
 * @author Speiger
 * <p>
 * Simple Helper class that allows you to easier find the starting point of how to add your own configured screen.
 */
public class ConfiguredHelper
{
    /**
     * Multi Config Screen that automatically loads configs out of a mod if present
     * Automatically picks the previous screen
     *
     * @param title      of the ConfigScreen that should be applied.
     * @param mod        the mod the configs should be loaded from
     * @param background of the config screen
     * @return a new screen with config selection included
     */

    @OnlyIn(Dist.CLIENT)
    public static Screen createForgeConfiguredScreen(ITextComponent title, ModContainer config, ResourceLocation background)
    {
        return createForgeConfiguredScreen(Minecraft.getInstance().currentScreen, title, config, background);
    }

    /**
     * Multi Config Screen that automatically loads configs out of a mod if present
     *
     * @param parent     screen that should be returned to after this new gui was closed
     * @param title      of the ConfigScreen that should be applied.
     * @param mod        the mod the configs should be loaded from
     * @param background of the config screen
     * @return a new screen with config selection included
     */
    @OnlyIn(Dist.CLIENT)
    public static Screen createForgeConfiguredScreen(Screen parent, ITextComponent title, ModContainer mod, ResourceLocation background)
    {
        Map<ModConfig.Type, Set<IModConfig>> configs = ClientHandler.createConfigMap(mod);
        return createConfiguredScreen(parent, title, configs, background);
    }

    /**
     * Multi Config Screen that allows you to sleect multiple configs at once.
     *
     * @param parent     screen that should be returned to after this new gui was closed
     * @param title      of the ConfigScreen that should be applied.
     * @param configs    the configs that should be in the screen
     * @param background of the config screen
     * @return a new screen with config selection included
     */
    @OnlyIn(Dist.CLIENT)
    public static Screen createConfiguredScreen(Screen parent, ITextComponent title, Map<ModConfig.Type, Set<IModConfig>> configs, ResourceLocation background)
    {
        return new ModConfigSelectionScreen(parent, title.getString(), background, configs);
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
    @OnlyIn(Dist.CLIENT)
    public static Screen createConfiguredScreen(ITextComponent title, IModConfig config, ResourceLocation background)
    {
        return createConfiguredScreen(Minecraft.getInstance().currentScreen, title, config, background);
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
    @OnlyIn(Dist.CLIENT)
    public static Screen createConfiguredScreen(Screen parent, ITextComponent title, IModConfig config, ResourceLocation background)
    {
        return new ConfigScreen(parent, title, config, background);
    }
}
