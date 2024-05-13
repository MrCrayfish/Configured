package com.mrcrayfish.configured.client.screen;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mrcrayfish.configured.client.screen.widget.IconButton;
import com.mrcrayfish.configured.client.util.ScreenUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.function.Function;

/**
 * A simple versatile confirmation screen
 * <p>
 * Author: MrCrayfish
 */
public class ConfirmationScreen extends Screen
{
    private static final ResourceLocation MENU_LIST_BACKGROUND = new ResourceLocation("textures/gui/menu_list_background.png");
    private static final ResourceLocation IN_GAME_MENU_LIST_BACKGROUND = new ResourceLocation("textures/gui/inworld_menu_list_background.png");
    private static final int FADE_LENGTH = 4;
    private static final int BRIGHTNESS = 32;
    private static final int MESSAGE_PADDING = 10;

    private final Screen parent;
    private final Component message;
    private final Icon icon;
    private final Function<Boolean, Boolean> handler;
    private Component positiveText = CommonComponents.GUI_YES;
    private Component negativeText = CommonComponents.GUI_NO;
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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        super.render(graphics, mouseX, mouseY, partialTicks);

        List<FormattedCharSequence> lines = this.font.split(this.message, 300);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(IconButton.ICONS, this.width / 2 - 10, this.startY - 30, 20, 20, this.icon.u(), this.icon.v(), 10, 10, 64, 64);

        this.drawListBackground(graphics, 0, this.width, this.startY, this.endY);

        for(int i = 0; i < lines.size(); i++)
        {
            int lineWidth = this.font.width(lines.get(i));
            graphics.drawString(this.font, lines.get(i), this.width / 2 - lineWidth / 2, this.startY + MESSAGE_PADDING + i * (this.font.lineHeight + 2) + 1, 0xFFFFFF);
            // TODO wat dis
            //this.font.draw(poseStack, lines.get(i), this.width / 2 - lineWidth / 2, this.startY + MESSAGE_PADDING + i * (this.font.lineHeight + 2) + 1, 0xFFFFFF);
        }
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

    public static void drawListBackground(GuiGraphics graphics, int startX, int endX, int startY, int endY)
    {
        boolean inGame = Minecraft.getInstance().level != null;
        RenderSystem.enableBlend();
        ResourceLocation backgroundTexture = !inGame ? MENU_LIST_BACKGROUND : IN_GAME_MENU_LIST_BACKGROUND;
        ResourceLocation headerTexture = !inGame ? Screen.HEADER_SEPARATOR : Screen.INWORLD_HEADER_SEPARATOR;
        ResourceLocation footerTexture = !inGame ? Screen.FOOTER_SEPARATOR : Screen.INWORLD_FOOTER_SEPARATOR;
        graphics.blit(backgroundTexture, startX, startY, (float) endX, (float) endY, endX - startX, endY - startY, 32, 32);
        graphics.blit(headerTexture, startX, startY - 2, 0, 0, endX - startX, 2, 32, 2);
        graphics.blit(footerTexture, startX, endY, 0, 0, endX - startX, 2, 32, 2);
        RenderSystem.disableBlend();
    }
}
