package com.mrcrayfish.configured.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mrcrayfish.configured.client.screen.IBackgroundTexture;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Author: MrCrayfish
 */
@Mixin(AbstractSelectionList.class)
public class AbstractSelectionListMixin
{
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderColor(FFFF)V"))
    public void beforeSetShaderColor(PoseStack poseStack, int mouseX, int mouseY, float partialTick, CallbackInfo ci)
    {
        IBackgroundTexture.loadTexture(this);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableDepthTest()V"))
    public void beforeEnableDepthTest(PoseStack poseStack, int mouseX, int mouseY, float partialTick, CallbackInfo ci)
    {
        IBackgroundTexture.loadTexture(this);
    }
}
