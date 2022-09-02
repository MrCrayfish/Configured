package com.mrcrayfish.configured.client.screen;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import com.mrcrayfish.configured.Reference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Author: MrCrayfish
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public abstract class TooltipScreen extends Screen
{
    private static final List<Component> DUMMY_TOOLTIP = ImmutableList.of(TextComponent.EMPTY);

    @Nullable
    protected List<FormattedCharSequence> tooltipText;
    @Nullable
    private Integer tooltipX;
    @Nullable
    private Integer tooltipY;
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
        this.tooltipX = null;
        this.tooltipY = null;
        this.tooltipBackgroundColour = null;
        this.tooltipOutlineColour = null;
    }

    /**
     * Sets the tool tip to render. Must be actively called in the render method as
     * the tooltip is reset every draw call.
     *
     * @param tooltip a tooltip list to show
     */
    public void setTooltip(List<FormattedCharSequence> tooltip)
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
    public void setActiveTooltip(Component text, int x, int y, int outlineColour, int backgroundColour)
    {
        this.resetTooltip();
        this.tooltipText = this.minecraft.font.split(text, 200);
        this.tooltipX = x;
        this.tooltipY = y;
        this.tooltipBackgroundColour = backgroundColour;
        this.tooltipOutlineColour = outlineColour;
    }

    protected void drawTooltip(PoseStack poseStack, int mouseX, int mouseY)
    {
        if(this.tooltipText != null)
        {
            int height = this.tooltipText.size() * 10 + (this.tooltipText.size() > 1 ? 2 : 0);
            boolean positioned = this.tooltipX != null && this.tooltipY != null;
            int x = positioned ? this.tooltipX - 8 : mouseX;
            int y = positioned ? this.tooltipY + 10 - height : mouseY;
            this.renderComponentTooltip(poseStack, DUMMY_TOOLTIP, x, y); // Yep, this is strange. See the forge events below!
        }
    }

    public static void registerTooltipFactory()
    {
        MinecraftForgeClient.registerTooltipComponentFactory(ListMenuTooltipComponent.class, ListMenuTooltipComponent::asClientTextTooltip);
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
    }
}
