package com.mrcrayfish.configured.client.screen;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.client.screen.widget.IconButton;
import com.mrcrayfish.configured.client.util.ScreenUtil;
import com.mrcrayfish.configured.util.ConfigHelper;
import joptsimple.internal.Strings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Function;

/**
 * Author: MrCrayfish
 */
@OnlyIn(Dist.CLIENT)
public class ConfigScreen extends ListMenuScreen
{
    public static final int TOOLTIP_WIDTH = 200;
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

    private ConfigScreen(Screen parent, Component title, ResourceLocation background, FolderEntry folderEntry)
    {
        super(parent, title, background, 24);
        this.folderEntry = folderEntry;
    }

    public ConfigScreen(Screen parent, Component title, ModConfig config, ResourceLocation background)
    {
        super(parent, title, background, 24);
        this.folderEntry = new FolderEntry(((ForgeConfigSpec) config.getSpec()).getValues(), (ForgeConfigSpec) config.getSpec());
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
            this.saveButton = this.addRenderableWidget(new IconButton(this.width / 2 - 140, this.height - 29, 22, 0, 90, Component.translatable("configured.gui.save"), (button) ->
            {
                if(this.config != null)
                {
                    this.saveConfig();
                }
                this.minecraft.setScreen(this.parent);
            }));
            this.restoreButton = this.addRenderableWidget(new IconButton(this.width / 2 - 45, this.height - 29, 0, 0, 90, Component.translatable("configured.gui.reset_all"), (button) ->
            {
                if(this.folderEntry.isRoot())
                {
                    this.showRestoreScreen();
                }
            }));
            this.addRenderableWidget(new Button(this.width / 2 + 50, this.height - 29, 90, 20, CommonComponents.GUI_CANCEL, (button) ->
            {
                if(this.isChanged(this.folderEntry))
                {
                    this.minecraft.setScreen(new ConfirmationScreen(this, Component.translatable("configured.gui.unsaved_changes"), result -> {
                        if(!result) return true;
                        this.minecraft.setScreen(this.parent);
                        return false;
                    }).setBackground(this.background));
                }
                else
                {
                    this.minecraft.setScreen(this.parent);
                }
            }));
            this.updateButtons();
        }
        else
        {
            this.addRenderableWidget(new Button(this.width / 2 - 75, this.height - 29, 150, 20, CommonComponents.GUI_BACK, button -> this.minecraft.setScreen(this.parent)));
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

        // Post logic for server configs
        if(this.config.getType() == ModConfig.Type.SERVER)
        {
            if(!ListMenuScreen.isPlayingGame())
            {
                // Unload server configs since still in main menu
                this.config.getHandler().unload(this.config.getFullPath().getParent(), this.config);
                ConfigHelper.setConfigData(this.config, null);
            }
        }
        else
        {
            Configured.LOGGER.info("Sending config reloading event for {}", this.config.getFileName());
            this.config.getSpec().afterReload();
            ConfigHelper.fireEvent(this.config, new ModConfigEvent.Reloading(this.config));
        }
    }

