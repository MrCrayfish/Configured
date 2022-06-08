package com.mrcrayfish.configured.client.screen;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.mojang.blaze3d.vertex.PoseStack;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;

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
public class EditListScreen extends Screen implements IBackgroundTexture
{
    private static final Map<ForgeConfigSpec.ConfigValue<?>, ListType> TYPE_CACHE = new HashMap<>();

    private final Screen parent;
    private final List<StringHolder> values = new ArrayList<>();
    private final ForgeConfigSpec.ValueSpec valueSpec;
    private final ResourceLocation background;
    private final ConfigScreen.ListValueHolder holder;
    private final ListType listType;
    private ObjectList list;

    public EditListScreen(Screen parent, Component titleIn, ConfigScreen.ListValueHolder holder, ResourceLocation background)
    {
        super(titleIn);
        this.parent = parent;
        this.holder = holder;
        this.valueSpec = holder.getSpec();
        this.listType = getType(holder);
        this.values.addAll(holder.getValue().stream().map(o -> new StringHolder(this.listType.getStringParser().apply(o))).collect(Collectors.toList()));
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
            this.valueSpec.correct(newValues);
            this.holder.setValue(newValues);
            this.minecraft.setScreen(this.parent);
        }));
        this.addRenderableWidget(new Button(this.width / 2 - 45, this.height - 29, 90, 20, Component.translatable("configured.gui.add_value"), (button) -> {
            this.minecraft.setScreen(new EditStringScreen(EditListScreen.this, background, Component.translatable("configured.gui.edit_value"), "", s -> {
                Object value = this.listType.getValueParser().apply(s);
                return value != null && this.valueSpec.test(Collections.singletonList(value));
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
    public ResourceLocation getBackgroundTexture()
    {
        return this.background;
    }

    @OnlyIn(Dist.CLIENT)
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
            return background;
        }
    }

    public class StringEntry extends ContainerObjectSelectionList.Entry<StringEntry>
    {
        private StringHolder holder;
        private final ObjectList list;
        private final Button editButton;
        private final Button deleteButton;

        public StringEntry(ObjectList list, StringHolder holder)
        {
            this.list = list;
            this.holder = holder;
            this.editButton = new Button(0, 0, 42, 20, Component.literal("Edit"), onPress -> {
                EditListScreen.this.minecraft.setScreen(new EditStringScreen(EditListScreen.this, background, Component.translatable("configured.gui.edit_value"), this.holder.getValue(), s -> {
                    Object value = EditListScreen.this.listType.getValueParser().apply(s);
                    return value != null && EditListScreen.this.valueSpec.test(Collections.singletonList(value));
                }, s -> {
                    this.holder.setValue(s);
                }));
            });
            Button.OnTooltip tooltip = (button, matrixStack, mouseX, mouseY) -> {
                if(button.active && button.isHoveredOrFocused()) {
                    EditListScreen.this.renderTooltip(matrixStack, EditListScreen.this.minecraft.font.split(Component.translatable("configured.gui.remove"), Math.max(EditListScreen.this.width / 2 - 43, 170)), mouseX, mouseY);
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
            EditListScreen.this.minecraft.font.draw(poseStack, Component.literal(this.holder.getValue()), left + 5, top + 6, 0xFFFFFF);
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

    protected static ListType getType(ConfigScreen.ListValueHolder holder)
    {
        return TYPE_CACHE.computeIfAbsent(holder.getConfigValue(), value -> ListType.fromHolder(holder));
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

        protected static ListType fromHolder(ConfigScreen.ListValueHolder holder)
        {
            ListType type = UNKNOWN;
            List<?> defaultList = (List<?>) holder.getSpec().getDefault();
            if(!defaultList.isEmpty())
            {
                type = fromObject(defaultList.get(0));
            }
            if(type == UNKNOWN)
            {
                type = fromElementValidator(holder.getSpec());
            }
            return type;
        }

        protected static ListType fromObject(Object o)
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

        protected static ListType fromList(List<?> list)
        {
            if(!list.isEmpty())
            {
                return fromObject(list.get(0));
            }
            return UNKNOWN;
        }

        /**
         * Attempts to determine the type of list from the element validator. This currently
         * used as a last resort since validation may fail even though it's the correct type.
         * It may also return the incorrect type if the validator accepts everything.
         */
        private static ListType fromElementValidator(ForgeConfigSpec.ValueSpec spec)
        {
            if(spec.test(Collections.singletonList("s")))
                return STRING;
            if(spec.test(Collections.singletonList(true)))
                return BOOLEAN;
            if(spec.test(Collections.singletonList(0.0D)))
                return DOUBLE;
            if(spec.test(Collections.singletonList(0L)))
                return LONG;
            if(spec.test(Collections.singletonList(0)))
                return INTEGER;
            return UNKNOWN;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static boolean testAddValue(List list, Object v)
        {
            try
            {
                list.add(v);
                list.clear();
                return true;
            }
            catch(ClassCastException e)
            {
                return false;
            }
        }
    }
}
