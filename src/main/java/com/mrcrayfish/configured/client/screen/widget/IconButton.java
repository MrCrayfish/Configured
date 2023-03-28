package com.mrcrayfish.configured.client.screen.widget;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Author: MrCrayfish
 */
public class IconButton extends ConfiguredButton
{
    public static final ResourceLocation ICONS = new ResourceLocation("configured:textures/gui/icons.png");

    private final Component label;
    private final int u, v;

    public IconButton(int x, int y, int u, int v, OnPress onPress)
    {
        this(x, y, u, v, 20, CommonComponents.EMPTY, onPress);
    }

    public IconButton(int x, int y, int u, int v, int width, Component label, OnPress onPress)
    {
        super(x, y, width, 20, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
        this.label = label;
        this.u = u;
        this.v = v;
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
    {
        super.renderButton(poseStack, mouseX, mouseY, partialTicks);
        Minecraft mc = Minecraft.getInstance();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, ICONS);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        int contentWidth = 10 + mc.font.width(this.label) + (!this.label.getString().isEmpty() ? 4 : 0);
        boolean renderIcon = contentWidth <= this.width;
        if(!renderIcon)
        {
            contentWidth = mc.font.width(this.label);
        }
        int iconX = this.getX() + (this.width - contentWidth) / 2;
        int iconY = this.getY() + 5;
        float brightness = this.active ? 1.0F : 0.5F;
        if(renderIcon)
        {
            RenderSystem.setShaderColor(brightness, brightness, brightness, this.alpha);
            blit(poseStack, iconX, iconY, this.getBlitOffset(), this.u, this.v, 11, 11, 64, 64);
        }
        int textX = renderIcon ? iconX + 14 : this.getX() + (this.width - mc.font.width(this.label)) / 2;
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
        int textColor = this.getFGColor() | Mth.ceil(this.alpha * 255.0F) << 24;
        drawString(poseStack, mc.font, this.label, textX, iconY + 1, textColor);
    }

    @Override
    protected MutableComponent createNarrationMessage()
    {
        return wrapDefaultNarrationMessage(this.label);
    }
}
