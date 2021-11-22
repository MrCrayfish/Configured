package com.mrcrayfish.configured.client.screen;

import com.electronwill.nightconfig.core.CommentedConfig;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;

/**
 * Author: MrCrayfish
 */
@OnlyIn(Dist.CLIENT)
public class ConfigScreen extends ListMenuScreen
{
    public static final Comparator<Item> SORT_ALPHABETICALLY = (o1, o2) ->
    {
        if(o1 instanceof FolderItem && o2 instanceof FolderItem)
        {
            return o1.getLabel().compareTo(o2.getLabel());
        }
        if(!(o1 instanceof FolderItem) && o2 instanceof FolderItem)
        {
            return 1;
        }
        if(o1 instanceof FolderItem)
        {
            return -1;
        }
        return o1.getLabel().compareTo(o2.getLabel());
    };

    protected final FolderEntry folderEntry;
    protected ModConfig config;
    protected Button saveButton;
    protected Button restoreButton;

    private ConfigScreen(Screen parent, ITextComponent title, ResourceLocation background, FolderEntry folderEntry)
    {
        super(parent, title, background, 24);
        this.folderEntry = folderEntry;
    }

    public ConfigScreen(Screen parent, ITextComponent title, ModConfig config, ResourceLocation background)
    {
        super(parent, title, background, 24);
        this.folderEntry = new FolderEntry("Root", config.getSpec().getValues(), config.getSpec(), true);
        this.config = config;
    }

    @Override
    protected void constructEntries(List<Item> entries)
    {
        List<Item> configEntries = new ArrayList<>();
        this.folderEntry.getEntries().forEach(c ->
        {
            if(c instanceof FolderEntry)
            {
                configEntries.add(new FolderItem((FolderEntry) c));
            }
            else if(c instanceof ValueEntry)
            {
                ValueEntry configValueEntry = (ValueEntry) c;
                Object value = ((ValueEntry) c).getHolder().getValue();
                if(value instanceof Boolean)
                {
                    configEntries.add(new BooleanItem((ValueHolder<Boolean>) configValueEntry.getHolder()));
                }
                else if(value instanceof Integer)
                {
                    configEntries.add(new IntegerItem((ValueHolder<Integer>) configValueEntry.getHolder()));
                }
                else if(value instanceof Double)
                {
                    configEntries.add(new DoubleItem((ValueHolder<Double>) configValueEntry.getHolder()));
                }
                else if(value instanceof Long)
                {
                    configEntries.add(new LongItem((ValueHolder<Long>) configValueEntry.getHolder()));
                }
                else if(value instanceof Enum)
                {
                    configEntries.add(new EnumItem((ValueHolder<Enum<?>>) configValueEntry.getHolder()));
                }
                else if(value instanceof String)
                {
                    configEntries.add(new StringItem((ValueHolder<String>) configValueEntry.getHolder()));
                }
                else if(value instanceof List<?>)
                {
                    configEntries.add(new ListItem((ListValueHolder) configValueEntry.getHolder()));
                }
                else
                {
                    Configured.LOGGER.info("Unsupported config value: " + configValueEntry.getHolder().configValue.getPath());
                }
            }
        });
        configEntries.sort(SORT_ALPHABETICALLY);
        entries.addAll(configEntries);
    }

    @Override
    protected void init()
    {
        super.init();

        if(this.folderEntry.isRoot())
        {
            this.saveButton = this.addButton(new IconButton(this.width / 2 - 140, this.height - 29, 22, 0, 90, new TranslationTextComponent("configured.gui.save"), (button) ->
            {
                if(this.config != null)
                {
                    this.saveConfig();
                }
                this.minecraft.displayGuiScreen(this.parent);
            }));
            this.restoreButton = this.addButton(new IconButton(this.width / 2 - 45, this.height - 29, 0, 0, 90, new TranslationTextComponent("configured.gui.reset_all"), (button) ->
            {
                if(this.folderEntry.isRoot())
                {
                    this.showRestoreScreen();
                }
            }));
            this.addButton(new Button(this.width / 2 + 50, this.height - 29, 90, 20, DialogTexts.GUI_CANCEL, (button) ->
            {
                if(this.isChanged(this.folderEntry))
                {
                    this.minecraft.displayGuiScreen(new ConfirmationScreen(this, new TranslationTextComponent("configured.gui.unsaved_changes"), result -> {
                        if(!result) return true;
                        this.minecraft.displayGuiScreen(this.parent);
                        return false;
                    }));
                }
                else
                {
                    this.minecraft.displayGuiScreen(this.parent);
                }
            }));
            this.updateButtons();
        }
        else
        {
            this.addButton(new Button(this.width / 2 - 75, this.height - 29, 150, 20, DialogTexts.GUI_BACK, button -> this.minecraft.displayGuiScreen(this.parent)));
        }
    }

