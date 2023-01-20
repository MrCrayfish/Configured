package com.mrcrayfish.configured.impl.simple;

import com.mrcrayfish.configured.api.IConfigProvider;
import com.mrcrayfish.configured.api.IModConfig;
import dev.architectury.platform.Mod;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public class SimpleConfigProvider implements IConfigProvider
{
    @Override
    public Set<IModConfig> getConfigurationsForMod(Mod container)
    {
        return SimpleConfigManager.getInstance().getConfigs().stream().filter(entry -> entry.getModId().equals(container.getModId())).collect(Collectors.toSet());
    }
}
