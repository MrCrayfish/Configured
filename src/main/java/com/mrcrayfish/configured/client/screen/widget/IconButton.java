package com.mrcrayfish.configured.client.screen.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Author: MrCrayfish
 */
public class IconButton extends Button
{
    public static final ResourceLocation ICONS = new ResourceLocation("configured:textures/gui/icons.png");

    private int u, v;

    public IconButton(int x, int y, int width, int height, int u, int v, Button.OnPress pressedAction)
    {
        super(x, y, width, height, TextComponent.EMPTY, pressedAction);
        this.u = u;
        this.v = v;
    }

    public IconButton(int x, int y, int width, int height, int u, int v, Button.OnTooltip onTooltip, Button.OnPress pressedAction)
    {
        super(x, y, width, height, TextComponent.EMPTY, pressedAction, onTooltip);
        this.u = u;
        this.v = v;
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
    {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
        int vOffset = this.getYImage(this.isHovered());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        this.blit(poseStack, this.x, this.y, 0, 46 + vOffset * 20, this.width / 2, this.height);
        this.blit(poseStack, this.x + this.width / 2, this.y, 200 - this.width / 2, 46 + vOffset * 20, this.width / 2, this.height);
        this.renderBg(poseStack, minecraft, mouseX, mouseY);
        int color = this.getFGColor();
        drawCenteredString(poseStack, font, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, color | Mth.ceil(this.alpha * 255.0F) << 24);
        RenderSystem.setShaderTexture(0, ICONS);
        float brightness = this.active ? 1.0F : 0.5F;
        RenderSystem.setShaderColor(brightness, brightness, brightness, this.alpha);
        blit(poseStack, this.x + 5, this.y + 4, this.getBlitOffset(), this.u, this.v, 11, 11, 32, 32);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
    }
}
