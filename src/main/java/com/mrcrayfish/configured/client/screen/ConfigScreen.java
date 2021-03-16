package com.mrcrayfish.configured.client.screen;

import com.electronwill.nightconfig.core.AbstractConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import joptsimple.internal.Strings;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.list.AbstractOptionList;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
@OnlyIn(Dist.CLIENT)
public class ConfigScreen extends Screen
{
    public static final Comparator<Entry> COMPARATOR = (o1, o2) ->
    {
        if(o1 instanceof SubMenu && o2 instanceof SubMenu)
        {
            return o1.getLabel().compareTo(o2.getLabel());
        }
        if(!(o1 instanceof SubMenu) && o2 instanceof SubMenu)
        {
            return 1;
        }
        return -1;
    };

    private final Screen parent;
    private final String displayName;
    private final ForgeConfigSpec clientSpec;
    private final UnmodifiableConfig clientValues;
    private final ForgeConfigSpec commonSpec;
    private final UnmodifiableConfig commonValues;
    private ConfigList list;
    private List<Entry> entries;
    private TextFieldWidget searchTextField;
    private boolean subMenu = false;

    public ConfigScreen(Screen parent, String displayName, ForgeConfigSpec spec, UnmodifiableConfig values)
    {
        super(new StringTextComponent(displayName));
        this.parent = parent;
        this.displayName = displayName;
        this.clientSpec = spec;
        this.clientValues = values;
        this.commonSpec = null;
        this.commonValues = null;
        this.subMenu = true;
        this.constructEntries();
    }

    public ConfigScreen(Screen parent, String displayName, @Nullable ForgeConfigSpec clientSpec, @Nullable UnmodifiableConfig clientValues, @Nullable ForgeConfigSpec commonSpec, @Nullable UnmodifiableConfig commonValues)
    {
        super(new StringTextComponent(displayName));
        this.parent = parent;
        this.displayName = displayName;
        this.clientSpec = clientSpec;
        this.clientValues = clientValues;
        this.commonSpec = commonSpec;
        this.commonValues = commonValues;
        this.constructEntries();
    }

    private void constructEntries()
    {
        List<Entry> entries = new ArrayList<>();
        if(this.clientValues != null && this.clientSpec != null)
        {
            if(!this.subMenu) entries.add(new TitleEntry("Client Configuration"));
            this.addEntries(this.clientValues, this.clientSpec, entries);
        }
        if(this.commonValues != null && this.commonSpec != null)
        {
            entries.add(new TitleEntry("Common Configuration"));
            this.addEntries(this.commonValues, this.commonSpec, entries);
        }
        this.entries = ImmutableList.copyOf(entries);
    }

    private void addEntries(UnmodifiableConfig values, ForgeConfigSpec spec, List<Entry> entries)
    {
        List<Entry> subEntries = new ArrayList<>();
        values.valueMap().forEach((s, o) ->
        {
            if(o instanceof AbstractConfig)
            {
                subEntries.add(new SubMenu(s, spec, (AbstractConfig) o));
            }
            else if(o instanceof ForgeConfigSpec.ConfigValue<?>)
            {
                ForgeConfigSpec.ConfigValue<?> configValue = (ForgeConfigSpec.ConfigValue<?>) o;
                ForgeConfigSpec.ValueSpec valueSpec = spec.getRaw(configValue.getPath());
                if(configValue instanceof ForgeConfigSpec.BooleanValue)
                {
                    subEntries.add(new BooleanEntry((ForgeConfigSpec.BooleanValue) configValue, valueSpec));
                }
                else if(configValue instanceof ForgeConfigSpec.IntValue)
                {
                    subEntries.add(new IntegerEntry((ForgeConfigSpec.IntValue) configValue, valueSpec));
                }
                else if(configValue instanceof ForgeConfigSpec.DoubleValue)
                {
                    subEntries.add(new DoubleEntry((ForgeConfigSpec.DoubleValue) configValue, valueSpec));
                }
                else if(configValue instanceof ForgeConfigSpec.LongValue)
                {
                    subEntries.add(new LongEntry((ForgeConfigSpec.LongValue) configValue, valueSpec));
                }
                else if(configValue instanceof ForgeConfigSpec.EnumValue)
                {
                    subEntries.add(new EnumEntry((ForgeConfigSpec.EnumValue) configValue, valueSpec));
                }
                else
                {
                    subEntries.add(new StringEntry(configValue, valueSpec));
                }
            }
        });
        subEntries.sort(COMPARATOR);
        entries.addAll(subEntries);
    }

