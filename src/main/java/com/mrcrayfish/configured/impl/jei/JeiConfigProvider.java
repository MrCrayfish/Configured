package com.mrcrayfish.configured.impl.jei;

import com.google.common.collect.ImmutableSet;
import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.IConfigProvider;
import com.mrcrayfish.configured.api.IModConfig;
import mezz.jei.api.runtime.config.IJeiConfigManager;
import net.minecraftforge.fml.ModContainer;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
@SuppressWarnings("unused")
public class JeiConfigProvider implements IConfigProvider
{
    @Override
    public Set<IModConfig> getConfigurationsForMod(ModContainer container)
    {
        if(container.getModId().equals("jei"))
        {
            return ConfiguredJeiPlugin.getJeiConfigManager().stream()
                    .map(IJeiConfigManager::getConfigFiles)
                    .flatMap(Collection::stream)
                    .map(file -> new JeiConfig("Client", ConfigType.CLIENT, file))
                    .collect(Collectors.toUnmodifiableSet());
        }
        return ImmutableSet.of();
    }
}
