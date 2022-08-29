package com.mrcrayfish.configured.client.screen;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import com.mrcrayfish.configured.Reference;
import com.mrcrayfish.configured.client.util.ScreenUtil;
import com.mrcrayfish.configured.util.ConfigHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public abstract class ListMenuScreen extends Screen implements IBackgroundTexture
{
    public static final ResourceLocation CONFIGURED_LOGO = new ResourceLocation(Reference.MOD_ID, "textures/gui/logo.png");
    public static final List<Component> DUMMY_TOOLTIP = ImmutableList.of(TextComponent.EMPTY);

    protected final Screen parent;
    protected final ResourceLocation background;
    protected final int itemHeight;
    protected EntryList list;
    protected List<Item> entries;
    protected TooltipHolder tooltip = new TooltipHolder();
    protected FocusedEditBox activeTextField;
    protected FocusedEditBox searchTextField;

    protected ListMenuScreen(Screen parent, Component title, ResourceLocation background, int itemHeight)
    {
        super(title);
        this.parent = parent;
        this.background = background;
        this.itemHeight = itemHeight;
    }

    @Override
    protected void init()
    {
        // Constructs a list of entries and adds them to an option list
        List<Item> entries = new ArrayList<>();
        this.constructEntries(entries);
        this.entries = ImmutableList.copyOf(entries); //Should this still be immutable?
        this.list = new EntryList(this.entries);
        this.list.setRenderBackground(!ConfigHelper.isPlayingGame());
        this.addWidget(this.list);

        // Adds a search text field to the top of the screen
        this.searchTextField = new FocusedEditBox(this.font, this.width / 2 - 110, 22, 220, 20, new TextComponent("Search"));
        this.searchTextField.setResponder(s ->
        {
            ScreenUtil.updateSearchTextFieldSuggestion(this.searchTextField, s, this.entries);
            this.list.replaceEntries(s.isEmpty() ? this.entries : this.entries.stream().filter(item -> {
                return !(item instanceof IIgnoreSearch) && item.getLabel().toLowerCase(Locale.ENGLISH).contains(s.toLowerCase(Locale.ENGLISH));
            }).collect(Collectors.toList()));
            if(!s.isEmpty())
            {
                this.list.setScrollAmount(0);
            }
        });
        this.addWidget(this.searchTextField);
        ScreenUtil.updateSearchTextFieldSuggestion(this.searchTextField, "", this.entries);
    }

    protected abstract void constructEntries(List<Item> entries);

    @Override
    public ResourceLocation getBackgroundTexture()
    {
        return this.background;
    }

    /**
     * Sets the tool tip to render. Must be actively called in the render method as
     * the tooltip is reset every draw call.
     *
     * @param tooltip a tooltip list to show
     */
    public void setActiveTooltip(List<FormattedCharSequence> tooltip)
    {
        this.tooltip.set(tooltip);
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
        this.tooltip.set(this.minecraft.font.split(text, 200));
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
        this.tooltip.set(this.minecraft.font.split(text, 200), outlineColour, backgroundColour);
    }

    protected void updateTooltip(int mouseX, int mouseY)
    {
        if(ScreenUtil.isMouseWithin(10, 13, 23, 23, mouseX, mouseY))
        {
            this.setActiveTooltip(this.minecraft.font.split(new TranslatableComponent("configured.gui.info"), 200));
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
    {
        // Resets the active tooltip each draw call
        this.tooltip.set(null);

        // Draws the background texture (dirt or custom texture)
        this.renderBackground(poseStack);

        // Draws widgets manually since they are not buttons
        this.list.render(poseStack, mouseX, mouseY, partialTicks);
        this.searchTextField.render(poseStack, mouseX, mouseY, partialTicks);

        // Draw title
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 7, 0xFFFFFF);

        super.render(poseStack, mouseX, mouseY, partialTicks);

        // Draws the foreground. Allows subclasses to draw onto the screen at the appropriate time.
        this.renderForeground(poseStack, mouseX, mouseY, partialTicks);

        // Draws the Configured logo in the top left of the screen
        RenderSystem.setShaderTexture(0, CONFIGURED_LOGO);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        Screen.blit(poseStack, 10, 13, this.getBlitOffset(), 0, 0, 23, 23, 32, 32);

        // Gives a chance for child classes to set the active tooltip
        this.updateTooltip(mouseX, mouseY);

        // Draws the active tooltip otherwise tries to draw button tooltips
        if(this.tooltip != null && this.tooltip.text != null)
        {
            // Yep, this is probably strange to you. See the forge events below!
            this.renderComponentTooltip(poseStack, DUMMY_TOOLTIP, mouseX, mouseY);
        }
        else
        {
            for(GuiEventListener widget : this.children())
            {
                if(widget instanceof Button && ((Button) widget).isHoveredOrFocused())
                {
                    ((Button) widget).renderToolTip(poseStack, mouseX, mouseY);
                    break;
                }
            }
        }
    }

    protected void renderForeground(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {}

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if(ScreenUtil.isMouseWithin(10, 13, 23, 23, (int) mouseX, (int) mouseY))
        {
            Style style = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.curseforge.com/minecraft/mc-mods/configured"));
            this.handleComponentClicked(style);
            return true;
        }
        if(this.activeTextField != null && !this.activeTextField.isMouseOver(mouseX, mouseY))
        {
            this.activeTextField.setFocused(false);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    protected class EntryList extends ContainerObjectSelectionList<Item> implements IBackgroundTexture
    {
        public EntryList(List<Item> entries)
        {
            super(ListMenuScreen.this.minecraft, ListMenuScreen.this.width, ListMenuScreen.this.height, 50, ListMenuScreen.this.height - 36, ListMenuScreen.this.itemHeight);
            entries.forEach(this::addEntry);
        }

        @Override
        protected int getScrollbarPosition()
        {
            return this.width / 2 + 144;
        }

        @Override
        public int getRowWidth()
        {
            return 260;
        }

        @Override
        public ResourceLocation getBackgroundTexture()
        {
            return ListMenuScreen.this.background;
        }

        // Overridden simply to make it public
        @Override
        public void replaceEntries(Collection<Item> entries)
        {
            super.replaceEntries(entries);
        }

        @Override
        public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
        {
            super.render(poseStack, mouseX, mouseY, partialTicks);
            this.renderToolTips(poseStack, mouseX, mouseY);
        }

        private void renderToolTips(PoseStack poseStack, int mouseX, int mouseY)
        {
            this.children().forEach(item ->
            {
                item.children().forEach(o ->
                {
                    if(o instanceof Button)
                    {
                        ((Button) o).renderToolTip(poseStack, mouseX, mouseY);
                    }
                });
            });
        }
    }

    protected abstract class Item extends ContainerObjectSelectionList.Entry<Item> implements ILabelProvider, Comparable<Item>
    {
        protected final Component label;
        @Nullable
        protected List<FormattedCharSequence> tooltip;

        public Item(Component label)
        {
            this.label = label;
        }

        public Item(String label)
        {
            this.label = new TextComponent(label);
        }

        @Override
        public String getLabel()
        {
            return this.label.getString(); //TODO test
        }

        public void setMouseTooltip(Component text, int maxWidth)
        {
            this.tooltip = ListMenuScreen.this.minecraft.font.split(text, maxWidth);
        }

        @Override
        public List<? extends GuiEventListener> children()
        {
            return Collections.emptyList();
        }

        @Override
        public List<? extends NarratableEntry> narratables()
        {
            return ImmutableList.of(new NarratableEntry()
            {
                @Override
                public NarratableEntry.NarrationPriority narrationPriority()
                {
                    return NarratableEntry.NarrationPriority.HOVERED;
                }

                @Override
                public void updateNarration(NarrationElementOutput output)
                {
                    output.add(NarratedElementType.TITLE, Item.this.label);
                }
            });
        }

        @Override
        public int compareTo(ListMenuScreen.Item o)
        {
            return this.label.getString().compareTo(o.label.getString());
        }
    }

    public class TitleItem extends Item implements IIgnoreSearch
    {
        public TitleItem(Component title)
        {
            super(title);
        }

        public TitleItem(String title)
        {
            super(new TextComponent(title).withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.YELLOW));
        }

        @Override
        public void render(PoseStack poseStack, int x, int top, int left, int width, int height, int mouseX, int mouseY, boolean selected, float partialTicks)
        {
            Screen.drawCenteredString(poseStack, ListMenuScreen.this.minecraft.font, this.label, left + width / 2, top + 5, 0xFFFFFF);
        }
    }

    public class SubTitleItem extends Item implements IIgnoreSearch
    {
        public SubTitleItem(Component title)
        {
            super(title);
        }

        public SubTitleItem(String title)
        {
            super(new TextComponent(title).withStyle(ChatFormatting.GRAY));
        }

        @Override
        public void render(PoseStack poseStack, int x, int top, int left, int width, int height, int mouseX, int mouseY, boolean selected, float partialTicks)
        {
            Screen.drawCenteredString(poseStack, ListMenuScreen.this.minecraft.font, this.label, left + width / 2, top + 5, 0xFFFFFF);
        }
    }

    protected class FocusedEditBox extends EditBox
    {
        public FocusedEditBox(Font font, int x, int y, int width, int height, Component label)
        {
            super(font, x, y, width, height, label);
        }

        @Override
        protected void setFocused(boolean focused)
        {
            super.setFocused(focused);
            if(focused)
            {
                if(ListMenuScreen.this.activeTextField != null && ListMenuScreen.this.activeTextField != this)
                {
                    ListMenuScreen.this.activeTextField.setFocused(false);
                }
                ListMenuScreen.this.activeTextField = this;
            }
        }
    }

    protected interface IIgnoreSearch {}

    public static void registerTooltipFactory()
    {
        MinecraftForgeClient.registerTooltipComponentFactory(ListMenuTooltipComponent.class, ListMenuTooltipComponent::asClientTextTooltip);
    }

    private static class TooltipHolder
    {
        private List<FormattedCharSequence> text;
        private Integer borderColour;
        private Integer backgroundColour;

        public void set(@Nullable List<FormattedCharSequence> text)
        {
            this.text = text;
            this.borderColour = null;
            this.backgroundColour = null;
        }

        public void set(List<FormattedCharSequence> text, int outlineColour, int backgroundColour)
        {
            this.text = text;
            this.borderColour = outlineColour;
            this.backgroundColour = backgroundColour;
        }

        public boolean isActive()
        {
            return this.text != null;
        }

        public boolean isColoured()
        {
            return this.borderColour != null && this.backgroundColour != null;
        }
    }

    private static class ListMenuTooltipComponent implements TooltipComponent
    {
        private FormattedCharSequence text;

        public ListMenuTooltipComponent(FormattedCharSequence text)
        {
            this.text = text;
        }

        public ClientTextTooltip asClientTextTooltip()
        {
            return new ClientTextTooltip(this.text);
        }
    }

    @SubscribeEvent
    public static void onGatherTooltipComponents(RenderTooltipEvent.GatherComponents event)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if(minecraft.screen instanceof ListMenuScreen listMenu && listMenu.tooltip.isActive())
        {
            event.getTooltipElements().clear();
            for(FormattedCharSequence text : listMenu.tooltip.text)
            {
                event.getTooltipElements().add(Either.right(new ListMenuTooltipComponent(text)));
            }
        }
    }

    @SubscribeEvent
    public static void onGetTooltipColor(RenderTooltipEvent.Color event)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if(minecraft.screen instanceof ListMenuScreen listMenu && listMenu.tooltip.isActive())
        {
            if(listMenu.tooltip.isColoured())
            {
                event.setBorderStart(listMenu.tooltip.borderColour);
                event.setBorderEnd(listMenu.tooltip.borderColour);
                event.setBackground(listMenu.tooltip.backgroundColour);
            }
        }
    }
}
