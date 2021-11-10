package com.mrcrayfish.configured.client.util;

import com.mrcrayfish.configured.client.screen.ConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.function.Predicate;

/**
 * Author: MrCrayfish
 */
public class ScreenUtil
{
    public static boolean isMouseWithin(int x, int y, int width, int height, int mouseX, int mouseY)
    {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public static Button.ITooltip createButtonTooltip(Screen screen, ITextComponent message, int maxWidth)
    {
        return createButtonTooltip(screen, message, maxWidth, button -> button.active && button.isHovered());
    }

    public static Button.ITooltip createButtonTooltip(Screen screen, ITextComponent message, int maxWidth, Predicate<Button> predicate)
    {
        return (button, matrixStack, mouseX, mouseY) ->
        {
            if(predicate.test(button))
            {
                screen.renderTooltip(matrixStack, Minecraft.getInstance().fontRenderer.trimStringToWidth(message, maxWidth), mouseX, mouseY);
            }
        };
    }
}
