package com.mrcrayfish.configured.client.screen;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Author: MrCrayfish
 */
public abstract class TooltipScreen extends Screen
{
    private static final List<Component> DUMMY_TOOLTIP = ImmutableList.of(Component.empty());

    @Nullable
    protected List<FormattedCharSequence> tooltipText;
    @Nullable
    private Integer tooltipOutlineColour;
    @Nullable
    private Integer tooltipBackgroundColour;

    protected TooltipScreen(Component title)
    {
        super(title);
    }

    protected void resetTooltip()
    {
        this.tooltipText = null;
        this.tooltipBackgroundColour = null;
        this.tooltipOutlineColour = null;
    }

    /**
     * Sets the tool tip to render. Must be actively called in the render method as
     * the tooltip is reset every draw call.
     *
     * @param tooltip a tooltip list to show
     */
    public void setActiveTooltip(List<FormattedCharSequence> tooltip)
    {
        this.resetTooltip();
        this.tooltipText = tooltip;
    }

    /**
     * Sets the tool tip from the given component. Must be actively called in the
     * render method as the tooltip is reset every draw call. This method automatically
     * splits the text.
     *
     * @param text the text to show on the tooltip
     */
    public void setActiveTooltip(Component text)
    {
        this.resetTooltip();
        this.tooltipText = this.minecraft.font.split(text, 200);
    }

    /**
     * Set the tool tip from the given component and colours. Must be actively called
     * in the render method as the tooltip is reset every draw call. This method
     * automatically splits the text.
     *
     * @param text the text to show on the tooltip
     */
    public void setActiveTooltip(Component text, int outlineColour, int backgroundColour)
    {
        this.resetTooltip();
        this.tooltipText = this.minecraft.font.split(text, 200);
        this.tooltipBackgroundColour = backgroundColour;
        this.tooltipOutlineColour = outlineColour;
    }

    protected void drawTooltip(PoseStack poseStack, int mouseX, int mouseY)
    {
        if(this.tooltipText != null)
        {
            // Yep, this is strange. See the forge events below!
            this.renderTooltip(poseStack, this.tooltipText, mouseX, mouseY);
        }
    }

    /*public static void registerTooltipFactory(RegisterClientTooltipComponentFactoriesEvent event)
    {
        event.register(ListMenuTooltipComponent.class, ListMenuTooltipComponent::asClientTextTooltip);
    }

    private record ListMenuTooltipComponent(FormattedCharSequence text) implements TooltipComponent
    {
        public ClientTextTooltip asClientTextTooltip()
        {
            return new ClientTextTooltip(this.text);
        }
    }

    @SubscribeEvent
    public static void onGatherTooltipComponents(RenderTooltipEvent.GatherComponents event)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if(!(minecraft.screen instanceof TooltipScreen screen))
            return;

        if(screen.tooltipText == null)
            return;

        event.getTooltipElements().clear();

        for(FormattedCharSequence text : screen.tooltipText)
        {
            event.getTooltipElements().add(Either.right(new ListMenuTooltipComponent(text)));
        }
    }

    @SubscribeEvent
    public static void onGetTooltipColor(RenderTooltipEvent.Color event)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if(!(minecraft.screen instanceof TooltipScreen screen))
            return;

        if(screen.tooltipText == null)
            return;

        if(screen.tooltipBackgroundColour == null || screen.tooltipOutlineColour == null)
            return;

        event.setBorderStart(screen.tooltipOutlineColour);
        event.setBorderEnd(screen.tooltipOutlineColour);
        event.setBackground(screen.tooltipBackgroundColour);
    }*/
}
