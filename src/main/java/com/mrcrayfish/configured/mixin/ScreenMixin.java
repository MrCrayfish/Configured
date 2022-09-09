package com.mrcrayfish.configured.mixin;

import com.mrcrayfish.configured.client.screen.IBackgroundTexture;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Author: MrCrayfish
 */
@Mixin(Screen.class)
public class ScreenMixin
{
    @Inject(method = "renderDirtBackground", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderColor(FFFF)V"))
    public void afterSetTexture(int i, CallbackInfo ci)
    {
        IBackgroundTexture.loadTexture(this);
    }
}
