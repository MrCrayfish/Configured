package com.mrcrayfish.configured.client.screen;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.client.screen.widget.IconButton;
import com.mrcrayfish.configured.client.util.ScreenUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public abstract class ListMenuScreen extends TooltipScreen
{
    public static final ResourceLocation CONFIGURED_LOGO = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/logo.png");

    protected final Screen parent;
    protected final int itemHeight;
    protected EntryList list;
    protected List<Item> entries;
    protected FocusedEditBox activeTextField;
    protected FocusedEditBox searchTextField;

    protected ListMenuScreen(Screen parent, Component title, int itemHeight)
    {
        super(title);
        this.parent = parent;;
        this.itemHeight = itemHeight;
    }

    protected abstract void constructEntries(List<Item> entries);

    @Override
    protected void init()
    {
        // Constructs a list of entries and adds them to an option list
        List<Item> entries = new ArrayList<>();
        this.constructEntries(entries);
        this.entries = ImmutableList.copyOf(entries); //Should this still be immutable?
        this.list = new EntryList(this.entries);
        //this.list.setRenderBackground(!ConfigHelper.isPlayingGame());
        this.addWidget(this.list);

        // Adds a search text field to the top of the screen
        this.searchTextField = new FocusedEditBox(this.font, this.width / 2 - 110, 22, 220, 20, Component.translatable("configured.gui.search"));
        this.searchTextField.setClearable(true);
        this.searchTextField.setResponder(s -> this.updateSearchResults());
        this.addWidget(this.searchTextField);
        ScreenUtil.updateSearchTextFieldSuggestion(this.searchTextField, "", this.entries);
    }

    protected void updateSearchResults()
    {
        String query = this.searchTextField.getValue();
        ScreenUtil.updateSearchTextFieldSuggestion(this.searchTextField, query, this.entries);
        this.list.replaceEntries(query.isEmpty() ? this.entries : this.getSearchResults(query));
        if(!query.isEmpty())
        {
            this.list.setScrollAmount(0);
        }
    }

    protected Collection<Item> getSearchResults(String s)
    {
        return this.entries.stream().filter(item -> {
            return !(item instanceof IIgnoreSearch) && item.getLabel().toLowerCase(Locale.ENGLISH).contains(s.toLowerCase(Locale.ENGLISH));
        }).collect(Collectors.toList());
    }

    protected void updateTooltip(GuiGraphics graphics, int mouseX, int mouseY)
    {
        if(ScreenUtil.isMouseWithin(10, 13, 23, 23, mouseX, mouseY))
        {
            this.setActiveTooltip(graphics, Component.translatable("configured.gui.info"), mouseX, mouseY);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        // Resets the active tooltip each draw call
        this.resetTooltip();

        // Draws the background texture (dirt or custom texture)
        super.render(graphics, mouseX, mouseY, partialTicks);

        // Draws widgets manually since they are not buttons
        this.list.render(graphics, mouseX, mouseY, partialTicks);
        this.searchTextField.render(graphics, mouseX, mouseY, partialTicks);

        // Draw title
        graphics.drawCenteredString(this.font, this.title,this.width / 2, 7, 0xFFFFFFFF);

        // Draws the foreground. Allows subclasses to draw onto the screen at the appropriate time.
        this.renderForeground(graphics, mouseX, mouseY, partialTicks);

        // Draws the Configured logo in the top left of the screen
        graphics.blit(RenderPipelines.GUI_TEXTURED, CONFIGURED_LOGO, 10, 13, 0, 0, 23, 23, 32, 32);

        // Draws the search icon next to the search text field
        graphics.blit(RenderPipelines.GUI_TEXTURED, IconButton.ICONS, this.width / 2 - 128, 26, 22, 11, 14, 14, 10, 10, 64, 64);

        // Gives a chance for child classes to set the active tooltip
        this.updateTooltip(graphics, mouseX, mouseY);
    }

    protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {}

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        if(ScreenUtil.isMouseWithin(10, 13, 23, 23, (int) event.x(), (int) event.y()))
        {
            Style style = Style.EMPTY.withClickEvent(new ClickEvent.OpenUrl(URI.create("https://www.curseforge.com/minecraft/mc-mods/configured")));
            this.handleComponentClicked(style);
            return true;
        }
        if(this.activeTextField != null && !this.activeTextField.isMouseOver(event.x(), event.y()))
        {
            this.activeTextField.setFocused(false);
        }
        return super.mouseClicked(event, doubleClick);
    }

    protected class EntryList extends ContainerObjectSelectionList<Item>
    {
        public EntryList(List<Item> entries)
        {
            super(ListMenuScreen.this.minecraft, ListMenuScreen.this.width, ListMenuScreen.this.height - 36 - 50, 50, ListMenuScreen.this.itemHeight);
            entries.forEach(this::addEntry);
        }

        @Override
        protected int scrollBarX()
        {
            return this.width / 2 + 144;
        }

        @Override
        public int getRowWidth()
        {
            return 260;
        }

        // Overridden simply to make it public
        @Override
        public void replaceEntries(Collection<Item> entries)
        {
            super.replaceEntries(entries);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
        {
            super.renderWidget(graphics, mouseX, mouseY, partialTicks);
        }

        @Override
        protected void renderListItems(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
        {
            List<Item> entries = this.children();
            for(int i = 0; i < entries.size(); i++)
            {
                var entry = entries.get(i);
                if(entry.getY() + entry.getHeight() >= this.getY() && entry.getY() <= this.getBottom())
                {
                    if(i % 2 != 0 && entry instanceof EntryBackground)
                    {
                        graphics.fill(entry.getX() - 3, entry.getY(), entry.getX() + entry.getWidth() + 3, entry.getY() + 1, 0x33000000);
                        graphics.fill(entry.getX() - 4, entry.getY() + 1, entry.getX() + entry.getWidth() + 4, entry.getY() + entry.getHeight() - 1, 0x33000000);
                        graphics.fill(entry.getX() - 3, entry.getY() + entry.getHeight() - 1, entry.getX() + entry.getWidth() + 3, entry.getY() + entry.getHeight(), 0x33000000);
                    }
                    this.renderItem(graphics, mouseX, mouseY, partialTick, entry);
                }
            }
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
            this.label = Component.literal(label);
        }

        @Override
        public String getLabel()
        {
            return this.label.getString();
        }

        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick)
        {
            if(this.isMouseOver(mouseX, mouseY))
            {
                ListMenuScreen.this.setActiveTooltip(this.tooltip);
            }
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
                public NarrationPriority narrationPriority()
                {
                    return NarrationPriority.HOVERED;
                }

                @Override
                public void updateNarration(NarrationElementOutput output)
                {
                    output.add(NarratedElementType.TITLE, Item.this.label);
                }
            });
        }

        @Override
        public int compareTo(Item o)
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
            super(Component.literal(title).withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.YELLOW));
        }

        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick)
        {
            graphics.drawCenteredString(ListMenuScreen.this.minecraft.font, this.label, this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - font.lineHeight) / 2, 0xFFFFFFFF);
        }
    }

    public class MultiTextItem extends Item implements IIgnoreSearch
    {
        private final Component bottomText;

        public MultiTextItem(Component topText, Component bottomText)
        {
            super(topText);
            this.bottomText = bottomText;
        }

        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick)
        {
            graphics.drawCenteredString(ListMenuScreen.this.minecraft.font, this.label, this.getX() + this.getWidth() / 2, this.getY(), 0xFFFFFFFF);
            graphics.drawCenteredString(ListMenuScreen.this.minecraft.font, this.bottomText, this.getX() + this.getWidth() / 2, this.getY() + 12, 0xFFFFFFFF);

            if(this.isMouseOver(mouseX, mouseY))
            {
                Style style = this.bottomText.getStyle();
                HoverEvent event = style.getHoverEvent();
                if(event instanceof HoverEvent.ShowText(Component value))
                {
                    ListMenuScreen.this.setActiveTooltip(graphics, value, mouseX, mouseY, TooltipStyle.LINK);
                }
            }
        }
    }

    protected class FocusedEditBox extends EditBox
    {
        private boolean clearable = false;

        public FocusedEditBox(Font font, int x, int y, int width, int height, Component label)
        {
            super(font, x, y, width, height, label);
        }

        public FocusedEditBox setClearable(boolean clearable)
        {
            this.clearable = clearable;
            return this;
        }

        @Override
        public void setFocused(boolean focused)
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

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
        {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            if(this.clearable && !this.getValue().isEmpty())
            {
                boolean hovered = ScreenUtil.isMouseWithin(this.getX() + this.width - 15, this.getY() + 5, 9, 9, mouseX, mouseY);
                graphics.blit(RenderPipelines.GUI_TEXTURED, IconButton.ICONS, this.getX() + this.width - 15, this.getY() + 5, hovered ? 9 : 0, 55, 9, 9, 9, 9, 64, 64);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
        {
            if(this.clearable && !this.getValue().isEmpty() && event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && ScreenUtil.isMouseWithin(this.getX() + this.width - 15, this.getY() + 5, 9, 9, (int) event.x(), (int) event.y()))
            {
                this.playDownSound(ListMenuScreen.this.minecraft.getSoundManager());
                this.setValue("");
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }
    }

    protected interface IIgnoreSearch {}
}
