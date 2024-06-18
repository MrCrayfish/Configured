package com.mrcrayfish.configured.client.screen;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.client.screen.widget.IconButton;
import com.mrcrayfish.configured.client.util.ScreenUtil;
import com.mrcrayfish.configured.util.ConfigHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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

    protected void updateTooltip(int mouseX, int mouseY)
    {
        if(ScreenUtil.isMouseWithin(10, 13, 23, 23, mouseX, mouseY))
        {
            this.setActiveTooltip(Component.translatable("configured.gui.info"));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        // Resets the active tooltip each draw call
        this.resetTooltip();

        // Draws the background texture (dirt or custom texture)
        super.render(graphics, mouseX, mouseY, partialTicks); // TODO check

        // Draws widgets manually since they are not buttons
        this.list.render(graphics, mouseX, mouseY, partialTicks);
        this.searchTextField.render(graphics, mouseX, mouseY, partialTicks);

        // Draw title
        graphics.drawCenteredString(this.font, this.title,this.width / 2, 7, 0xFFFFFF);

        // Draws the foreground. Allows subclasses to draw onto the screen at the appropriate time.
        this.renderForeground(graphics, mouseX, mouseY, partialTicks);

        // Draws the Configured logo in the top left of the screen
        graphics.blit(CONFIGURED_LOGO, 10, 13, 0, 0, 0, 23, 23, 32, 32);

        // Draws the search icon next to the search text field
        graphics.blit(IconButton.ICONS, this.width / 2 - 128, 26, 14, 14, 22, 11, 10, 10, 64, 64);

        // Gives a chance for child classes to set the active tooltip
        this.updateTooltip(mouseX, mouseY);

        // Draws the active tooltip otherwise tries to draw button tooltips
        if(this.tooltipText != null)
        {
            this.drawTooltip(graphics, mouseX, mouseY);
        }
        else
        {
            for(GuiEventListener widget : this.children())
            {
                if(widget instanceof Button && ((Button) widget).isHoveredOrFocused())
                {
                    //TODO check
                    //((Button) widget).renderToolTip(poseStack, mouseX, mouseY);
                    break;
                }
            }
        }
    }

    protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {}

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

    protected class EntryList extends ContainerObjectSelectionList<Item>
    {
        public EntryList(List<Item> entries)
        {
            super(ListMenuScreen.this.minecraft, ListMenuScreen.this.width, ListMenuScreen.this.height - 36 - 50, 50, ListMenuScreen.this.itemHeight);
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
            this.renderToolTips(graphics, mouseX, mouseY);
        }

        private void renderToolTips(GuiGraphics graphics, int mouseX, int mouseY)
        {
            this.children().forEach(item ->
            {
                item.children().forEach(o ->
                {
                    if(o instanceof Button)
                    {
                        //TODO figure this out
                        //((Button) o).renderToolTip(poseStack, mouseX, mouseY);
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
            this.label = Component.literal(label);
        }

        @Override
        public String getLabel()
        {
            return this.label.getString();
        }

        @Override
        public void render(GuiGraphics graphics, int x, int top, int left, int width, int height, int mouseX, int mouseY, boolean selected, float partialTicks)
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
        public void render(GuiGraphics graphics, int x, int top, int left, int width, int height, int mouseX, int mouseY, boolean selected, float partialTicks)
        {
            graphics.drawCenteredString(ListMenuScreen.this.minecraft.font, this.label, left + width / 2, top + 5, 0xFFFFFF);
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
        public void render(GuiGraphics graphics, int x, int top, int left, int width, int height, int mouseX, int mouseY, boolean selected, float partialTicks)
        {
            graphics.drawCenteredString(ListMenuScreen.this.minecraft.font, this.label, left + width / 2, top, 0xFFFFFFFF);
            graphics.drawCenteredString(ListMenuScreen.this.minecraft.font, this.bottomText, left + width / 2, top + 12, 0xFFFFFFFF);

            if(this.isMouseOver(mouseX, mouseY))
            {
                Style style = this.bottomText.getStyle();
                HoverEvent event = style.getHoverEvent();
                if(event != null && event.getAction() == HoverEvent.Action.SHOW_TEXT)
                {
                    ListMenuScreen.this.setActiveTooltip(event.getValue(HoverEvent.Action.SHOW_TEXT), 0xFFFCA800);
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
                RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha); // TODO test alpha
                boolean hovered = ScreenUtil.isMouseWithin(this.getX() + this.width - 15, this.getY() + 5, 9, 9, mouseX, mouseY);
                graphics.blit(IconButton.ICONS, this.getX() + this.width - 15, this.getY() + 5, 9, 9, hovered ? 9 : 0, 55, 9, 9, 64, 64);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button)
        {
            if(this.clearable && !this.getValue().isEmpty() && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && ScreenUtil.isMouseWithin(this.getX() + this.width - 15, this.getY() + 5, 9, 9, (int) mouseX, (int) mouseY))
            {
                this.playDownSound(ListMenuScreen.this.minecraft.getSoundManager());
                this.setValue("");
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    protected interface IIgnoreSearch {}
}
