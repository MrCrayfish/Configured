package com.mrcrayfish.configured.impl.simple;

import com.mrcrayfish.configured.api.IConfigProvider;
import com.mrcrayfish.configured.api.IModConfig;
import net.minecraftforge.fml.ModContainer;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public final class SimpleConfigProvider implements IConfigProvider
{
    @Override
    public Set<IModConfig> getConfigurationsForMod(ModContainer container)
    {
         // Check against event mod container
        return SimpleConfigManager.getInstance().getConfigs().stream().filter(entry -> entry.getModId().equals(container.getModId())).collect(Collectors.toSet());
    }
}
