package com.mrcrayfish.configured.mixin.client;

import com.mrcrayfish.configured.client.screen.IBackgroundTexture;
import net.minecraft.client.gui.components.AbstractSelectionList;
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
    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V"), index = 1)
    public ResourceLocation beforeEnableDepthTest(ResourceLocation original)
    {
        if(this instanceof IBackgroundTexture texture) //TODO not sure what background this is testing agains so I commented this out original.equals(GuiComponent.BACKGROUND_LOCATION) &&
        {
            return texture.getBackgroundTexture();
        }
        return original;
    }
}
