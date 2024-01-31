package com.mrcrayfish.configured.api;

import net.fabricmc.loader.api.ModContainer;

import java.util.Set;

/**
 * Author: MrCrayfish
 */
@Deprecated(since = "2.2.0", forRemoval = true)
public interface IConfigProviderT
{
    Set<IModConfig> getConfigurationsForMod(ModContainer container);
}
