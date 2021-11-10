package com.mrcrayfish.configured.client.screen;

import com.electronwill.nightconfig.core.AbstractConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.client.screen.widget.IconButton;
import com.mrcrayfish.configured.client.util.ScreenUtil;
import com.mrcrayfish.configured.util.ConfigHelper;
import joptsimple.internal.Strings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.LanguageMap;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Author: MrCrayfish
 */
@OnlyIn(Dist.CLIENT)
public class ConfigScreen extends ListMenuScreen
{
    public static final Comparator<Entry> SORT_ALPHABETICALLY = (o1, o2) ->
    {
        if(o1 instanceof SubMenu && o2 instanceof SubMenu)
        {
            return o1.getLabel().compareTo(o2.getLabel());
        }
        if(!(o1 instanceof SubMenu) && o2 instanceof SubMenu)
        {
            return 1;
        }
        if(o1 instanceof SubMenu)
        {
            return -1;
        }
        return o1.getLabel().compareTo(o2.getLabel());
    };

    @Nullable
    protected final ConfigFileEntry configFileEntry;
    protected ModConfig config;
    protected Button restoreButton;

    protected ConfigScreen(Screen parent, ITextComponent title, ResourceLocation background, @Nullable ConfigFileEntry configFileEntry)
    {
        super(parent, title, background, 24);
        this.configFileEntry = configFileEntry;
    }

    public ConfigScreen(Screen parent, ITextComponent title, @Nullable ConfigFileEntry entry, ResourceLocation background)
    {
        this(parent, title, background, entry);
    }

    public ConfigScreen(Screen parent, ITextComponent title, ModConfig config, ResourceLocation background)
    {
        this(parent, title, background, new ConfigFileEntry(config.getSpec(), config.getSpec().getValues()));
        this.config = config;
    }

    @Override
    protected void constructEntries(List<Entry> entries)
    {
        if(this.configFileEntry != null)
        {
            List<Entry> configEntries = new ArrayList<>();
            this.createEntriesFromConfig(this.configFileEntry.config, this.configFileEntry.spec, configEntries);
            configEntries.sort(SORT_ALPHABETICALLY);
            entries.addAll(configEntries);
        }
    }

    /**
     * Scans the given unmodifiable config and creates an entry for each scanned
     * config value based on it's type.
     *
     * @param values  the values to scan
     * @param spec    the spec of config
     * @param entries the list to add the entries to
     */
    private void createEntriesFromConfig(UnmodifiableConfig values, ForgeConfigSpec spec, List<Entry> entries)
    {
        values.valueMap().forEach((s, o) ->
        {
            if(o instanceof AbstractConfig)
            {
                entries.add(new SubMenu(s, spec, (AbstractConfig) o));
            }
            else if(o instanceof ForgeConfigSpec.ConfigValue<?>)
            {
                ForgeConfigSpec.ConfigValue<?> configValue = (ForgeConfigSpec.ConfigValue<?>) o;
                ForgeConfigSpec.ValueSpec valueSpec = spec.getRaw(configValue.getPath());
                Object value = configValue.get();
                if(value instanceof Boolean)
                {
                    entries.add(new BooleanEntry((ForgeConfigSpec.ConfigValue<Boolean>) configValue, valueSpec));
                }
                else if(value instanceof Integer)
                {
                    entries.add(new IntegerEntry((ForgeConfigSpec.ConfigValue<Integer>) configValue, valueSpec));
                }
                else if(value instanceof Double)
                {
                    entries.add(new DoubleEntry((ForgeConfigSpec.ConfigValue<Double>) configValue, valueSpec));
                }
                else if(value instanceof Long)
                {
                    entries.add(new LongEntry((ForgeConfigSpec.ConfigValue<Long>) configValue, valueSpec));
                }
                else if(value instanceof Enum)
                {
                    entries.add(new EnumEntry((ForgeConfigSpec.ConfigValue<Enum>) configValue, valueSpec));
                }
                else if(value instanceof String)
                {
                    entries.add(new StringEntry((ForgeConfigSpec.ConfigValue<String>) configValue, valueSpec));
                }
                else if(value instanceof List<?>)
                {
                    entries.add(new ListStringEntry((ForgeConfigSpec.ConfigValue<List<?>>) configValue, valueSpec));
                }
                else
                {
                    Configured.LOGGER.info("Unsupported config value: " + configValue.getPath());
                }
            }
        });
    }

