package com.mrcrayfish.configured.client.screen;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mrcrayfish.configured.Reference;
import com.mrcrayfish.configured.client.util.ScreenUtil;
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
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public abstract class ListMenuScreen extends Screen implements IBackgroundTexture
{
    public static final ResourceLocation CONFIGURED_LOGO = new ResourceLocation(Reference.MOD_ID, "textures/gui/logo.png");

    protected final Screen parent;
    protected final ResourceLocation background;
    protected final int itemHeight;
    protected EntryList list;
    protected List<Item> entries;
    protected List<FormattedCharSequence> activeTooltip;
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
        this.list.setRenderBackground(!isPlayingGame());
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
        this.activeTooltip = tooltip;
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
        this.activeTooltip = null;

        // Draws the background texture (dirt or custom texture)
        this.renderBackground(poseStack);

        // Draws widgets manually since they are not buttons
        this.list.render(poseStack, mouseX, mouseY, partialTicks);
        this.searchTextField.render(poseStack, mouseX, mouseY, partialTicks);

        // Draw title
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 7, 0xFFFFFF);

        super.render(poseStack, mouseX, mouseY, partialTicks);

        // Draws the Configured logo in the top left of the screen
        RenderSystem.setShaderTexture(0, CONFIGURED_LOGO);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        Screen.blit(poseStack, 10, 13, this.getBlitOffset(), 0, 0, 23, 23, 32, 32);

        // Gives a chance for child classes to set the active tooltip
        this.updateTooltip(mouseX, mouseY);

        // Draws the active tooltip otherwise tries to draw button tooltips
        if(this.activeTooltip != null)
        {
            this.renderTooltip(poseStack, this.activeTooltip, mouseX, mouseY);
        }
        else
        {
            for(GuiEventListener widget : this.children())
            {
                if(widget instanceof Button && ((Button) widget).isHovered())
                {
                    ((Button) widget).renderToolTip(poseStack, mouseX, mouseY);
                    break;
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if(ScreenUtil.isMouseWithin(10, 13, 23, 23, (int) mouseX, (int) mouseY))
        {
            Style style = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.curseforge.com/minecraft/mc-mods/configured"));
            this.handleComponentClicked(style);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public static boolean isPlayingGame()
    {
        return Minecraft.getInstance().player != null;
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
            if(this.isMouseOver(mouseX, mouseY) && mouseX < ListMenuScreen.this.list.getRowLeft() + ListMenuScreen.this.list.getRowWidth() - 67)
            {
                Item item = this.getEntryAtPosition(mouseX, mouseY);
                if(item != null)
                {
                    ListMenuScreen.this.setActiveTooltip(item.tooltip);
                }
            }
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

    protected abstract class Item extends ContainerObjectSelectionList.Entry<Item> implements ILabelProvider
    {
        protected final Component label;
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

        public void setTooltip(Component text, int maxWidth)
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
                    output.add(NarratedElementType.TITLE, label);
                }
            });
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

    @OnlyIn(Dist.CLIENT)
    protected class FocusedEditBox extends EditBox
    {
        public FocusedEditBox(Font font, int x, int y, int width, int height, Component label)
        {
            super(font, x, y, width, height, label);
        }

        @Override
        protected void onFocusedChanged(boolean focused)
        {
            super.onFocusedChanged(focused);
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
}