    private void saveConfig()
    {
        // Don't need to save if nothing changed
        if(!this.isChanged(this.folderEntry))
            return;

        // Creates a temporary config to merge into the real config. This avoids multiple save calls
        CommentedConfig newConfig = CommentedConfig.copy(this.config.getConfigData());
        Queue<FolderEntry> found = new ArrayDeque<>();
        found.add(this.folderEntry);
        while(!found.isEmpty())
        {
            FolderEntry folder = found.poll();
            for(IEntry entry : folder.getEntries())
            {
                if(entry instanceof FolderEntry)
                {
                    found.offer((FolderEntry) entry);
                }
                else if(entry instanceof ValueEntry)
                {
                    ValueEntry valueEntry = (ValueEntry) entry;
                    ValueHolder<?> holder = valueEntry.getHolder();
                    if(holder.isChanged())
                    {
                        List<String> path = holder.configValue.getPath();
                        if(holder instanceof ListValueHolder)
                        {
                            ListValueHolder listHolder = (ListValueHolder) holder;
                            Function<List<?>, List<?>> converter = listHolder.getConverter();
                            if(converter != null)
                            {
                                List<?> convertedList = converter.apply(listHolder.getValue());
                                newConfig.set(path, convertedList);
                                continue;
                            }
                        }
                        newConfig.set(path, holder.getValue());
                    }
                }
            }
        }
        this.config.getConfigData().putAll(newConfig);
        ConfigHelper.resetCache(this.config);

        // Post logic for server configs
        if(this.config.getType() == ModConfig.Type.SERVER)
        {
            if(!ListMenuScreen.isPlayingGame())
            {
                // Unload server configs since still in main menu
                this.config.getHandler().unload(this.config.getFullPath().getParent(), this.config);
                ConfigHelper.setConfigData(this.config, null);
            }
            else
            {
                ConfigHelper.sendConfigDataToServer(this.config);
            }
        }
    }

    private void showRestoreScreen()
    {
        ConfirmationScreen confirmScreen = new ConfirmationScreen(ConfigScreen.this, new TranslationTextComponent("configured.gui.restore_message"), result ->
        {
            if(!result) return true;
            this.restoreDefaults(this.folderEntry);
            this.updateButtons();
            return true;
        });
        confirmScreen.setBackground(background);
        confirmScreen.setPositiveText(new TranslationTextComponent("configured.gui.reset_all").mergeStyle(TextFormatting.GOLD, TextFormatting.BOLD));
        confirmScreen.setNegativeText(DialogTexts.GUI_CANCEL);
        Minecraft.getInstance().displayGuiScreen(confirmScreen);
    }

    private void restoreDefaults(FolderEntry entry)
    {
        entry.getEntries().forEach(e ->
        {
            if(e instanceof FolderEntry)
            {
                this.restoreDefaults((FolderEntry) e);
            }
            else if(e instanceof ValueEntry)
            {
                ((ValueEntry) e).getHolder().restoreDefaultValue();
            }
        });
    }

