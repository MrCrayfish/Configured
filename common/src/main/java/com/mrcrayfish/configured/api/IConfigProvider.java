package com.mrcrayfish.configured.api;

import dev.architectury.platform.Mod;

import java.util.Set;

/**
 * Author: MrCrayfish
 */
public interface IConfigProvider
{
    Set<IModConfig> getConfigurationsForMod(Mod container);
}