    private void showRestoreScreen()
    {
        ConfirmationScreen confirmScreen = new ConfirmationScreen(ConfigScreen.this, Component.translatable("configured.gui.restore_message"), result ->
        {
            if(!result) return true;
            this.restoreDefaults(this.folderEntry);
            this.updateButtons();
            return true;
        });
        confirmScreen.setBackground(background);
        confirmScreen.setPositiveText(Component.translatable("configured.gui.reset_all").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        confirmScreen.setNegativeText(CommonComponents.GUI_CANCEL);
        Minecraft.getInstance().setScreen(confirmScreen);
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
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
    {
        this.activeTooltip = null;
        this.renderBackground(poseStack);
        this.list.render(poseStack, mouseX, mouseY, partialTicks);
        this.searchTextField.render(poseStack, mouseX, mouseY, partialTicks);
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, 7, 0xFFFFFF);
        super.render(poseStack, mouseX, mouseY, partialTicks);
    }

    public class FolderItem extends Item
    {
        private final Button button;

        public FolderItem(FolderEntry folderEntry)
        {
            super(createLabelForFolderEntry(folderEntry));
            this.button = new Button(10, 5, 44, 20, Component.literal(this.getLabel()).withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.WHITE), onPress -> {
                Component newTitle = ConfigScreen.this.title.copy().append(" > " + this.getLabel());
                ConfigScreen.this.minecraft.setScreen(new ConfigScreen(ConfigScreen.this, newTitle, background, folderEntry));
            });
            if(folderEntry.getComment() != null)
            {
                this.tooltip = Language.getInstance().getVisualOrder(ConfigScreen.this.getTranslatableComment(folderEntry));
            }
        }

        @Override
        public List<? extends GuiEventListener> children()
        {
            return ImmutableList.of(this.button);
        }

        @Override
        public void render(PoseStack poseStack, int x, int top, int left, int width, int height, int mouseX, int mouseY, boolean selected, float partialTicks)
        {
            if(this.isMouseOver(mouseX, mouseY))
            {
                ConfigScreen.this.setActiveTooltip(this.tooltip);
            }

            this.button.x = left - 1;
            this.button.y = top;
            this.button.setWidth(width);
            this.button.render(poseStack, mouseX, mouseY, partialTicks);
        }

        private static Component createLabelForFolderEntry(FolderEntry folderEntry)
        {
            if(folderEntry.getTranslationKey() != null && I18n.exists(folderEntry.getTranslationKey()))
            {
                return Component.translatable(folderEntry.getTranslationKey());
            }
            return Component.literal(createLabel(folderEntry.getLabel()));
        }
    }

    public abstract class ConfigItem<T> extends Item
    {
        protected final ValueHolder<T> holder;
        protected final List<GuiEventListener> eventListeners = new ArrayList<>();
        protected final Button resetButton;

        @SuppressWarnings("unchecked")
        public ConfigItem(ValueHolder<T> holder)
        {
            super(createLabelFromHolder(holder));
            this.holder = holder;
            if(this.holder.getComment() != null)
            {
                this.tooltip = this.createToolTip(holder);
            }
            int maxTooltipWidth = Math.max(ConfigScreen.this.width / 2 - 43, 170);
            Button.OnTooltip tooltip = ScreenUtil.createButtonTooltip(ConfigScreen.this, Component.translatable("configured.gui.reset"), maxTooltipWidth);
            this.resetButton = new IconButton(0, 0, 0, 0, onPress -> {
                this.holder.restoreDefaultValue();
                this.onResetValue();
            }, tooltip);
            this.eventListeners.add(this.resetButton);
        }

        protected void onResetValue() {}

        @Override
        public List<? extends GuiEventListener> children()
        {
            return this.eventListeners;
        }

        @Override
        public void render(PoseStack poseStack, int x, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            Minecraft.getInstance().font.draw(poseStack, this.getTrimmedLabel(width - 75), left, top + 6, 0xFFFFFF);

            if(this.isMouseOver(mouseX, mouseY) && mouseX < ConfigScreen.this.list.getRowLeft() + ConfigScreen.this.list.getRowWidth() - 67)
            {
                ConfigScreen.this.setActiveTooltip(this.tooltip);
            }

            this.resetButton.active = !this.holder.isDefaultValue();
            this.resetButton.x = left + width - 21;
            this.resetButton.y = top;
            this.resetButton.render(poseStack, mouseX, mouseY, partialTicks);
        }

        private Component getTrimmedLabel(int maxWidth)
        {
            if(ConfigScreen.this.minecraft.font.width(this.label) > maxWidth)
            {
                return Component.literal(ConfigScreen.this.minecraft.font.substrByWidth(this.label, maxWidth).getString() + "...");
            }
            return this.label;
        }

