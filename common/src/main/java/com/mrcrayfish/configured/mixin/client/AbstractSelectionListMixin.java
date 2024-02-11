package com.mrcrayfish.configured.mixin.client;

import com.mrcrayfish.configured.client.screen.IBackgroundTexture;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Author: MrCrayfish
 */
@Mixin(AbstractSelectionList.class)
public abstract class AbstractSelectionListMixin
{
    public ResourceLocation beforeEnableDepthTest(ResourceLocation original)
    @ModifyArg(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"), index = 0)
    {
        if(original.equals(Screen.BACKGROUND_LOCATION) && this instanceof IBackgroundTexture texture)
        {
            return texture.getBackgroundTexture();
        }
        return original;
    }
}
