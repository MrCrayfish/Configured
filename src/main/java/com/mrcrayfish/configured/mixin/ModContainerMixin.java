package com.mrcrayfish.configured.mixin;

import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.EnumMap;

/**
 * Author: MrCrayfish
 */
@Mixin(ModContainer.class)
public interface ModContainerMixin
{
    @Accessor(value = "configs", remap = false)
    EnumMap<ModConfig.Type, ModConfig> getConfigs();
}
