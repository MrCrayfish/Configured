package com.mrcrayfish.configured.client.screen;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.client.screen.list.IListType;
import com.mrcrayfish.configured.client.screen.list.ListTypes;
import com.mrcrayfish.configured.client.screen.widget.ConfiguredButton;
import com.mrcrayfish.configured.client.screen.widget.IconButton;
import com.mrcrayfish.configured.client.util.ScreenUtil;
import com.mrcrayfish.configured.util.ConfigHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public class EditListScreen<T> extends Screen implements IBackgroundTexture, IEditing
{
    private final Screen parent;
    private final IModConfig config;
    private final List<StringHolder> initialValues = new ArrayList<>();
    private final List<StringHolder> values = new ArrayList<>();
    private final ResourceLocation background;
    private final IConfigValue<List<T>> holder;
    private final IListType<T> listType;
    private ObjectList list;

    public EditListScreen(Screen parent, IModConfig config, Component titleIn, IConfigValue<List<T>> holder, ResourceLocation background)
    {
        super(titleIn);
        this.parent = parent;
        this.config = config;
        this.holder = holder;
        this.listType = ListTypes.getType(holder);
        this.initialValues.addAll(holder.get().stream().map(o -> new StringHolder(this.listType.getStringParser().apply(o))).toList());
        this.values.addAll(this.initialValues);
        this.background = background;
    }

    @Override
    protected void init()
    {
        this.list = new ObjectList();
        this.list.setRenderBackground(!ConfigHelper.isPlayingGame());
        this.addWidget(this.list);
        if(!this.config.isReadOnly())
        {
            this.addRenderableWidget(new IconButton(this.width / 2 - 140, this.height - 29, 0, 44, 90, Component.translatable("configured.gui.apply"), (button) -> {
                List<T> newValues = this.values.stream().map(StringHolder::getValue).map(s -> this.listType.getValueParser().apply(s)).collect(Collectors.toList());
                this.holder.set(newValues);
                this.minecraft.setScreen(this.parent);
            }));
            this.addRenderableWidget(new IconButton(this.width / 2 - 45, this.height - 29, 22, 33, 90, Component.translatable("configured.gui.add_value"), (button) -> {
                this.minecraft.setScreen(new EditStringScreen(EditListScreen.this, this.config, this.background, Component.translatable("configured.gui.edit_value"), "", s -> {
                    T value = this.listType.getValueParser().apply(s);
                    if(value != null)
                    {
                        if(this.holder.isValid(Collections.singletonList(value)))
                        {
                            return Pair.of(true, CommonComponents.EMPTY);
                        }
                        return Pair.of(false, this.holder.getValidationHint());
                    }
                    return Pair.of(false, this.listType.getHint());
                }, s -> {
                    StringHolder holder = new StringHolder(s);
                    this.values.add(holder);
                    this.list.addEntry(new StringEntry(this.list, holder));
                }));
            }));
        }
        boolean readOnly = this.config.isReadOnly();
        int cancelWidth = readOnly ? 150 : 90;
        int cancelOffset = readOnly ? -75 : 50;
        Component cancelLabel = readOnly ? Component.translatable("configured.gui.close") : CommonComponents.GUI_CANCEL;
        this.addRenderableWidget(ScreenUtil.button(this.width / 2 + cancelOffset, this.height - 29, cancelWidth, 20, cancelLabel, (button) ->
        {
            if(this.isModified())
            {
                ConfirmationScreen confirmScreen = new ActiveConfirmationScreen(EditListScreen.this, EditListScreen.this.config, Component.translatable("configured.gui.list_changed"), ConfirmationScreen.Icon.WARNING, result -> {
                    if(!result) return true;
                    this.minecraft.setScreen(this.parent);
                    return false;
                });
                this.minecraft.setScreen(confirmScreen);
            }
            else
            {
                this.minecraft.setScreen(this.parent);
            }
        }));
    }

    @Override
    public void render(GuiGraphics poseStack, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(poseStack);
        this.list.render(poseStack, mouseX, mouseY, partialTicks);
        poseStack.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);
        super.render(poseStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public IModConfig getActiveConfig()
    {
        return this.config;
    }

    @Override
    public ResourceLocation getBackgroundTexture()
    {
        return this.background;
    }

    public boolean isModified()
    {
        if(this.initialValues.size() != this.values.size())
        {
            return true;
        }
        for(int i = 0; i < this.initialValues.size(); i++)
        {
            String s1 = this.initialValues.get(i).getValue();
            String s2 = this.values.get(i).getValue();
            if(!s1.equals(s2))
            {
                return true;
            }
        }
        return false;
    }

    public class ObjectList extends ContainerObjectSelectionList<StringEntry> implements IBackgroundTexture
    {
        public ObjectList()
        {
            super(EditListScreen.this.minecraft, EditListScreen.this.width, EditListScreen.this.height, 36, EditListScreen.this.height - 36, 24);
            EditListScreen.this.values.forEach(value -> {
                this.addEntry(new StringEntry(this, value));
            });
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
        public int addEntry(StringEntry entry)
        {
            return super.addEntry(entry);
        }

        @Override
        public boolean removeEntry(StringEntry entry)
        {
            return super.removeEntry(entry);
        }

        @Override
        public void render(GuiGraphics poseStack, int mouseX, int mouseY, float partialTicks)
        {
            super.render(poseStack, mouseX, mouseY, partialTicks);
            this.children().forEach(entry ->
            {
                entry.children().forEach(o ->
                {
                    if(o instanceof Button)
                    {
                        //TODO invesigate new tooltip system
                        //((Button) o).renderToolTip(poseStack, mouseX, mouseY);
                    }
                });
            });
        }

        @Override
        public ResourceLocation getBackgroundTexture()
        {
            return EditListScreen.this.background;
        }
    }

    public class StringEntry extends ContainerObjectSelectionList.Entry<StringEntry>
    {
        private final StringHolder holder;
        private final ObjectList list;
        private final ConfiguredButton editButton;
        private final ConfiguredButton deleteButton;

        public StringEntry(ObjectList list, StringHolder holder)
        {
            this.list = list;
            this.holder = holder;

            this.editButton = new IconButton(0, 0, 1, 22, 20, CommonComponents.EMPTY, onPress -> {
                EditListScreen.this.minecraft.setScreen(new EditStringScreen(EditListScreen.this, EditListScreen.this.config, EditListScreen.this.background, Component.translatable("configured.gui.edit_value"), this.holder.getValue(), s -> {
                    T value = EditListScreen.this.listType.getValueParser().apply(s);
                    if(value != null)
                    {
                        if(EditListScreen.this.holder.isValid(Collections.singletonList(value)))
                        {
                            return Pair.of(true, CommonComponents.EMPTY);
                        }
                        return Pair.of(false, EditListScreen.this.holder.getValidationHint());
                    }
                    return Pair.of(false, EditListScreen.this.listType.getHint());
                }, this.holder::setValue));
            });
            this.editButton.setTooltip(Tooltip.create(Component.translatable("configured.gui.edit")), btn -> btn.isActive() && btn.isHoveredOrFocused());
            this.editButton.active = !EditListScreen.this.config.isReadOnly();

            this.deleteButton = new IconButton(0, 0, 11, 0, onPress -> {
                EditListScreen.this.values.remove(this.holder);
                this.list.removeEntry(this);
            });
            this.deleteButton.setTooltip(Tooltip.create(Component.translatable("configured.gui.remove")), btn -> btn.isActive() && btn.isHoveredOrFocused());
            this.deleteButton.active = !EditListScreen.this.config.isReadOnly();
        }

        @Override
        public void render(GuiGraphics poseStack, int x, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean selected, float partialTicks)
        {
            if(x % 2 != 0) poseStack.fill(left, top, left + width, top + 24, 0x55000000);
            //TODO check for new
            //EditListScreen.this.minecraft.font.draw(poseStack, Component.literal(this.holder.getValue()), left + 5, top + 8, 0xFFFFFF);
            this.editButton.visible = true;
            this.editButton.setX(left + width - 44);
            this.editButton.setY(top + 2);
            this.editButton.render(poseStack, mouseX, mouseY, partialTicks);
            this.deleteButton.visible = true;
            this.deleteButton.setX(left + width - 22);
            this.deleteButton.setY(top + 2);
            this.deleteButton.render(poseStack, mouseX, mouseY, partialTicks);
        }

        @Override
        public List<? extends GuiEventListener> children()
        {
            return ImmutableList.of(this.editButton, this.deleteButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables()
        {
            return ImmutableList.of(new NarratableEntry()
            {
                public NarrationPriority narrationPriority()
                {
                    return NarrationPriority.HOVERED;
                }

                public void updateNarration(NarrationElementOutput output)
                {
                    output.add(NarratedElementType.TITLE, StringEntry.this.holder.getValue());
                }
            }, StringEntry.this.editButton, StringEntry.this.deleteButton);
        }
    }

    public static class StringHolder
    {
        private String value;

        public StringHolder(String value)
        {
            this.value = value;
        }

        public String getValue()
        {
            return this.value;
        }

        public void setValue(String value)
        {
            this.value = value;
        }
    }
}
