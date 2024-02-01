package com.mrcrayfish.configured.platform.services;

import com.mrcrayfish.configured.api.IModConfigProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelResource;

import java.util.Set;

/**
 * Author: MrCrayfish
 */
public interface IConfigHelper
{
    LevelResource getServerConfigResource();

    Set<IModConfigProvider> getProviders();

    ResourceLocation getBackgroundTexture(String modId);
}