    @Override
    protected void init()
    {
        this.list = new ConfigList(this.entries);
        this.children.add(this.list);

        this.searchTextField = new TextFieldWidget(this.font, this.width / 2 - 110, 22, 220, 20, new StringTextComponent("Search"));
        //It's broken
        //this.searchTextField.setSuggestion(new TranslationTextComponent("configured.gui.search").getString());
        this.searchTextField.setResponder(s ->
        {
            if(!s.isEmpty())
            {
                this.list.replaceEntries(this.entries.stream().filter(entry -> (entry instanceof SubMenu || entry instanceof ConfigEntry<?>) && entry.getLabel().toLowerCase(Locale.ENGLISH).contains(s.toLowerCase(Locale.ENGLISH))).collect(Collectors.toList()));
            }
            else
            {
                this.list.replaceEntries(this.entries);
            }
        });
        this.children.add(this.searchTextField);

        if(this.subMenu)
        {
            this.addButton(new Button(this.width / 2 - 75, this.height - 29, 150, 20, DialogTexts.GUI_BACK, (button) -> {
                this.minecraft.displayGuiScreen(this.parent);
            }));
        }
        else
        {
            this.addButton(new Button(this.width / 2 - 155 + 160, this.height - 29, 150, 20, DialogTexts.GUI_DONE, (button) -> {
                this.clientSpec.save();
                this.minecraft.displayGuiScreen(this.parent);
            }));
            this.addButton(new Button(this.width / 2 - 155, this.height - 29, 150, 20, new TranslationTextComponent("configured.gui.restore_defaults"), (button) -> {
                //TODO
            }));
        }
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(matrixStack);
        this.list.render(matrixStack, mouseX, mouseY, partialTicks);
        this.searchTextField.render(matrixStack, mouseX, mouseY, partialTicks);
        drawCenteredString(matrixStack, this.font, this.title, this.width / 2, 7, 0xFFFFFF);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    public abstract class Entry extends AbstractOptionList.Entry<Entry>
    {
        protected String label;

        public Entry(String label)
        {
            this.label = label;
        }

        public String getLabel()
        {
            return this.label;
        }
    }

    public class TitleEntry extends Entry
    {
        public TitleEntry(String title)
        {
            super(title);
        }

        @Override
        public List<? extends IGuiEventListener> getEventListeners()
        {
            return Collections.emptyList();
        }

        @Override
        public void render(MatrixStack matrixStack, int x, int top, int left, int width, int p_230432_6_, int p_230432_7_, int p_230432_8_, boolean p_230432_9_, float p_230432_10_)
        {
            ITextComponent title = new StringTextComponent(this.label).mergeStyle(TextFormatting.BOLD).mergeStyle(TextFormatting.YELLOW);
            AbstractGui.drawCenteredString(matrixStack, ConfigScreen.this.minecraft.fontRenderer, title, left + width / 2, top + 5, 16777215);
        }
    }

    public class SubMenu extends Entry
    {
        private final Button button;

        public SubMenu(String label, ForgeConfigSpec spec, AbstractConfig values)
        {
            super(label);
            String title = StringUtils.capitalize(label);
            this.button = new Button(10, 5, 44, 20, new StringTextComponent(title).mergeStyle(TextFormatting.BOLD).mergeStyle(TextFormatting.WHITE), onPress -> {
                String newTitle = ConfigScreen.this.displayName + " > " + title;
                ConfigScreen.this.minecraft.displayGuiScreen(new ConfigScreen(ConfigScreen.this, newTitle, spec, values));
            });
        }

        @Override
        public List<? extends IGuiEventListener> getEventListeners()
        {
            return ImmutableList.of(this.button);
        }

        @Override
        public void render(MatrixStack matrixStack, int x, int top, int left, int width, int height, int mouseX, int mouseY, boolean selected, float partialTicks)
        {
            this.button.x = left - 1;
            this.button.y = top;
            this.button.setWidth(width);
            this.button.render(matrixStack, mouseX, mouseY, partialTicks);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public abstract class ConfigEntry<T extends ForgeConfigSpec.ConfigValue<?>> extends Entry
    {
        protected T configValue;
        protected ForgeConfigSpec.ValueSpec valueSpec;
        private List<IReorderingProcessor> tooltip;
        protected final List<IGuiEventListener> eventListeners = Lists.newArrayList();

        public ConfigEntry(T configValue, ForgeConfigSpec.ValueSpec valueSpec)
        {
            super(createLabelFromConfig(configValue, valueSpec));
            this.configValue = configValue;
            this.valueSpec = valueSpec;
        }

        public ConfigEntry setTooltip(List<IReorderingProcessor> tooltip)
        {
            this.tooltip = tooltip;
            return this;
        }

        @Override
        public List<? extends IGuiEventListener> getEventListeners()
        {
            return this.eventListeners;
        }

        @Override
        public void render(MatrixStack matrixStack, int x, int y, int left, int width, int p_230432_6_, int p_230432_7_, int p_230432_8_, boolean p_230432_9_, float p_230432_10_)
        {
            ITextComponent title = new StringTextComponent(this.label);
            if(ConfigScreen.this.minecraft.fontRenderer.getStringPropertyWidth(title) > width - 50)
            {
                String trimmed = ConfigScreen.this.minecraft.fontRenderer.func_238417_a_(title, width - 50).getString() + "...";
                ConfigScreen.this.minecraft.fontRenderer.func_243246_a(matrixStack, new StringTextComponent(trimmed), left, y + 6, 0xFFFFFF);
            }
            else
            {
                ConfigScreen.this.minecraft.fontRenderer.func_243246_a(matrixStack, title, left, y + 6, 0xFFFFFF);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public class ConfigList extends AbstractOptionList<ConfigScreen.Entry>
    {
        public ConfigList(List<ConfigScreen.Entry> entries)
        {
            super(ConfigScreen.this.minecraft, ConfigScreen.this.width, ConfigScreen.this.height, 50, ConfigScreen.this.height - 36, 24);
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
        public void replaceEntries(Collection<ConfigScreen.Entry> entries)
        {
            super.replaceEntries(entries);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public class IntegerEntry extends ConfigEntry<ForgeConfigSpec.IntValue>
    {
        public IntegerEntry(ForgeConfigSpec.IntValue configValue, ForgeConfigSpec.ValueSpec valueSpec)
        {
            super(configValue, valueSpec);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public class DoubleEntry extends ConfigEntry<ForgeConfigSpec.DoubleValue>
    {
        public DoubleEntry(ForgeConfigSpec.DoubleValue configValue, ForgeConfigSpec.ValueSpec valueSpec)
        {
            super(configValue, valueSpec);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public class LongEntry extends ConfigEntry<ForgeConfigSpec.LongValue>
    {
        public LongEntry(ForgeConfigSpec.LongValue configValue, ForgeConfigSpec.ValueSpec valueSpec)
        {
            super(configValue, valueSpec);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public class BooleanEntry extends ConfigEntry<ForgeConfigSpec.BooleanValue>
    {
        private final Button button;

        public BooleanEntry(ForgeConfigSpec.BooleanValue configValue, ForgeConfigSpec.ValueSpec valueSpec)
        {
            super(configValue, valueSpec);
            this.button = new Button(10, 5, 44, 20, DialogTexts.optionsEnabled(configValue.get()), (button) -> {
                boolean flag = !configValue.get();
                configValue.set(flag);
                button.setMessage(DialogTexts.optionsEnabled(configValue.get()));
            }) {
                @Override
                protected IFormattableTextComponent getNarrationMessage()
                {
                    return DialogTexts.getComposedOptionMessage(new StringTextComponent(lastValue(configValue.getPath(), "Option")), configValue.get());
                }
            };
            this.eventListeners.add(this.button);
        }

        @Override
        public void render(MatrixStack matrixStack, int index, int top, int left, int width, int p_230432_6_, int p_230432_7_, int p_230432_8_, boolean selected, float partialTicks)
        {
            super.render(matrixStack, index, top, left, width, p_230432_6_, p_230432_7_, p_230432_8_, selected, partialTicks);
            this.button.x = left + width - 45;
            this.button.y = top;
            this.button.render(matrixStack, p_230432_7_, p_230432_8_, partialTicks);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public class StringEntry extends ConfigEntry<ForgeConfigSpec.ConfigValue<?>>
    {
        public StringEntry(ForgeConfigSpec.ConfigValue<?> configValue, ForgeConfigSpec.ValueSpec valueSpec)
        {
            super(configValue, valueSpec);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public class EnumEntry extends ConfigEntry<ForgeConfigSpec.EnumValue<?>>
    {
        public EnumEntry(ForgeConfigSpec.EnumValue<?> configValue, ForgeConfigSpec.ValueSpec valueSpec)
        {
            super(configValue, valueSpec);
        }
    }

    private static <V> V lastValue(List<V> list, V defaultValue)
    {
        if(list.size() > 0)
        {
            return list.get(list.size() - 1);
        }
        return defaultValue;
    }

    private static String createLabelFromConfig(ForgeConfigSpec.ConfigValue<?> configValue, ForgeConfigSpec.ValueSpec valueSpec)
    {
        if(valueSpec.getTranslationKey() != null)
        {
            return new TranslationTextComponent(valueSpec.getTranslationKey()).getString();
        }
        String valueName = lastValue(configValue.getPath(), "");
        String[] words = valueName.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
        for(int i = 0; i < words.length; i++) words[i] = StringUtils.capitalize(words[i]);
        return Strings.join(words, " ");
    }
}
