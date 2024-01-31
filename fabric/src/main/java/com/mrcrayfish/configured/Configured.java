package com.mrcrayfish.configured;

import net.fabricmc.api.ModInitializer;

/**
 * Author: MrCrayfish
 */
public class Configured implements ModInitializer
{
    @Override
    public void onInitialize()
    {
        Bootstrap.init();
    }
}
