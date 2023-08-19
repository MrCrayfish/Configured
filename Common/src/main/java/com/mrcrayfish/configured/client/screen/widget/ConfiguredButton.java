package com.mrcrayfish.configured.client.screen.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * Author: MrCrayfish
 */
public class ConfiguredButton extends Button
{
    private Tooltip tooltip;
    private Predicate<Button> tooltipPredicate = button -> true;

    protected ConfiguredButton(int x, int y, int width, int height, Component message, OnPress onPress, CreateNarration narration)
    {
        super(x, y, width, height, message, onPress, narration);
    }

    public void setTooltip(@Nullable Tooltip tooltip, Predicate<Button> predicate)
    {
        this.setTooltip(tooltip);
        this.tooltipPredicate = predicate;
        this.tooltip = tooltip;
    }

    @Override
    public void render(GuiGraphics poseStack, int mouseX, int mouseY, float partialTick)
    {
        if(this.visible)
        {
            this.setTooltip(this.tooltipPredicate.test(this) ? this.tooltip : null);
        }
        super.render(poseStack, mouseX, mouseY, partialTick);
    }
}
