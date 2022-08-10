package com.mrcrayfish.configured.client.screen;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.client.screen.widget.IconButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public class EditListScreen extends Screen implements IBackgroundTexture, IEditing
{
    private static final Map<IConfigValue<List<?>>, ListType> TYPE_CACHE = new HashMap<>();

    private final Screen parent;
    private final IModConfig config;
    private final List<StringHolder> values = new ArrayList<>();
    private final ResourceLocation background;
    private final IConfigValue<List<?>> holder;
    private final ListType listType;
    private ObjectList list;

    public EditListScreen(Screen parent, IModConfig config, Component titleIn, IConfigValue<List<?>> holder, ResourceLocation background)
    {
        super(titleIn);
        this.parent = parent;
        this.config = config;
        this.holder = holder;
        this.listType = getType(holder);
        this.values.addAll(holder.get().stream().map(o -> new StringHolder(this.listType.getStringParser().apply(o))).toList());
        this.background = background;
    }

    @Override
    protected void init()
    {
        this.list = new ObjectList();
        this.list.setRenderBackground(!ListMenuScreen.isPlayingGame());
        this.addWidget(this.list);
        this.addRenderableWidget(new Button(this.width / 2 - 140, this.height - 29, 90, 20, CommonComponents.GUI_DONE, (button) -> {
            List<?> newValues = this.values.stream().map(StringHolder::getValue).map(s -> this.listType.getValueParser().apply(s)).collect(Collectors.toList());
            this.holder.set(newValues);
            this.minecraft.setScreen(this.parent);
        }));
        this.addRenderableWidget(new Button(this.width / 2 - 45, this.height - 29, 90, 20, new TranslatableComponent("configured.gui.add_value"), (button) -> {
            this.minecraft.setScreen(new EditStringScreen(EditListScreen.this, this.background, new TranslatableComponent("configured.gui.edit_value"), "", s -> {
                Object value = this.listType.getValueParser().apply(s);
                return value != null && this.holder.isValid(Collections.singletonList(value));
            }, s -> {
                StringHolder holder = new StringHolder(s);
                this.values.add(holder);
                this.list.addEntry(new StringEntry(this.list, holder));
            }));
        }));
        this.addRenderableWidget(new Button(this.width / 2 + 50, this.height - 29, 90, 20, CommonComponents.GUI_CANCEL, (button) -> {
            this.minecraft.setScreen(this.parent);
        }));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(poseStack);
        this.list.render(poseStack, mouseX, mouseY, partialTicks);
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 14, 0xFFFFFF);
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
        public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
        {
            super.render(poseStack, mouseX, mouseY, partialTicks);
            this.children().forEach(entry ->
            {
                entry.children().forEach(o ->
                {
                    if(o instanceof Button)
                    {
                        ((Button) o).renderToolTip(poseStack, mouseX, mouseY);
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
        private final Button editButton;
        private final Button deleteButton;

        public StringEntry(ObjectList list, StringHolder holder)
        {
            this.list = list;
            this.holder = holder;
            this.editButton = new Button(0, 0, 42, 20, new TextComponent("Edit"), onPress -> {
                EditListScreen.this.minecraft.setScreen(new EditStringScreen(EditListScreen.this, EditListScreen.this.background, new TranslatableComponent("configured.gui.edit_value"), this.holder.getValue(), s -> {
                    Object value = EditListScreen.this.listType.getValueParser().apply(s);
                    return value != null && EditListScreen.this.holder.isValid(Collections.singletonList(value));
                }, this.holder::setValue));
            });
            Button.OnTooltip tooltip = (button, matrixStack, mouseX, mouseY) -> {
                if(button.active && button.isHoveredOrFocused()) {
                    EditListScreen.this.renderTooltip(matrixStack, EditListScreen.this.minecraft.font.split(new TranslatableComponent("configured.gui.remove"), Math.max(EditListScreen.this.width / 2 - 43, 170)), mouseX, mouseY);
                }
            };
            this.deleteButton = new IconButton(0, 0, 11, 0, onPress -> {
                EditListScreen.this.values.remove(this.holder);
                this.list.removeEntry(this);
            }, tooltip);
        }

        @Override
        public void render(PoseStack poseStack, int x, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean selected, float partialTicks)
        {
            EditListScreen.this.minecraft.font.draw(poseStack, new TextComponent(this.holder.getValue()), left + 5, top + 6, 0xFFFFFF);
            this.editButton.visible = true;
            this.editButton.x = left + width - 65;
            this.editButton.y = top;
            this.editButton.render(poseStack, mouseX, mouseY, partialTicks);
            this.deleteButton.visible = true;
            this.deleteButton.x = left + width - 21;
            this.deleteButton.y = top;
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
                public NarratableEntry.NarrationPriority narrationPriority()
                {
                    return NarratableEntry.NarrationPriority.HOVERED;
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

    protected static ListType getType(IConfigValue<List<?>> holder)
    {
        return TYPE_CACHE.computeIfAbsent(holder, value -> ListType.fromHolder(holder));
    }

    protected enum ListType
    {
        BOOLEAN(Object::toString, Boolean::valueOf),
        INTEGER(Object::toString, Ints::tryParse),
        LONG(Object::toString, Longs::tryParse),
        DOUBLE(Object::toString, Doubles::tryParse),
        STRING(Object::toString, o -> o),
        UNKNOWN(Object::toString, o -> o);

        final Function<Object, String> stringParser;
        final Function<String, ?> valueParser;

        ListType(Function<Object, String> stringParser, Function<String, ?> valueParser)
        {
            this.stringParser = stringParser;
            this.valueParser = valueParser;
        }

        public Function<Object, String> getStringParser()
        {
            return this.stringParser;
        }

        public Function<String, ?> getValueParser()
        {
            return this.valueParser;
        }

        private static ListType fromHolder(IConfigValue<List<?>> holder)
        {
            ListType type = UNKNOWN;
            List<?> defaultList = holder.getDefault();
            if(!defaultList.isEmpty())
            {
                type = fromObject(defaultList.get(0));
            }
            if(type == UNKNOWN)
            {
                type = fromElementValidator(holder);
            }
            return type;
        }

        private static ListType fromObject(Object o)
        {
            if(o instanceof Boolean)
            {
                return BOOLEAN;
            }
            else if(o instanceof Integer)
            {
                return INTEGER;
            }
            else if(o instanceof Long)
            {
                return LONG;
            }
            else if(o instanceof Double)
            {
                return DOUBLE;
            }
            else if(o instanceof String)
            {
                return STRING;
            }
            return UNKNOWN;
        }

        /**
         * Attempts to determine the type of list from the element validator. This currently
         * used as a last resort since validation may fail even though it's the correct type.
         * It may also return the incorrect type if the validator accepts everything.
         */
        private static ListType fromElementValidator(IConfigValue<List<?>> spec)
        {
            if(spec.isValid(Collections.singletonList("s")))
                return STRING;
            if(spec.isValid(Collections.singletonList(true)))
                return BOOLEAN;
            if(spec.isValid(Collections.singletonList(0.0D)))
                return DOUBLE;
            if(spec.isValid(Collections.singletonList(0L)))
                return LONG;
            if(spec.isValid(Collections.singletonList(0)))
                return INTEGER;
            return UNKNOWN;
        }
    }
}