        private List<FormattedCharSequence> createToolTip(ValueHolder<T> holder)
        {
            List<FormattedText> lines = ConfigScreen.this.getTranslatableComment(holder);
            if(lines != null)
            {
                String name = lastValue(holder.configValue.getPath(), "");
                lines.add(0, Component.literal(name).withStyle(ChatFormatting.YELLOW));
                for(int i = 1; i < lines.size(); i++) // Search will never match index 0
                {
                    String text = lines.get(i).getString();
                    if(text.startsWith("Range: ") || text.startsWith("Allowed Values: "))
                    {
                        for(int j = i; j < lines.size(); j++)
                        {
                            lines.set(j, Component.literal(lines.get(j).getString()).withStyle(ChatFormatting.GRAY));
                        }
                        break;
                    }
                }
                return Language.getInstance().getVisualOrder(lines);
            }
            return null;
        }
    }

    /**
     * Creates a translatable comment for tooltip
     *
     * @param entry a commented translatable to use for creating tooltip lines
     * @return a list of formatted text representing the tooltip lines or null if no comment exists
     */
    @Nullable
    private List<FormattedText> getTranslatableComment(ICommentedTranslatable entry)
    {
        String rawComment = entry.getComment();
        String key = entry.getTranslationKey();
        if(key != null && I18n.exists(key + ".tooltip")) // Still check for translation even if rawComment is null
        {
            MutableComponent comment = Component.translatable(key + ".tooltip");
            if(rawComment != null)
            {
                int rangeIndex = rawComment.indexOf("Range: ");
                int allowedValIndex = rawComment.indexOf("Allowed Values: ");
                if(rangeIndex >= 0 || allowedValIndex >= 0)
                {
                    comment.append(Component.literal(rawComment.substring(Math.max(rangeIndex, allowedValIndex) - 1))); // - 1 to include new line char
                }
            }
            return splitTooltip(comment);
        }
        return rawComment != null ? splitTooltip(Component.literal(rawComment)) : null;
    }

    public abstract class NumberItem<T extends Number> extends ConfigItem<T>
    {
        private final FocusedEditBox textField;

        @SuppressWarnings("unchecked")
        public NumberItem(ValueHolder<T> holder, Function<String, Number> parser)
        {
            super(holder);
            this.textField = new FocusedEditBox(ConfigScreen.this.font, 0, 0, 44, 18, this.label);
            this.textField.setValue(holder.getValue().toString());
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
        public void render(PoseStack poseStack, int index, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            super.render(poseStack, index, top, left, width, p_230432_6_, mouseX, mouseY, hovered, partialTicks);
            this.textField.x = left + width - 68;
            this.textField.y = top + 1;
            this.textField.render(poseStack, mouseX, mouseY, partialTicks);
        }

        @Override
        public void onResetValue()
        {
            this.textField.setValue(this.holder.getValue().toString());
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
            this.button = new Button(10, 5, 46, 20, CommonComponents.optionStatus(holder.getValue()), button ->
            {
                holder.setValue(!holder.getValue());
                button.setMessage(CommonComponents.optionStatus(holder.getValue()));
                ConfigScreen.this.updateButtons();
            });
            this.eventListeners.add(this.button);
        }

        @Override
        public void render(PoseStack poseStack, int index, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            super.render(poseStack, index, top, left, width, p_230432_6_, mouseX, mouseY, hovered, partialTicks);
            this.button.x = left + width - 69;
            this.button.y = top;
            this.button.render(poseStack, mouseX, mouseY, partialTicks);
        }

        @Override
        public void onResetValue()
        {
            this.button.setMessage(CommonComponents.optionStatus(this.holder.getValue()));
        }
    }

    public class StringItem extends ConfigItem<String>
    {
        private final Button button;

        public StringItem(ValueHolder<String> holder)
        {
            super(holder);
            this.button = new Button(10, 5, 46, 20, Component.translatable("configured.gui.edit"), button -> Minecraft.getInstance().setScreen(new EditStringScreen(ConfigScreen.this, background, this.label, holder.getValue(), holder.valueSpec::test, s -> {
                holder.setValue(s);
                ConfigScreen.this.updateButtons();
            })));
            this.eventListeners.add(this.button);
        }

        @Override
        public void render(PoseStack poseStack, int index, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            super.render(poseStack, index, top, left, width, p_230432_6_, mouseX, mouseY, hovered, partialTicks);
            this.button.x = left + width - 69;
            this.button.y = top;
            this.button.render(poseStack, mouseX, mouseY, partialTicks);
        }
    }

