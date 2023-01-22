package com.mrcrayfish.configured.client.util;

import com.mrcrayfish.configured.client.screen.ILabelProvider;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
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
    /**
     * Determines if the mouse is within the specified area.
     *
     * @param x      the x position of the area
     * @param y      the y position of the area
     * @param width  the width of the area
     * @param height the height of the area
     * @param mouseX the x position of the mouse
     * @param mouseY the y position of the mouse
     * @return true if the mouse is within the area
     */
    public static boolean isMouseWithin(int x, int y, int width, int height, int mouseX, int mouseY)
    {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    /**
     * A helper method to create a button tooltip. The tooltip will only render if the button is
     * active and is hovered by the cursor.
     *
     * @param screen   the screen that will render the tooltip
     * @param message  the message to show in the tooltip
     * @param maxWidth the maximum text wrap width
     * @return a new button tooltip instance
     */
    public static Tooltip createTooltip(Screen screen, Component message, int maxWidth)
    {
        return Tooltip.create(message);
    }

    /**
     * Creates a button tooltip but only shows if the given predicate is true
     *
     * @param screen    the screen that will render the tooltip
     * @param message   the message to show in the tooltip
     * @param maxWidth  the maximum text wrap width
     * @param predicate the condition to determine if the tooltip should render
     * @return a new button tooltip instance
     */
    public static Tooltip createTooltip(Screen screen, Component message, int maxWidth, Predicate<Button> predicate)
    {
        return Tooltip.create(message);
       /* return (button, poseStack, mouseX, mouseY) ->
        {
            if(predicate.test(button))
            {
                screen.renderTooltip(poseStack, Minecraft.getInstance().font.split(message, maxWidth), mouseX, mouseY);
            }
        };*/
    }

    /**
     * Updates the suggestion of a {@link EditBox} based on a list of label providers.
     *
     * @param editBox the edit box to update the suggestion
     * @param value   the user input value
     * @param entries a list of label providers to test the user input against
     */
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

    public static Button button(int x, int y, int width, int height, Component label, Button.OnPress onPress)
    {
        return Button.builder(label, onPress).pos(x, y).size(width, height).build();
    }
}
