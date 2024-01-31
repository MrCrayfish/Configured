package com.mrcrayfish.configured.impl.simple;

import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.api.IModConfigProvider;
import com.mrcrayfish.configured.api.ModContext;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public final class SimpleConfigProvider implements IModConfigProvider
{
    @Override
    public Set<IModConfig> getConfigurationsForMod(ModContext context)
    {
         // Check against event mod container
        return SimpleConfigManager.getInstance().getConfigs().stream().filter(entry -> entry.getModId().equals(context.modId())).collect(Collectors.toSet());
    }
}
