package com.mrcrayfish.configured.client.screen;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Author: MrCrayfish
 */
public interface IColouredTooltip
{
    List<Component> DUMMY_TOOLTIP = ImmutableList.of(CommonComponents.EMPTY);

    @Nullable
    List<FormattedCharSequence> getTooltipText();

    @Nullable
    Integer getTooltipX();

    @Nullable
    Integer getTooltipY();

    @Nullable
    Integer getTooltipOutlineColour();

    @Nullable
    Integer getTooltipBackgroundColour();

    default boolean drawColouredTooltip(GuiGraphics poseStack, int mouseX, int mouseY, Screen screen)
    {
        if(this.getTooltipText() != null)
        {
            boolean positioned = this.getTooltipX() != null && this.getTooltipY() != null;
            int x = positioned ? this.getTooltipX() + 12 : mouseX;
            int y = positioned ? this.getTooltipY() - 12 : mouseY;
            poseStack.renderComponentTooltip(Minecraft.getInstance().font, DUMMY_TOOLTIP, x, y); // Yep, this is strange. See the forge events below!
            return true;
        }
        return false;
    }
}
