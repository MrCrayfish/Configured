package com.mrcrayfish.configured.client.screen.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;

/**
 * Author: MrCrayfish
 */
public class IconButton extends Button
{
    public static final ResourceLocation ICONS = new ResourceLocation("configured:textures/gui/icons.png");

    private int u, v;
    private Tooltip tooltip;

    public IconButton(int x, int y, int width, int height, int u, int v, IPressable pressedAction)
    {
        super(x, y, width, height, "", pressedAction);
        this.u = u;
        this.v = v;
    }

    public IconButton(int x, int y, int width, int height, int u, int v, Tooltip tooltip, IPressable pressedAction)
    {
        super(x, y, width, height, "", pressedAction);
        this.u = u;
        this.v = v;
        this.tooltip = tooltip;
    }

    @Override
    public void renderButton(int mouseX, int mouseY, float partialTicks)
    {
        Minecraft mc = Minecraft.getInstance();
        FontRenderer font = mc.fontRenderer;
        mc.getTextureManager().bindTexture(WIDGETS_LOCATION);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.alpha);
        int vOffset = this.getYImage(this.isHovered());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        this.blit(this.x, this.y, 0, 46 + vOffset * 20, this.width / 2, this.height);
        this.blit(this.x + this.width / 2, this.y, 200 - this.width / 2, 46 + vOffset * 20, this.width / 2, this.height);
        this.renderBg(mc, mouseX, mouseY);
        int color = this.getFGColor();
        drawCenteredString(font, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, color | MathHelper.ceil(this.alpha * 255.0F) << 24);
        mc.getTextureManager().bindTexture(ICONS);
        float brightness = this.active ? 1.0F : 0.5F;
        RenderSystem.color4f(brightness, brightness, brightness, this.alpha);
        blit(this.x + 5, this.y + 4, this.getBlitOffset(), this.u, this.v, 11, 11, 32, 32);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.alpha);
    }

    public interface Tooltip
    {
        void onTooltip(Button button, int mouseX, int mouseY);
    }
}
