package com.mrcrayfish.configured.mixin.client.jei;

import com.mrcrayfish.configured.impl.jei.JeiReflection;
import mezz.jei.common.config.JEIClientConfigs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

/**
 * Author: MrCrayfish
 */
@Pseudo
@Mixin(JEIClientConfigs.class)
public class JEIClientConfigsMixin
{
    @Inject(method = "<init>", at = @At(value = "TAIL"))
    private void configuredInit(Path path, CallbackInfo ci)
    {
        JeiReflection.readSchema((JEIClientConfigs) (Object) this);
    }
}
