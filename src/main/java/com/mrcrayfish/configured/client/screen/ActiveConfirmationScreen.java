package com.mrcrayfish.configured.client.screen;

import com.mrcrayfish.configured.api.IModConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Function;

/**
 * Author: MrCrayfish
 */
public class ActiveConfirmationScreen extends ConfirmationScreen implements IEditing
{
    private final IModConfig config;

    public ActiveConfirmationScreen(Screen parent, IModConfig config, Component message, Function<Boolean, Boolean> handler)
    {
        super(parent, message, handler);
        this.config = config;
    }

    @Override
    public IModConfig getActiveConfig()
    {
        return this.config;
    }
}