    @Override
    protected void init()
    {
        super.init();

        if(!(this.parent instanceof ConfigScreen))
        {
            this.restoreButton = this.addButton(new Button(this.width / 2 - 155, this.height - 29, 150, 20, new TranslationTextComponent("configured.gui.restore_defaults"), (button) ->
            {
                if(this.config != null)
                {
                    this.showRestoreScreen();
                }
            }));
            this.updateRestoreButton();

            this.addButton(new Button(this.width / 2 - 155 + 160, this.height - 29, 150, 20, DialogTexts.GUI_DONE, (button) ->
            {
                this.minecraft.displayGuiScreen(this.parent);

                if(this.config == null || this.config.getType() != ModConfig.Type.SERVER)
                    return;

                if(!ListMenuScreen.isPlayingGame())
                {
                    // Unload server configs since still in main menu
                    this.config.getHandler().unload(this.config.getFullPath().getParent(), this.config);
                    ConfigHelper.setConfigData(this.config, null);
                }
                else
                {
                    ConfigHelper.sendConfigDataToServer(this.config);
                    ConfigHelper.resetCache(this.config); //TODO probably don't need
                }
            }));
        }
        else
        {
            this.addButton(new Button(this.width / 2 - 75, this.height - 29, 150, 20, DialogTexts.GUI_BACK, button -> this.minecraft.displayGuiScreen(this.parent)));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void showRestoreScreen()
    {
        ConfirmationScreen confirmScreen = new ConfirmationScreen(ConfigScreen.this, new TranslationTextComponent("configured.gui.restore_message"), result ->
        {
            if(!result || this.config == null)
                return;
            // Resets all config values
            ConfigHelper.gatherAllConfigValues(this.config).forEach(pair ->
            {
                ForgeConfigSpec.ConfigValue configValue = pair.getLeft();
                ForgeConfigSpec.ValueSpec valueSpec = pair.getRight();
                configValue.set(valueSpec.getDefault());
            });
            ConfigHelper.fireEvent(this.config, ConfigHelper.reloadingEvent());
            this.updateRestoreButton();
        });
        confirmScreen.setBackground(background);
        confirmScreen.setPositiveText(new TranslationTextComponent("configured.gui.restore").mergeStyle(TextFormatting.GOLD, TextFormatting.BOLD));
        confirmScreen.setNegativeText(DialogTexts.GUI_CANCEL);
        Minecraft.getInstance().displayGuiScreen(confirmScreen);
    }

    private void updateRestoreButton()
    {
        if(this.config != null && this.restoreButton != null)
        {
            this.restoreButton.active = ConfigHelper.isModified(this.config);
        }
    }

    protected Predicate<Entry> getSearchFilter(String s)
    {
        return entry -> !(entry instanceof TitleEntry) && entry.getLabel().toLowerCase(Locale.ENGLISH).contains(s.toLowerCase(Locale.ENGLISH));
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        this.activeTooltip = null;
        this.renderBackground(matrixStack);
        this.list.render(matrixStack, mouseX, mouseY, partialTicks);
        this.searchTextField.render(matrixStack, mouseX, mouseY, partialTicks);
        drawCenteredString(matrixStack, this.font, this.title, this.width / 2, 7, 0xFFFFFF);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    public class SubMenu extends Entry
    {
        private final Button button;

        public SubMenu(String label, ForgeConfigSpec spec, AbstractConfig values)
        {
            super(new StringTextComponent(createLabel(label)));
            this.button = new Button(10, 5, 44, 20, new StringTextComponent(this.getLabel()).mergeStyle(TextFormatting.BOLD).mergeStyle(TextFormatting.WHITE), onPress -> {
                ITextComponent newTitle = ConfigScreen.this.title.copyRaw().appendString(" > " + this.getLabel());
                ConfigScreen.this.minecraft.displayGuiScreen(new ConfigScreen(ConfigScreen.this, newTitle, new ConfigFileEntry(spec, values), background));
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

    public abstract class ConfigEntry<T extends ForgeConfigSpec.ConfigValue> extends Entry
    {
        protected final T configValue;
        protected final ForgeConfigSpec.ValueSpec valueSpec;
        protected final List<IGuiEventListener> eventListeners = Lists.newArrayList();
        protected final Button resetButton;

        @SuppressWarnings("unchecked")
        public ConfigEntry(T configValue, ForgeConfigSpec.ValueSpec valueSpec)
        {
            super(createLabelFromConfig(configValue, valueSpec));
            this.configValue = configValue;
            this.valueSpec = valueSpec;
            if(valueSpec.getComment() != null)
            {
                this.tooltip = this.createToolTip(configValue, valueSpec);
            }
            int maxTooltipWidth = Math.max(ConfigScreen.this.width / 2 - 43, 170);
            Button.ITooltip tooltip = ScreenUtil.createButtonTooltip(ConfigScreen.this, new TranslationTextComponent("configured.gui.reset"), maxTooltipWidth);
            this.resetButton = new IconButton(0, 0, 20, 20, 0, 0, onPress -> {
                configValue.set(valueSpec.getDefault());
                this.onResetValue();
            }, tooltip);
            this.eventListeners.add(this.resetButton);
        }

        public void onResetValue() {}

        @Override
        public List<? extends IGuiEventListener> getEventListeners()
        {
            return this.eventListeners;
        }

        @Override
        public void render(MatrixStack matrixStack, int x, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            Minecraft.getInstance().fontRenderer.func_243246_a(matrixStack, this.getTrimmedLabel(width - 75), left, top + 6, 0xFFFFFF);

            if(this.isMouseOver(mouseX, mouseY) && mouseX < ConfigScreen.this.list.getRowLeft() + ConfigScreen.this.list.getRowWidth() - 67)
            {
                ConfigScreen.this.setActiveTooltip(this.tooltip);
            }

            this.resetButton.active = !this.configValue.get().equals(this.valueSpec.getDefault());
            this.resetButton.x = left + width - 21;
            this.resetButton.y = top;
            this.resetButton.render(matrixStack, mouseX, mouseY, partialTicks);
        }

        private ITextComponent getTrimmedLabel(int maxWidth)
        {
            if(ConfigScreen.this.minecraft.fontRenderer.getStringPropertyWidth(this.label) > maxWidth)
            {
                return new StringTextComponent(ConfigScreen.this.minecraft.fontRenderer.func_238417_a_(this.label, maxWidth).getString() + "...");
            }
            return this.label;
        }

        private List<IReorderingProcessor> createToolTip(ForgeConfigSpec.ConfigValue<?> value, ForgeConfigSpec.ValueSpec spec)
        {
            FontRenderer font = Minecraft.getInstance().fontRenderer;
            List<ITextProperties> lines = font.getCharacterManager().func_238362_b_(new StringTextComponent(spec.getComment()), 200, Style.EMPTY);
            String name = lastValue(value.getPath(), "");
            lines.add(0, new StringTextComponent(name).mergeStyle(TextFormatting.YELLOW));
            int rangeIndex = -1;
            for(int i = 0; i < lines.size(); i++)
            {
                String text = lines.get(i).getString();
                if(text.startsWith("Range: ") || text.startsWith("Allowed Values: "))
                {
                    rangeIndex = i;
                    break;
                }
            }
            if(rangeIndex != -1)
            {
                for(int i = rangeIndex; i < lines.size(); i++)
                {
                    lines.set(i, new StringTextComponent(lines.get(i).getString()).mergeStyle(TextFormatting.GRAY));
                }
            }
            return LanguageMap.getInstance().func_244260_a(lines);
        }
    }

    public abstract class NumberEntry<T extends ForgeConfigSpec.ConfigValue> extends ConfigEntry<T>
    {
        private final FocusedTextFieldWidget textField;

        @SuppressWarnings("unchecked")
        public NumberEntry(T configValue, ForgeConfigSpec.ValueSpec valueSpec, Function<String, Number> parser)
        {
            super(configValue, valueSpec);
            this.textField = new FocusedTextFieldWidget(ConfigScreen.this.font, 0, 0, 44, 18, this.label);
            this.textField.setText(configValue.get().toString());
            this.textField.setResponder((s) ->
            {
                try
                {
                    Number n = parser.apply(s);
                    if(valueSpec.test(n))
                    {
                        this.textField.setTextColor(14737632);
                        configValue.set(n);
                    }
                    else
                    {
                        this.textField.setTextColor(16711680);
                    }
                }
                catch(Exception ignored)
                {
                    this.textField.setTextColor(16711680);
                }
            });
            this.eventListeners.add(this.textField);
        }

        @Override
        public void render(MatrixStack matrixStack, int index, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            super.render(matrixStack, index, top, left, width, p_230432_6_, mouseX, mouseY, hovered, partialTicks);
            this.textField.x = left + width - 68;
            this.textField.y = top + 1;
            this.textField.render(matrixStack, mouseX, mouseY, partialTicks);
        }

        @Override
        public void onResetValue()
        {
            this.textField.setText(this.configValue.get().toString());
        }
    }

    public class IntegerEntry extends NumberEntry<ForgeConfigSpec.ConfigValue<Integer>>
    {
        public IntegerEntry(ForgeConfigSpec.ConfigValue<Integer> configValue, ForgeConfigSpec.ValueSpec valueSpec)
        {
            super(configValue, valueSpec, Integer::parseInt);
        }
    }

    public class DoubleEntry extends NumberEntry<ForgeConfigSpec.ConfigValue<Double>>
    {
        public DoubleEntry(ForgeConfigSpec.ConfigValue<Double> configValue, ForgeConfigSpec.ValueSpec valueSpec)
        {
            super(configValue, valueSpec, Double::parseDouble);
        }
    }

    public class LongEntry extends NumberEntry<ForgeConfigSpec.ConfigValue<Long>>
    {
        public LongEntry(ForgeConfigSpec.ConfigValue<Long> configValue, ForgeConfigSpec.ValueSpec valueSpec)
        {
            super(configValue, valueSpec, Long::parseLong);
        }
    }

    public class BooleanEntry extends ConfigEntry<ForgeConfigSpec.ConfigValue<Boolean>>
    {
        private final Button button;

        public BooleanEntry(ForgeConfigSpec.ConfigValue<Boolean> configValue, ForgeConfigSpec.ValueSpec valueSpec)
        {
            super(configValue, valueSpec);
            this.button = new Button(10, 5, 46, 20, DialogTexts.optionsEnabled(configValue.get()), button ->
            {
                boolean flag = !configValue.get();
                configValue.set(flag);
                button.setMessage(DialogTexts.optionsEnabled(configValue.get()));
            });
            this.eventListeners.add(this.button);
        }

        @Override
        public void render(MatrixStack matrixStack, int index, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            super.render(matrixStack, index, top, left, width, p_230432_6_, mouseX, mouseY, hovered, partialTicks);
            this.button.x = left + width - 69;
            this.button.y = top;
            this.button.render(matrixStack, mouseX, mouseY, partialTicks);
        }

        @Override
        public void onResetValue()
        {
            this.button.setMessage(DialogTexts.optionsEnabled(this.configValue.get()));
        }
    }

    public class StringEntry extends ConfigEntry<ForgeConfigSpec.ConfigValue<String>>
    {
        private final Button button;

        public StringEntry(ForgeConfigSpec.ConfigValue<String> configValue, ForgeConfigSpec.ValueSpec valueSpec)
        {
            super(configValue, valueSpec);
            String title = createLabelFromConfig(configValue, valueSpec);
            this.button = new Button(10, 5, 46, 20, new TranslationTextComponent("configured.gui.edit"), button -> Minecraft.getInstance().displayGuiScreen(new EditStringScreen(ConfigScreen.this, background, new StringTextComponent(title), configValue.get(), valueSpec::test, configValue::set)));
            this.eventListeners.add(this.button);
        }

        @Override
        public void render(MatrixStack matrixStack, int index, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            super.render(matrixStack, index, top, left, width, p_230432_6_, mouseX, mouseY, hovered, partialTicks);
            this.button.x = left + width - 69;
            this.button.y = top;
            this.button.render(matrixStack, mouseX, mouseY, partialTicks);
        }
    }

    public class ListStringEntry extends ConfigEntry<ForgeConfigSpec.ConfigValue<List<?>>>
    {
        private final Button button;

        public ListStringEntry(ForgeConfigSpec.ConfigValue<List<?>> configValue, ForgeConfigSpec.ValueSpec valueSpec)
        {
            super(configValue, valueSpec);
            String title = createLabelFromConfig(configValue, valueSpec);
            this.button = new Button(10, 5, 46, 20, new TranslationTextComponent("configured.gui.edit"), button -> Minecraft.getInstance().displayGuiScreen(new EditStringListScreen(ConfigScreen.this, new StringTextComponent(title), configValue, valueSpec, background)));
            this.eventListeners.add(this.button);
        }

        @Override
        public void render(MatrixStack matrixStack, int index, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            super.render(matrixStack, index, top, left, width, p_230432_6_, mouseX, mouseY, hovered, partialTicks);
            this.button.x = left + width - 69;
            this.button.y = top;
            this.button.render(matrixStack, mouseX, mouseY, partialTicks);
        }
    }

    public class EnumEntry extends ConfigEntry<ForgeConfigSpec.ConfigValue<Enum>>
    {
        private final Button button;

        public EnumEntry(ForgeConfigSpec.ConfigValue<Enum> configValue, ForgeConfigSpec.ValueSpec valueSpec)
        {
            super(configValue, valueSpec);
            this.button = new Button(10, 5, 46, 20, new TranslationTextComponent("configured.gui.change"), button -> Minecraft.getInstance().displayGuiScreen(new ChangeEnumScreen(ConfigScreen.this, this.label, background, configValue)));
            this.eventListeners.add(this.button);
        }

        @Override
        public void render(MatrixStack matrixStack, int index, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            super.render(matrixStack, index, top, left, width, p_230432_6_, mouseX, mouseY, hovered, partialTicks);
            this.button.x = left + width - 69;
            this.button.y = top;
            this.button.render(matrixStack, mouseX, mouseY, partialTicks);
        }
    }

    /**
     * Gets the last element in a list
     *
     * @param list         the list of get the value from
     * @param defaultValue if the list is empty, return this value instead
     * @param <V>          the type of list
     * @return the last element
     */
    private static <V> V lastValue(List<V> list, V defaultValue)
    {
        if(list.size() > 0)
        {
            return list.get(list.size() - 1);
        }
        return defaultValue;
    }

    /**
     * Tries to create a readable label from the given config value and spec. This will
     * first attempt to create a label from the translation key in the spec, otherwise it
     * will create a readable label from the raw config value name.
     *
     * @param configValue the config value
     * @param valueSpec   the associated value spec
     * @return a readable label string
     */
    private static String createLabelFromConfig(ForgeConfigSpec.ConfigValue<?> configValue, ForgeConfigSpec.ValueSpec valueSpec)
    {
        if(valueSpec.getTranslationKey() != null && I18n.hasKey(valueSpec.getTranslationKey()))
        {
            return new TranslationTextComponent(valueSpec.getTranslationKey()).getString();
        }
        return createLabel(lastValue(configValue.getPath(), ""));
    }

    /**
     * Tries to create a readable label from the given input. This input should be
     * the raw config value name. For example "shouldShowParticles" will be converted
     * to "Should Show Particles".
     *
     * @param input the config value name
     * @return a readable label string
     */
    public static String createLabel(String input)
    {
        String valueName = input;
        // Try split by camel case
        String[] words = valueName.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
        for(int i = 0; i < words.length; i++) words[i] = StringUtils.capitalize(words[i]);
        valueName = Strings.join(words, " ");
        // Try split by underscores
        words = valueName.split("_");
        for(int i = 0; i < words.length; i++) words[i] = StringUtils.capitalize(words[i]);
        // Finally join words. Some mods have inputs like "Foo_Bar" and this causes a double space.
        // To fix this any whitespace is replaced with a single space
        return Strings.join(words, " ").replaceAll("\\s++", " ");
    }

    @Override
    public boolean shouldCloseOnEsc()
    {
        return this.config == null || this.config.getType() != ModConfig.Type.SERVER;
    }

    public static class ConfigFileEntry
    {
        private final ForgeConfigSpec spec;
        private final UnmodifiableConfig config;

        public ConfigFileEntry(ForgeConfigSpec spec, UnmodifiableConfig config)
        {
            this.spec = spec;
            this.config = config;
        }
    }
}
