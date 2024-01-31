package com.mrcrayfish.configured.platform.services;

import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.api.IModConfigProvider;
import com.mrcrayfish.configured.api.ModContext;
import com.mrcrayfish.configured.api.simple.SimpleConfig;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 */
public interface IConfigHelper
{
    Set<IModConfigProvider> getProviders();

    List<Function<ModContext, Supplier<Set<IModConfig>>>> getLegacyProviders();

    List<Pair<SimpleConfig, Object>> getAllSimpleConfigs();

    void sendLegacySimpleConfigLoadEvent(String modId, Object source);

    void sendLegacySimpleConfigUnloadEvent(String modId, Object source);

    void sendLegacySimpleConfigReloadEvent(String modId, Object source);

    ResourceLocation getBackgroundTexture(String modId);
}
