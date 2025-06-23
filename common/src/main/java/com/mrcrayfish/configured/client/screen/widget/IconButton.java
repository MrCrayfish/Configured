package com.mrcrayfish.configured.client.screen.widget;

import com.mrcrayfish.configured.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * Author: MrCrayfish
 */
public class IconButton extends ConfiguredButton
{
    public static final ResourceLocation ICONS = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/icons.png");

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
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        Minecraft mc = Minecraft.getInstance();
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
            int j = ARGB.white(brightness);
            graphics.blit(RenderPipelines.GUI_TEXTURED, ICONS, iconX, iconY, this.u, this.v, 11, 11, 64, 64, j); //TODO what happen to blit offset
        }
        int textColor = (this.active ? 0xFFFFFFFF : 0xFFA0A0A0) | Mth.ceil(this.alpha * 255.0F) << 24;
        graphics.drawString(mc.font, this.label, iconX + 14, iconY + 1, textColor);
    }

    @Override
    protected MutableComponent createNarrationMessage()
    {
        return wrapDefaultNarrationMessage(this.label);
    }
}
