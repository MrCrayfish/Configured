package com.mrcrayfish.configured.mixin;

import com.mrcrayfish.configured.client.screen.TooltipScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Author: MrCrayfish
 */
@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin
{
    @ModifyArg(method = "renderTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderTooltipBackground(Lnet/minecraft/client/gui/GuiGraphics;IIIILnet/minecraft/resources/ResourceLocation;)V"), index = 5)
    private ResourceLocation modifyTooltipBackground(ResourceLocation original)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if(!(minecraft.screen instanceof TooltipScreen screen))
            return original;

        if(screen.tooltipText == null)
            return original;

        if(screen.tooltipStyle == null)
            return original;

        return screen.tooltipStyle.getTexture();
    }
}
