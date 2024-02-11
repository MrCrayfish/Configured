package com.mrcrayfish.configured.mixin.client;

import com.mrcrayfish.configured.client.screen.IBackgroundTexture;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Author: MrCrayfish
 */
@Mixin(Screen.class)
public class ScreenMixin
{
    @ModifyArg(method = "renderDirtBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIFFIIII)V", ordinal = 0), index = 0)
    public ResourceLocation configuredModifyBlitBackgroundTexture(ResourceLocation location)
    {
        return IBackgroundTexture.loadTexture(this, location);
    }
}