    private void updateButtons()
    {
        if(this.folderEntry.isRoot())
        {
            if(this.saveButton != null)
            {
                this.saveButton.active = this.isChanged(this.folderEntry);
            }
            if(this.restoreButton != null)
            {
                this.restoreButton.active = this.isModified(this.folderEntry);
            }
        }
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

    public class FolderItem extends Item
    {
        private final Button button;

        public FolderItem(FolderEntry folderEntry)
        {
            super(new StringTextComponent(createLabel(folderEntry.label)));
            this.button = new Button(10, 5, 44, 20, new StringTextComponent(this.getLabel()).mergeStyle(TextFormatting.BOLD).mergeStyle(TextFormatting.WHITE), onPress -> {
                ITextComponent newTitle = ConfigScreen.this.title.copyRaw().appendString(" > " + this.getLabel());
                ConfigScreen.this.minecraft.displayGuiScreen(new ConfigScreen(ConfigScreen.this, newTitle, background, folderEntry));
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

    public abstract class ConfigItem<T> extends Item
    {
        protected final ValueHolder<T> holder;
        protected final List<IGuiEventListener> eventListeners = Lists.newArrayList();
        protected final Button resetButton;

        @SuppressWarnings("unchecked")
        public ConfigItem(ValueHolder<T> holder)
        {
            super(createLabelFromHolder(holder));
            this.holder = holder;
            if(this.holder.valueSpec.getComment() != null)
            {
                this.tooltip = this.createToolTip(holder);
            }
            int maxTooltipWidth = Math.max(ConfigScreen.this.width / 2 - 43, 170);
            Button.ITooltip tooltip = ScreenUtil.createButtonTooltip(ConfigScreen.this, new TranslationTextComponent("configured.gui.reset"), maxTooltipWidth);
            this.resetButton = new IconButton(0, 0, 0, 0, onPress -> {
                this.holder.restoreDefaultValue();
                this.onResetValue();
            }, tooltip);
            this.eventListeners.add(this.resetButton);
        }

        protected void onResetValue() {}

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

            this.resetButton.active = !this.holder.isDefaultValue();
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

        private List<IReorderingProcessor> createToolTip(ValueHolder<T> holder)
        {
            FontRenderer font = Minecraft.getInstance().fontRenderer;
            List<ITextProperties> lines = font.getCharacterManager().func_238362_b_(new StringTextComponent(holder.valueSpec.getComment()), 200, Style.EMPTY);
            String name = lastValue(holder.configValue.getPath(), "");
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

    public abstract class NumberItem<T extends Number> extends ConfigItem<T>
    {
        private final FocusedTextFieldWidget textField;

        @SuppressWarnings("unchecked")
        public NumberItem(ValueHolder<T> holder, Function<String, Number> parser)
        {
            super(holder);
            this.textField = new FocusedTextFieldWidget(ConfigScreen.this.font, 0, 0, 44, 18, this.label);
            this.textField.setText(holder.getValue().toString());
            this.textField.setResponder((s) ->
            {
                try
                {
                    Number n = parser.apply(s);
                    if(holder.valueSpec.test(n))
                    {
                        this.textField.setTextColor(14737632);
                        holder.setValue((T) n);
                        ConfigScreen.this.updateButtons();
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
            this.textField.setText(this.holder.getValue().toString());
        }
    }

    public class IntegerItem extends NumberItem<Integer>
    {
        public IntegerItem(ValueHolder<Integer> holder)
        {
            super(holder, Integer::parseInt);
        }
    }

    public class DoubleItem extends NumberItem<Double>
    {
        public DoubleItem(ValueHolder<Double> holder)
        {
            super(holder, Double::parseDouble);
        }
    }

    public class LongItem extends NumberItem<Long>
    {
        public LongItem(ValueHolder<Long> holder)
        {
            super(holder, Long::parseLong);
        }
    }

    public class BooleanItem extends ConfigItem<Boolean>
    {
        private final Button button;

        public BooleanItem(ValueHolder<Boolean> holder)
        {
            super(holder);
            this.button = new Button(10, 5, 46, 20, DialogTexts.optionsEnabled(holder.getValue()), button ->
            {
                holder.setValue(!holder.getValue());
                button.setMessage(DialogTexts.optionsEnabled(holder.getValue()));
                ConfigScreen.this.updateButtons();
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
            this.button.setMessage(DialogTexts.optionsEnabled(this.holder.getValue()));
        }
    }

    public class StringItem extends ConfigItem<String>
    {
        private final Button button;

        public StringItem(ValueHolder<String> holder)
        {
            super(holder);
            this.button = new Button(10, 5, 46, 20, new TranslationTextComponent("configured.gui.edit"), button -> Minecraft.getInstance().displayGuiScreen(new EditStringScreen(ConfigScreen.this, background, this.label, holder.getValue(), holder.valueSpec::test, s -> {
                holder.setValue(s);
                ConfigScreen.this.updateButtons();
            })));
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

    public class ListItem extends ConfigItem<List<?>>
    {
        private final Button button;

        public ListItem(ListValueHolder holder)
        {
            super(holder);
            this.button = new Button(10, 5, 46, 20, new TranslationTextComponent("configured.gui.edit"), button -> Minecraft.getInstance().displayGuiScreen(new EditListScreen(ConfigScreen.this, this.label, holder, background)));
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

    public class EnumItem extends ConfigItem<Enum<?>>
    {
        private final Button button;

        public EnumItem(ValueHolder<Enum<?>> holder)
        {
            super(holder);
            this.button = new Button(10, 5, 46, 20, new TranslationTextComponent("configured.gui.change"), button -> Minecraft.getInstance().displayGuiScreen(new ChangeEnumScreen(ConfigScreen.this, this.label, background, holder.getValue(), e -> {
                holder.setValue(e);
                ConfigScreen.this.updateButtons();
            })));
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
     * @param holder the config value holder
     * @return a readable label string
     */
    private static String createLabelFromHolder(ValueHolder<?> holder)
    {
        if(holder.valueSpec.getTranslationKey() != null && I18n.hasKey(holder.valueSpec.getTranslationKey()))
        {
            return new TranslationTextComponent(holder.valueSpec.getTranslationKey()).getString();
        }
        return createLabel(lastValue(holder.configValue.getPath(), ""));
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

    public class ValueHolder<T>
    {
        private final ForgeConfigSpec.ConfigValue<T> configValue;
        private final ForgeConfigSpec.ValueSpec valueSpec;
        private final T initialValue;
        protected T value;

        public ValueHolder(ForgeConfigSpec.ConfigValue<T> configValue, ForgeConfigSpec.ValueSpec valueSpec)
        {
            this.configValue = configValue;
            this.valueSpec = valueSpec;
            this.initialValue = configValue.get();
            this.setValue(configValue.get());
        }

        protected void setValue(T value)
        {
            this.value = value;
        }

        public T getValue()
        {
            return this.value;
        }

        @SuppressWarnings("unchecked")
        public void restoreDefaultValue()
        {
            this.setValue((T) this.valueSpec.getDefault());
            ConfigScreen.this.updateButtons();
        }

        public boolean isDefaultValue()
        {
            return this.value.equals(this.valueSpec.getDefault());
        }

        public boolean isChanged()
        {
            return !this.value.equals(this.initialValue);
        }

        public ForgeConfigSpec.ConfigValue<T> getConfigValue()
        {
            return configValue;
        }

        public ForgeConfigSpec.ValueSpec getSpec()
        {
            return this.valueSpec;
        }
    }

    public class ListValueHolder extends ValueHolder<List<?>>
    {
        @Nullable
        private final Function<List<?>, List<?>> converter;

        public ListValueHolder(ForgeConfigSpec.ConfigValue<List<?>> configValue, ForgeConfigSpec.ValueSpec valueSpec)
        {
            super(configValue, valueSpec);
            this.converter = this.createConverter(configValue);
        }

        @Nullable
        private Function<List<?>, List<?>> createConverter(ForgeConfigSpec.ConfigValue<List<?>> configValue)
        {
            List<?> original = configValue.get();
            if(original instanceof ArrayList)
            {
                return ArrayList::new;
            }
            else if(original instanceof LinkedList)
            {
                return LinkedList::new;
            }
            // TODO allow developers to hook custom list
            return null;
        }

        @Override
        protected void setValue(List<?> value)
        {
            this.value = new ArrayList<>(value);
        }

        @Nullable
        public Function<List<?>, List<?>> getConverter()
        {
            return this.converter;
        }
    }

    public interface IEntry {}

    public class FolderEntry implements IEntry
    {
        private final String label;
        private final UnmodifiableConfig config;
        private final ForgeConfigSpec spec;
        private final boolean root;
        private List<IEntry> entries;

        public FolderEntry(String label, UnmodifiableConfig config, ForgeConfigSpec spec, boolean root)
        {
            this.label = label;
            this.config = config;
            this.spec = spec;
            this.root = root;
            this.init();
        }

        private void init()
        {
            if(this.entries == null)
            {
                ImmutableList.Builder<IEntry> builder = ImmutableList.builder();
                this.config.valueMap().forEach((s, o) ->
                {
                    if(o instanceof UnmodifiableConfig)
                    {
                        builder.add(new FolderEntry(s, (UnmodifiableConfig) o, this.spec, false));
                    }
                    else if(o instanceof ForgeConfigSpec.ConfigValue<?>)
                    {
                        ForgeConfigSpec.ConfigValue<?> configValue = (ForgeConfigSpec.ConfigValue<?>) o;
                        ForgeConfigSpec.ValueSpec valueSpec = this.spec.getRaw(configValue.getPath());
                        builder.add(new ValueEntry(configValue, valueSpec));
                    }
                });
                this.entries = builder.build();
            }
        }

        public boolean isRoot()
        {
            return this.root;
        }

        public boolean isInitialized()
        {
            return this.entries != null;
        }

        public List<IEntry> getEntries()
        {
            return this.entries;
        }
    }

    public class ValueEntry implements IEntry
    {
        private final ValueHolder<?> holder;

        public ValueEntry(ForgeConfigSpec.ConfigValue<?> configValue, ForgeConfigSpec.ValueSpec valueSpec)
        {
            this.holder = configValue.get() instanceof List ? new ListValueHolder((ForgeConfigSpec.ConfigValue<List<?>>) configValue, valueSpec) : new ValueHolder<>(configValue, valueSpec);
        }

        public ValueHolder<?> getHolder()
        {
            return this.holder;
        }
    }

    public boolean isModified(FolderEntry folder)
    {
        for(IEntry entry : folder.getEntries())
        {
            if(entry instanceof FolderEntry)
            {
                if(this.isModified((FolderEntry) entry))
                {
                    return true;
                }
            }
            else if(entry instanceof ValueEntry)
            {
                if(!((ValueEntry) entry).getHolder().isDefaultValue())
                {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isChanged(FolderEntry folder)
    {
        for(IEntry entry : folder.getEntries())
        {
            if(entry instanceof FolderEntry)
            {
                if(this.isChanged((FolderEntry) entry))
                {
                    return true;
                }
            }
            else if(entry instanceof ValueEntry)
            {
                if(((ValueEntry) entry).getHolder().isChanged())
                {
                    return true;
                }
            }
        }
        return false;
    }
}
