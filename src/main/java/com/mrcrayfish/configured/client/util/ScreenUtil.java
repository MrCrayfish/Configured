package com.mrcrayfish.configured.client.util;

import com.mrcrayfish.configured.client.screen.ILabelProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

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

    public static Button.OnTooltip createButtonTooltip(Screen screen, Component message, int maxWidth)
    {
        return createButtonTooltip(screen, message, maxWidth, button -> button.active && button.isHoveredOrFocused());
    }

    public static Button.OnTooltip createButtonTooltip(Screen screen, Component message, int maxWidth, Predicate<Button> predicate)
    {
        return (button, poseStack, mouseX, mouseY) ->
        {
            if(predicate.test(button))
            {
                screen.renderTooltip(poseStack, Minecraft.getInstance().font.split(message, maxWidth), mouseX, mouseY);
            }
        };
    }

    public static void updateSearchTextFieldSuggestion(EditBox editBox, String value, List<? extends ILabelProvider> entries)
    {
        if(!value.isEmpty())
        {
            Optional<? extends ILabelProvider> optional = entries.stream().filter(info -> info.getLabel().toLowerCase(Locale.ENGLISH).startsWith(value.toLowerCase(Locale.ENGLISH))).min(Comparator.comparing(ILabelProvider::getLabel));
            if(optional.isPresent())
            {
                int length = value.length();
                String displayName = optional.get().getLabel();
                editBox.setSuggestion(displayName.substring(length));
            }
            else
            {
                editBox.setSuggestion("");
            }
        }
        else
        {
            editBox.setSuggestion(Component.translatable("configured.gui.search").getString());
        }
    }
}
