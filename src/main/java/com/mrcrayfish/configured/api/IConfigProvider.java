package com.mrcrayfish.configured.api;

import net.fabricmc.loader.api.ModContainer;

import java.util.Set;

/**
 * Author: MrCrayfish
 */
public interface IConfigProvider
{
    Set<IModConfig> getConfigurationsForMod(ModContainer container);
}
