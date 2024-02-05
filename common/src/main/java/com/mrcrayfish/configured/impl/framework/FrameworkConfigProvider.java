package com.mrcrayfish.configured.impl.framework;

import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.api.IModConfigProvider;
import com.mrcrayfish.configured.api.ModContext;
import com.mrcrayfish.configured.platform.Services;
import com.mrcrayfish.framework.config.FrameworkConfigManager;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public class FrameworkConfigProvider implements IModConfigProvider
{
    @Override
    public Set<IModConfig> getConfigurationsForMod(ModContext context)
    {
        if(!Services.PLATFORM.isModLoaded("framework"))
            return Collections.emptySet();

        return FrameworkConfigManager.getInstance().getConfigs().stream().filter(config -> {
            return config.getName().getNamespace().equals(context.modId());
        }).map(FrameworkModConfig::new).collect(Collectors.toUnmodifiableSet());
    }
}
