package com.mrcrayfish.configured.util;

import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.api.ModContext;
import com.mrcrayfish.configured.api.util.ConfigScreenHelper;
import com.mrcrayfish.configured.client.ClientHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModContainer;

import java.util.Map;
import java.util.Set;

/**
 * Author: MrCrayfish
 */
public class ForgeConfigScreenHelper
{
    /**
     * Multi Config Screen that automatically loads configs out of a mod if present
     * Automatically picks the previous screen
     *
     * @param title      of the ConfigScreen that should be applied.
     * @param mod        the mod the configs should be loaded from
     * @return a new screen with config selection included
     */
    public static Screen createForgeConfigSelectionScreen(Component title, ModContainer mod)
    {
        return createForgeConfigSelectionScreen(Minecraft.getInstance().screen, title, mod);
    }

    /**
     * Multi Config Screen that automatically loads configs out of a mod if present
     *
     * @param parent     screen that should be returned to after this new gui was closed
     * @param title      of the ConfigScreen that should be applied.
     * @param mod        the mod the configs should be loaded from
     * @return a new screen with config selection included
     */
    public static Screen createForgeConfigSelectionScreen(Screen parent, Component title, ModContainer mod)
    {
        Map<ConfigType, Set<IModConfig>> configs = ClientHandler.createConfigMap(new ModContext(mod.getModId()));
        return ConfigScreenHelper.createSelectionScreen(parent, title, configs);
    }
}
