package com.mrcrayfish.configured.client.screen.widget;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;

/**
 * Author: MrCrayfish
 */
public class CheckBoxButton extends AbstractButton
{
    public static final ResourceLocation ICONS = new ResourceLocation("configured:textures/gui/icons.png");

    private final OnPress onPress;
    private boolean selected;

    public CheckBoxButton(int x, int y, OnPress onPress)
    {
        super(x, y, 14, 14, CommonComponents.EMPTY);
        this.onPress = onPress;
    }

    public boolean isSelected()
    {
        return this.selected;
    }

    @Override
    public void onPress()
    {
        this.selected = !this.selected;
        this.onPress.onPress(this);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha); //TODO need this
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        graphics.blit(ICONS, this.getX(), this.getY(), this.isHoveredOrFocused() ? 50 : 36, this.isSelected() ? 49 : 35, 14, 14, 64, 64);
        //this.renderBg(poseStack, minecraft, mouseX, mouseY); //TODO wat this?
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput)
    {

    }

    public interface OnPress
    {
        void onPress(CheckBoxButton button);
    }
}
