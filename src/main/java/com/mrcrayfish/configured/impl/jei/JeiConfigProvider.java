package com.mrcrayfish.configured.impl.jei;

import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.IConfigProvider;
import com.mrcrayfish.configured.api.IModConfig;
import mezz.jei.api.constants.ModIds;
import mezz.jei.api.runtime.config.IJeiConfigFile;
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
        if(!ModIds.JEI_ID.equals(container.getModId()))
        {
            return Set.of();
        }
        return ConfiguredJeiPlugin.getJeiConfigManager()
                .stream()
                .map(IJeiConfigManager::getConfigFiles)
                .flatMap(Collection::stream)
                .map(JeiConfigProvider::createClientConfig)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static IModConfig createClientConfig(IJeiConfigFile configFile)
    {
        return new JeiConfig("Client", ConfigType.CLIENT, configFile);
    }
}
