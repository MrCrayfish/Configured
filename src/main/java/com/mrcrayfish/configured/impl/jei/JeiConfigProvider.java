package com.mrcrayfish.configured.impl.jei;

import com.google.common.collect.ImmutableSet;
import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.IConfigProvider;
import com.mrcrayfish.configured.api.IModConfig;
import mezz.jei.common.config.file.ConfigSchema;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;

import java.util.HashSet;
import java.util.Set;

/**
 * Author: MrCrayfish
 */
@SuppressWarnings("unused")
public class JeiConfigProvider implements IConfigProvider
{
    @Override
    public Set<IModConfig> getConfigurationsForMod(ModContainer container)
    {
        if(!ModList.get().isLoaded("jei"))
            return ImmutableSet.of();

        Set<IModConfig> configs = new HashSet<>();
        ConfigSchema schema = JeiInstanceHolder.getClientSchema();
        if(schema != null)
        {
            configs.add(new JeiConfig("Client", ConfigType.CLIENT, schema));
        }
        return configs;
    }
}
