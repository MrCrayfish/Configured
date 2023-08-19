package com.mrcrayfish.configured.mixin.client;

import com.mrcrayfish.configured.client.screen.IBackgroundTexture;
import net.minecraft.client.gui.GuiGraphics;
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
    //TODO fix this mixin, I don't know how to do it
    @Inject(method = "renderDirtBackground", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderColor(FFFF)V", ordinal = 0))
    public void afterSetTexture(GuiGraphics poseStack, CallbackInfo ci)
    {
        IBackgroundTexture.loadTexture(this);
    }
}
