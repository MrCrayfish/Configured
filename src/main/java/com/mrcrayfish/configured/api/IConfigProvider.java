package com.mrcrayfish.configured.api;

import net.minecraftforge.fml.ModContainer;

import java.util.Set;

/**
 * Use this class to allow mod configs from custom config implementations
 * to be shown in the mod config selection screen.
 * <p>
 * Providers must be defined in the mods.toml file.
 * <p><code>
 * [modproperties.mod_id]
 *     configuredProviders=[
 *         "com.example.path.to.MyProvider"
 *     ]
 * </code>
 * <p>
 * See the following classes for examples:
 * {@link com.mrcrayfish.configured.impl.forge.ForgeConfigProvider} and
 * {@link com.mrcrayfish.configured.impl.simple.SimpleConfigProvider}
 */
public interface IConfigProvider
{
    /**
     * Gets all the mod configs for a mod from the implementing provider.
     *
     * @param container the container of the mod to filter returned configs
     * @return a set of mod configs for the given mod
     */
    Set<IModConfig> getConfigurationsForMod(ModContainer container);
}
