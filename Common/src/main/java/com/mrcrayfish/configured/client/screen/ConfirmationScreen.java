package com.mrcrayfish.configured.client.screen;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mrcrayfish.configured.client.screen.widget.IconButton;
import com.mrcrayfish.configured.client.util.ScreenUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

/**
 * A simple versatile confirmation screen
 * <p>
 * Author: MrCrayfish
 */
public class ConfirmationScreen extends Screen implements IBackgroundTexture
{
    private static final int FADE_LENGTH = 4;
    private static final int BRIGHTNESS = 32;
    private static final int MESSAGE_PADDING = 10;

    private final Screen parent;
    private final Component message;
    private final Icon icon;
    private final Function<Boolean, Boolean> handler;
    private Component positiveText = CommonComponents.GUI_YES;
    private Component negativeText = CommonComponents.GUI_NO;
    private ResourceLocation background = Screen.BACKGROUND_LOCATION;
    private int startY, endY;

    public ConfirmationScreen(Screen parent, Component message, Icon icon, Function<Boolean, Boolean> handler)
    {
        super(message);
        this.parent = parent;
        this.message = message;
        this.icon = icon;
        this.handler = handler;
    }

    @Override
    protected void init()
    {
        List<FormattedCharSequence> lines = this.font.split(this.message, 300);
        this.startY = this.height / 2 - 10 - (lines.size() * (this.font.lineHeight + 2)) / 2 - MESSAGE_PADDING - 1;
        this.endY = this.startY + lines.size() * (this.font.lineHeight + 2) + MESSAGE_PADDING * 2;

        int offset = this.negativeText != null ? 105 : 50;
        this.addRenderableWidget(ScreenUtil.button(this.width / 2 - offset, this.endY + 10, 100, 20, this.positiveText, button ->
        {
            if(this.handler.apply(true))
            {
                this.minecraft.setScreen(this.parent);
            }
        }));
        if(this.negativeText != null)
        {
            this.addRenderableWidget(ScreenUtil.button(this.width / 2 + 5, this.endY + 10, 100, 20, this.negativeText, button ->
            {
                if(this.handler.apply(false))
                {
                    this.minecraft.setScreen(this.parent);
                }
            }));
        }
    }

    @Override
    public void render(GuiGraphics poseStack, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTicks);

        List<FormattedCharSequence> lines = this.font.split(this.message, 300);

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, IconButton.ICONS);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.blit(IconButton.ICONS, this.width / 2 - 10, this.startY - 30, 20, 20, this.icon.u(), this.icon.v(), 10, 10, 64, 64);

        drawListBackground(0.0, this.width, this.startY, this.endY);

        for(int i = 0; i < lines.size(); i++)
        {
            int lineWidth = this.font.width(lines.get(i));
            //TODO check for new
            //this.font.draw(poseStack, lines.get(i), this.width / 2 - lineWidth / 2, this.startY + MESSAGE_PADDING + i * (this.font.lineHeight + 2) + 1, 0xFFFFFF);
        }
    }

    @Override
    public ResourceLocation getBackgroundTexture()
    {
        return this.background;
    }

    /**
     * Sets the text for the positive button. This must be called before the screen is displayed.
     *
     * @param positiveText the text component to display as the positive button label
     */
    public void setPositiveText(Component positiveText)
    {
        this.positiveText = positiveText;
    }

    /**
     * Sets the text for the negative button. This must be called before the screen is displayed.
     *
     * @param negativeText the text component to display as the negative button label
     */
    public void setNegativeText(@Nullable Component negativeText)
    {
        this.negativeText = negativeText;
    }

    /**
     * Sets the image to use as the background for this screen
     *
     * @param background a resource location pointing to a texture
     */
    public ConfirmationScreen setBackground(ResourceLocation background)
    {
        this.background = background;
        return this;
    }

    public enum Icon
    {
        INFO(11, 44),
        WARNING(0, 11),
        ERROR(11, 11);

        private final int u, v;

        Icon(int u, int v)
        {
            this.u = u;
            this.v = v;
        }

        public int u()
        {
            return this.u;
        }

        public int v()
        {
            return this.v;
        }
    }

    public static void drawListBackground(double startX, double endX, double startY, double endY)
    {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, BACKGROUND_LOCATION);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.vertex(startX, endY, 0.0).uv((float) startX / 32.0F, (float) endY / 32.0F).color(BRIGHTNESS, BRIGHTNESS, BRIGHTNESS, 255).endVertex();
        buffer.vertex(endX, endY, 0.0).uv((float) endX / 32.0F, (float) endY / 32.0F).color(BRIGHTNESS, BRIGHTNESS, BRIGHTNESS, 255).endVertex();
        buffer.vertex(endX, startY, 0.0).uv((float) endX / 32.0F, (float) startY / 32.0F).color(BRIGHTNESS, BRIGHTNESS, BRIGHTNESS, 255).endVertex();
        buffer.vertex(startX, startY, 0.0).uv((float) startX / 32.0F, (float) startY / 32.0F).color(BRIGHTNESS, BRIGHTNESS, BRIGHTNESS, 255).endVertex();
        tesselator.end();

        RenderSystem.depthFunc(515);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(startX, startY + FADE_LENGTH, 0.0).color(0, 0, 0, 0).endVertex();
        buffer.vertex(endX, startY + FADE_LENGTH, 0.0).color(0, 0, 0, 0).endVertex();
        buffer.vertex(endX, startY, 0.0).color(0, 0, 0, 255).endVertex();
        buffer.vertex(startX, startY, 0.0).color(0, 0, 0, 255).endVertex();
        buffer.vertex(startX, endY, 0.0).color(0, 0, 0, 255).endVertex();
        buffer.vertex(endX, endY, 0.0).color(0, 0, 0, 255).endVertex();
        buffer.vertex(endX, endY - FADE_LENGTH, 0.0).color(0, 0, 0, 0).endVertex();
        buffer.vertex(startX, endY - FADE_LENGTH, 0.0).color(0, 0, 0, 0).endVertex();
        tesselator.end();
    }
}
