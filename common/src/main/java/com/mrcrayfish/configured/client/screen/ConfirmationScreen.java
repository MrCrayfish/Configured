package com.mrcrayfish.configured.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mrcrayfish.configured.client.screen.widget.IconButton;
import com.mrcrayfish.configured.client.util.ScreenUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
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
    private static final ResourceLocation MENU_LIST_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/menu_list_background.png");
    private static final ResourceLocation IN_GAME_MENU_LIST_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/inworld_menu_list_background.png");
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

        graphics.blit(RenderPipelines.GUI_TEXTURED, IconButton.ICONS, this.width / 2 - 10, this.startY - 30, this.icon.u(), this.icon.v(), 20, 20, 10, 10, 64, 64);

        drawListBackground(graphics, 0, this.width, this.startY, this.endY);

        for(int i = 0; i < lines.size(); i++)
        {
            int lineWidth = this.font.width(lines.get(i));
            graphics.drawString(this.font, lines.get(i), this.width / 2 - lineWidth / 2, this.startY + MESSAGE_PADDING + i * (this.font.lineHeight + 2) + 1, 0xFFFFFFFF);
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
        ResourceLocation backgroundTexture = !inGame ? MENU_LIST_BACKGROUND : IN_GAME_MENU_LIST_BACKGROUND;
        ResourceLocation headerTexture = !inGame ? Screen.HEADER_SEPARATOR : Screen.INWORLD_HEADER_SEPARATOR;
        ResourceLocation footerTexture = !inGame ? Screen.FOOTER_SEPARATOR : Screen.INWORLD_FOOTER_SEPARATOR;
        graphics.blit(RenderPipelines.GUI_TEXTURED, backgroundTexture, startX, startY, (float) endX, (float) endY, endX - startX, endY - startY, 32, 32);
        graphics.blit(RenderPipelines.GUI_TEXTURED, headerTexture, startX, startY - 2, 0, 0, endX - startX, 2, 32, 2);
        graphics.blit(RenderPipelines.GUI_TEXTURED, footerTexture, startX, endY, 0, 0, endX - startX, 2, 32, 2);
    }

    public static void showInfo(Minecraft minecraft, Screen parent, Component message)
    {
        showMessage(minecraft, parent, message, Icon.INFO);
    }

    public static void showError(Minecraft minecraft, Screen parent, Component message)
    {
        showMessage(minecraft, parent, message, Icon.ERROR);
    }

    private static void showMessage(Minecraft minecraft, Screen parent, Component message, ConfirmationScreen.Icon icon)
    {
        ConfirmationScreen confirm = new ConfirmationScreen(parent, message, icon, result -> true);
        confirm.setPositiveText(Component.translatable("configured.gui.close"));
        confirm.setNegativeText(null);
        minecraft.setScreen(confirm);
    }
}