    public class ListItem extends ConfigItem<List<?>>
    {
        private final Button button;

        public ListItem(ListValueHolder holder)
        {
            super(holder);
            this.button = new Button(10, 5, 46, 20, Component.translatable("configured.gui.edit"), button -> Minecraft.getInstance().setScreen(new EditListScreen(ConfigScreen.this, this.label, holder, background)));
            this.eventListeners.add(this.button);
        }

        @Override
        public void render(PoseStack poseStack, int index, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            super.render(poseStack, index, top, left, width, p_230432_6_, mouseX, mouseY, hovered, partialTicks);
            this.button.x = left + width - 69;
            this.button.y = top;
            this.button.render(poseStack, mouseX, mouseY, partialTicks);
        }
    }

    public class EnumItem extends ConfigItem<Enum<?>>
    {
        private final Button button;

        public EnumItem(ValueHolder<Enum<?>> holder)
        {
            super(holder);
            this.button = new Button(10, 5, 46, 20, Component.translatable("configured.gui.change"), button -> Minecraft.getInstance().setScreen(new ChangeEnumScreen(ConfigScreen.this, this.label, background, holder.getValue(), e -> {
                holder.setValue(e);
                ConfigScreen.this.updateButtons();
            })));
            this.eventListeners.add(this.button);
        }

        @Override
        public void render(PoseStack poseStack, int index, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            super.render(poseStack, index, top, left, width, p_230432_6_, mouseX, mouseY, hovered, partialTicks);
            this.button.x = left + width - 69;
            this.button.y = top;
            this.button.render(poseStack, mouseX, mouseY, partialTicks);
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
        if(holder.valueSpec.getTranslationKey() != null && I18n.exists(holder.valueSpec.getTranslationKey()))
        {
            return Component.translatable(holder.valueSpec.getTranslationKey()).getString();
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

    /**
     * Word wraps formatted text for tooltips using a standardised width
     *
     * @param text the text to wrap
     * @return a list of formatted text representing each wrapped line
     */
    private static List<FormattedText> splitTooltip(FormattedText text)
    {
        return Minecraft.getInstance().font.getSplitter().splitLines(text, TOOLTIP_WIDTH, Style.EMPTY);
    }

    @Override
    public boolean shouldCloseOnEsc()
    {
        return this.config == null || this.config.getType() != ModConfig.Type.SERVER;
    }

    public interface ICommentedTranslatable
    {
        @Nullable
        String getComment();

        @Nullable
        String getTranslationKey();
    }

    public class ValueHolder<T> implements ICommentedTranslatable
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

        @Nullable
        @Override
        public String getComment()
        {
            return this.valueSpec.getComment();
        }

        @Nullable
        @Override
        public String getTranslationKey()
        {
            return this.valueSpec.getTranslationKey();
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

    public class FolderEntry implements IEntry, ICommentedTranslatable
    {
        private final List<String> path;
        private final UnmodifiableConfig config;
        private final ForgeConfigSpec spec;
        private List<IEntry> entries;

        public FolderEntry(UnmodifiableConfig config, ForgeConfigSpec spec)
        {
            this(new ArrayList<>(), config, spec);
        }

        public FolderEntry(List<String> path, UnmodifiableConfig config, ForgeConfigSpec spec)
        {
            this.path = path;
            this.config = config;
            this.spec = spec;
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
                        List<String> path = new ArrayList<>(this.path);
                        path.add(s);
                        builder.add(new FolderEntry(path, (UnmodifiableConfig) o, this.spec));
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
            return this.path.isEmpty();
        }

        public boolean isInitialized()
        {
            return this.entries != null;
        }

        public List<IEntry> getEntries()
        {
            return this.entries;
        }

        public String getLabel()
        {
            return lastValue(this.path, "Root");
        }

        @Nullable
        @Override
        public String getComment()
        {
            return this.spec.getLevelComment(this.path);
        }

        @Nullable
        @Override
        public String getTranslationKey()
        {
            return this.spec.getLevelTranslationKey(this.path);
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
