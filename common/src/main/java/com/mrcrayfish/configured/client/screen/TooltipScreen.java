package com.mrcrayfish.configured.client.screen;

import com.google.common.collect.ImmutableList;
import com.mrcrayfish.configured.Constants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public abstract class TooltipScreen extends Screen
{
    private static final List<Component> DUMMY_TOOLTIP = ImmutableList.of(Component.empty());

    @Nullable
    public List<FormattedCharSequence> tooltipText;
    @Nullable
    public TooltipStyle tooltipStyle;

    protected TooltipScreen(Component title)
    {
        super(title);
    }

    protected void resetTooltip()
    {
        this.tooltipText = null;
        this.tooltipStyle = null;
    }

    /**
     * Sets the tool tip to render. Must be actively called in the render method as
     * the tooltip is reset every draw call.
     *
     * @param tooltip a tooltip list to show
     * @deprecated Use {@link #setActiveTooltip(GuiGraphicsExtractor, List, int, int)}
     */
    @Deprecated
    public void setActiveTooltip(@Nullable List<FormattedCharSequence> tooltip)
    {
        this.resetTooltip();
        this.tooltipText = tooltip;
    }

    /**
     * Sets the tool tip to render. Must be actively called in the render method as
     * the tooltip is reset every draw call.
     *
     * @param extractor the guigraphics instance
     * @param tooltip a tooltip list to show
     * @param mouseX the mouse x position
     * @param mouseY the mouse y position
     */
    public void setActiveTooltip(GuiGraphicsExtractor extractor, @Nullable List<FormattedCharSequence> tooltip, int mouseX, int mouseY)
    {
        this.resetTooltip();
        this.tooltipText = tooltip;
        if(tooltip != null)
        {
            Identifier style = this.tooltipStyle != null ? this.tooltipStyle.getTexture() : null;
            extractor.setTooltipForNextFrame(this.minecraft.font, tooltip, mouseX, mouseY, style);
        }
    }

    /**
     * Sets the tool tip from the given component. Must be actively called in the
     * render method as the tooltip is reset every draw call. This method automatically
     * splits the text.
     *
     * @param text the text to show on the tooltip
     */
    public void setActiveTooltip(GuiGraphicsExtractor extractor, Component text, int mouseX, int mouseY)
    {
        this.resetTooltip();
        Identifier style = this.tooltipStyle != null ? this.tooltipStyle.getTexture() : null;
        extractor.setTooltipForNextFrame(this.minecraft.font, this.minecraft.font.split(text, 200), mouseX, mouseY, style);
    }

    /**
     * Set the tool tip from the given component and colours. Must be actively called
     * in the render method as the tooltip is reset every draw call. This method
     * automatically splits the text.
     *
     * @param text the text to show on the tooltip
     */
    public void setActiveTooltip(GuiGraphicsExtractor extractor, Component text, int mouseX, int mouseY, @Nullable TooltipStyle style)
    {
        this.resetTooltip();
        this.tooltipText = this.minecraft.font.split(text, 200);
        this.tooltipStyle = style;
        Identifier styleTexture = this.tooltipStyle != null ? this.tooltipStyle.getTexture() : null;
        extractor.setTooltipForNextFrame(this.minecraft.font, this.tooltipText, mouseX, mouseY, styleTexture);
    }

    public record ListMenuTooltipComponent(FormattedCharSequence text) implements TooltipComponent
    {
        public ClientTextTooltip asClientTextTooltip()
        {
            return new ClientTextTooltip(this.text);
        }
    }

    public enum TooltipStyle
    {
        SUCCESS(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "success")),
        HINT(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "hint")),
        ERROR(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "error")),
        LINK(Identifier.fromNamespaceAndPath(Constants.MOD_ID, "link"));

        private final Identifier texture;

        TooltipStyle(Identifier texture)
        {
            this.texture = texture;
        }

        public Identifier getTexture()
        {
            return this.texture;
        }
    }
}
