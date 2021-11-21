package com.mrcrayfish.configured.client.util;

import com.mrcrayfish.configured.client.screen.ILabelProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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

    public static void updateSearchTextFieldSuggestion(TextFieldWidget textField, String value, List<? extends ILabelProvider> entries)
    {
        if(!value.isEmpty())
        {
            Optional<? extends ILabelProvider> optional = entries.stream().filter(info -> info.getLabel().toLowerCase(Locale.ENGLISH).startsWith(value.toLowerCase(Locale.ENGLISH))).min(Comparator.comparing(ILabelProvider::getLabel));
            if(optional.isPresent())
            {
                int length = value.length();
                String displayName = optional.get().getLabel();
                textField.setSuggestion(displayName.substring(length));
            }
            else
            {
                textField.setSuggestion("");
            }
        }
        else
        {
            textField.setSuggestion(new TranslationTextComponent("configured.gui.search").getString());
        }
    }
}
