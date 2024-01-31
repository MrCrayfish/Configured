package com.mrcrayfish.configured.api;

import java.util.Set;

/**
 * Use this class to allow mod configs from custom config implementations
 * to be shown in the mod config selection screen.
 * <p>
 * If using Forge, providers must be defined in the mods.toml file.
 * <p><code>
 * [modproperties.mod_id]
 *     configuredProviders=[
 *         "com.example.path.to.MyProvider"
 *     ]
 * </code>
 * <p>
 * <p>
 * If using Fabric, providers must be defined in the fabric.mod.json file.
 * <p><code>
 * "custom": {
 *     "configured": {
 *         "providers": [
 *             "com.example.path.to.MyProvider"
 *         ]
 *     }
 * }
 * </code>
 * <p>
 */
public interface IModConfigProvider
{
    /**
     * Gets all the mod configs for a mod from the implementing provider.
     *
     * @param context the container of the mod to filter returned configs
     * @return a set of mod configs for the given mod
     */
    Set<IModConfig> getConfigurationsForMod(ModContext context);
}
