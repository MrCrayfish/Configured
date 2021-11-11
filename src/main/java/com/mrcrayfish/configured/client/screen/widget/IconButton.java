package com.mrcrayfish.configured.client.screen.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

/**
 * Author: MrCrayfish
 */
public class IconButton extends Button
{
    public static final ResourceLocation ICONS = new ResourceLocation("configured:textures/gui/icons.png");

    private final ITextComponent label;
    private final int u, v;

    public IconButton(int x, int y, int u, int v, IPressable onPress, Button.ITooltip onTooltip)
    {
        this(x, y, u, v, 20, StringTextComponent.EMPTY, onPress, onTooltip);
    }

    public IconButton(int x, int y, int u, int v, int width, ITextComponent label, IPressable onPress, Button.ITooltip onTooltip)
    {
        super(x, y, width, 20, StringTextComponent.EMPTY, onPress, onTooltip);
        this.label = label;
        this.u = u;
        this.v = v;
    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        super.renderButton(matrixStack, mouseX, mouseY, partialTicks);
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getTextureManager().bindTexture(ICONS);
        RenderSystem.enableDepthTest();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        int contentWidth = 10 + minecraft.fontRenderer.getStringPropertyWidth(this.label) + (!this.label.getString().isEmpty() ? 4 : 0);
        int iconX = this.x + (this.width - contentWidth) / 2;
        int iconY = this.y + 5;
        float brightness = this.active ? 1.0F : 0.5F;
        RenderSystem.color4f(brightness, brightness, brightness, this.alpha);
        blit(matrixStack, iconX, iconY, this.u, this.v, 11, 11, 64, 64);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.alpha);
        int textColor = this.getFGColor() | MathHelper.ceil(this.alpha * 255.0F) << 24;
        drawString(matrixStack, minecraft.fontRenderer, this.label, iconX + 14, iconY + 1, textColor);
    }
}
