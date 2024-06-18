package com.mrcrayfish.configured.client.screen.widget;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mrcrayfish.configured.Constants;
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
    public static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/icons.png");

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
        graphics.blit(ICONS, this.getX(), this.getY(), this.isHoveredOrFocused() ? 50 : 36, this.isSelected() ? 49 : 35, 14, 14, 64, 64);
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
