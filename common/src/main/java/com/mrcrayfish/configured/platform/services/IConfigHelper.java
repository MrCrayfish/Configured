package com.mrcrayfish.configured.platform.services;

import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.api.IModConfigProvider;
import com.mrcrayfish.configured.api.ModContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelResource;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 */
public interface IConfigHelper
{
    LevelResource getServerConfigResource();

    Set<IModConfigProvider> getProviders();

    List<Function<ModContext, Supplier<Set<IModConfig>>>> getLegacyProviders();

    ResourceLocation getBackgroundTexture(String modId);
}
