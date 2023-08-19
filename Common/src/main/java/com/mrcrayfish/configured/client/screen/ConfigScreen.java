package com.mrcrayfish.configured.client.screen;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mrcrayfish.configured.Config;
import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.client.screen.widget.CheckBoxButton;
import com.mrcrayfish.configured.client.screen.widget.ConfiguredButton;
import com.mrcrayfish.configured.client.screen.widget.IconButton;
import com.mrcrayfish.configured.client.util.ScreenUtil;
import com.mrcrayfish.configured.util.ConfigHelper;
import joptsimple.internal.Strings;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public class ConfigScreen extends ListMenuScreen implements IEditing
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

    protected final IConfigEntry folderEntry;
    protected final IModConfig config;
    protected final Map<String, String> cachedTextMap = new HashMap<>();
    protected Button saveButton;
    protected Button restoreButton;
    protected CheckBoxButton deepSearchCheckBox;

    private ConfigScreen(Screen parent, Component title, IModConfig config, ResourceLocation background, IConfigEntry folderEntry)
    {
        super(parent, title, background, 24);
        this.config = config;
        this.folderEntry = folderEntry;
    }

    public ConfigScreen(Screen parent, Component title, IModConfig config, ResourceLocation background)
    {
        super(parent, title, background, 24);
        this.config = config;
        this.folderEntry = config.getRoot();
    }

    @Override
    public IModConfig getActiveConfig()
    {
        return this.config;
    }

    @Override
    public void removed()
    {
        this.cachedTextMap.clear();
    }

    @Override
    protected void constructEntries(List<Item> entries)
    {
        List<Item> configEntries = new ArrayList<>();
        this.folderEntry.getChildren().forEach(entry -> {
            Item item = this.createItemFromEntry(entry);
            if(item != null) configEntries.add(item);
        });
        configEntries.sort(SORT_ALPHABETICALLY);
        entries.addAll(configEntries);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private Item createItemFromEntry(IConfigEntry entry)
    {
        if(entry.isLeaf())
        {
            IConfigValue<?> value = entry.getValue();
            if(value != null)
            {
                Object object = value.get();
                if(object instanceof Boolean)
                {
                    return new BooleanItem((IConfigValue<Boolean>) value);
                }
                else if(object instanceof Integer)
                {
                    return new IntegerItem((IConfigValue<Integer>) value);
                }
                else if(object instanceof Double)
                {
                    return new DoubleItem((IConfigValue<Double>) value);
                }
                else if(object instanceof Long)
                {
                    return new LongItem((IConfigValue<Long>) value);
                }
                else if(object instanceof Enum)
                {
                    return new EnumItem((IConfigValue<Enum<?>>) value);
                }
                else if(object instanceof String)
                {
                    return new StringItem((IConfigValue<String>) value);
                }
                else if(object instanceof List<?>)
                {
                    return new ListItem((IConfigValue<List<?>>) value);
                }
                else
                {
                    Constants.LOG.info("Unsupported config value: " + value.getName());
                }
            }
            return null;
        }
        return new FolderItem(entry);
    }

    @Override
    protected void init()
    {
        super.init();

        if(this.folderEntry.isRoot())
        {
            this.saveButton = this.addRenderableWidget(new IconButton(this.width / 2 - 140, this.height - 29, 22, 0, 90, Component.translatable("configured.gui.save"), (button) ->
            {
                this.saveConfig();
                if(ConfigHelper.getChangedValues(this.folderEntry).stream().anyMatch(IConfigValue::requiresGameRestart))
                {
                    ConfirmationScreen confirm = new ConfirmationScreen(this.parent, Component.translatable("configured.gui.game_restart_needed"), ConfirmationScreen.Icon.INFO, result -> true);
                    confirm.setPositiveText(Component.translatable("configured.gui.close"));
                    confirm.setNegativeText(null);
                    this.minecraft.setScreen(confirm);
                }
                else if(this.minecraft.level != null && ConfigHelper.getChangedValues(this.folderEntry).stream().anyMatch(IConfigValue::requiresWorldRestart))
                {
                    ConfirmationScreen confirm = new ConfirmationScreen(this.parent, Component.translatable("configured.gui.world_restart_needed"), ConfirmationScreen.Icon.INFO, result -> true);
                    confirm.setPositiveText(Component.translatable("configured.gui.close"));
                    confirm.setNegativeText(null);
                    this.minecraft.setScreen(confirm);
                }
                else
                {
                    this.minecraft.setScreen(this.parent);
                }
            }));
            this.restoreButton = this.addRenderableWidget(new IconButton(this.width / 2 - 45, this.height - 29, 0, 0, 90, Component.translatable("configured.gui.reset_all"), (button) ->
            {
                if(this.folderEntry.isRoot())
                {
                    this.showRestoreScreen();
                }
            }));
            this.addRenderableWidget(ScreenUtil.button(this.width / 2 + 50, this.height - 29, 90, 20, CommonComponents.GUI_CANCEL, (button) ->
            {
                if(this.isChanged(this.folderEntry))
                {
                    this.minecraft.setScreen(new ActiveConfirmationScreen(this, ConfigScreen.this.config, Component.translatable("configured.gui.unsaved_changes"), ConfirmationScreen.Icon.WARNING, result -> {
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
            this.addRenderableWidget(new IconButton(this.width / 2 - 130, this.height - 29, 22, 44, 128, Component.translatable("configured.gui.home"), button -> {
                ConfigScreen target = this;
                while(true)
                {
                    if(target.parent instanceof ConfigScreen)
                    {
                        target = (ConfigScreen) target.parent;
                        continue;
                    }
                    break;
                }
                this.minecraft.setScreen(target);
            }));
            this.addRenderableWidget(ScreenUtil.button(this.width / 2 + 2, this.height - 29, 128, 20, CommonComponents.GUI_BACK, button -> this.minecraft.setScreen(this.parent)));
        }

        this.deepSearchCheckBox = new CheckBoxButton(this.width / 2 + 115, 25, button -> this.updateSearchResults());
        this.addRenderableWidget(this.deepSearchCheckBox);
    }

    private void saveConfig()
    {
        // Don't need to save if nothing changed
        if(!this.isChanged(this.folderEntry) || this.config == null)
            return;
        this.config.update(this.folderEntry);
    }

    private void showRestoreScreen()
    {
        ConfirmationScreen confirmScreen = new ActiveConfirmationScreen(ConfigScreen.this, ConfigScreen.this.config, Component.translatable("configured.gui.restore_message"), ConfirmationScreen.Icon.WARNING, result -> {
            if(!result) return true;
            this.restoreDefaults(this.folderEntry);
            this.updateButtons();
            return true;
        });
        confirmScreen.setBackground(this.background);
        confirmScreen.setPositiveText(Component.translatable("configured.gui.reset_all").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        confirmScreen.setNegativeText(CommonComponents.GUI_CANCEL);
        Minecraft.getInstance().setScreen(confirmScreen);
    }

    private void restoreDefaults(IConfigEntry entry)
    {
        for(IConfigEntry child : entry.getChildren())
        {
            if(child.isLeaf())
            {
                IConfigValue<?> value = child.getValue();
                if(value != null) value.restore();
                continue;
            }
            this.restoreDefaults(child);
        }
    }

    private void updateButtons()
    {
        if(this.folderEntry.isRoot())
        {
            if(this.saveButton != null)
            {
                this.saveButton.active = !this.config.isReadOnly() && this.isChanged(this.folderEntry);
            }
            if(this.restoreButton != null)
            {
                this.restoreButton.active = !this.config.isReadOnly() && this.isModified(this.folderEntry);
            }
        }
    }

    @Override
    protected void renderForeground(GuiGraphics poseStack, int mouseX, int mouseY, float partialTicks)
    {
        if(this.config.isReadOnly())
        {
            RenderSystem.setShaderTexture(0, IconButton.ICONS);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.blit(IconButton.ICONS, this.width - 30, 14, 20, 20, 0, 33, 10, 10, 64, 64);
            if(ScreenUtil.isMouseWithin(this.width - 30, 14, 20, 20, mouseX, mouseY))
            {
                this.setActiveTooltip(Component.translatable("configured.gui.read_only_config"), 0xFF1E6566);
            }
        }

        if(this.deepSearchCheckBox.isMouseOver(mouseX, mouseY))
        {
            this.setActiveTooltip(Component.translatable("configured.gui.deep_search"));
        }
    }

    @Override
    protected Collection<Item> getSearchResults(String s)
    {
        List<Item> entries = this.entries;
        if(this.deepSearchCheckBox.selected())
        {
            List<Item> allEntries = new ArrayList<>();
            ConfigHelper.gatherAllConfigEntries(this.folderEntry).forEach(entry -> {
                Item item = this.createItemFromEntry(entry);
                if(item instanceof ConfigItem<?>) {
                    allEntries.add(item);
                }
            });
            allEntries.sort(SORT_ALPHABETICALLY);
            entries = allEntries;
        }
        return entries.stream().filter(item -> {
            if(item instanceof IIgnoreSearch)
                return false;
            if(item instanceof FolderItem && !Config.CLIENT.includeFoldersInSearch.get())
                return false;
            return item.getLabel().toLowerCase(Locale.ENGLISH).contains(s.toLowerCase(Locale.ENGLISH));
        }).collect(Collectors.toList());
    }

    public class FolderItem extends Item
    {
        private final IconButton button;

        public FolderItem(IConfigEntry entry)
        {
            super(createLabelForFolderEntry(entry));
            this.button = new IconButton(10, 5, 11, 33, 0, Component.literal(this.getLabel()).withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.WHITE), onPress -> {
                Component newTitle = ConfigScreen.this.title.copy().append(Component.literal(" > ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)).append(this.getLabel());
                ConfigScreen.this.minecraft.setScreen(new ConfigScreen(ConfigScreen.this, newTitle, ConfigScreen.this.config, ConfigScreen.this.background, entry));
            });
            if(entry.getTooltip() != null)
            {
                this.tooltip = Language.getInstance().getVisualOrder(splitTooltip(entry.getTooltip()));
            }
        }

        @Override
        public List<? extends GuiEventListener> children()
        {
            return ImmutableList.of(this.button);
        }

        @Override
        public void render(GuiGraphics poseStack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean selected, float partialTicks)
        {
            super.render(poseStack, index, top, left, width, height, mouseX, mouseY, selected, partialTicks);
            this.button.setX(left - 1);
            this.button.setY(top);
            this.button.setWidth(width);
            this.button.render(poseStack, mouseX, mouseY, partialTicks);
        }

        private static Component createLabelForFolderEntry(IConfigEntry entry)
        {
            String key = entry.getTranslationKey();
            if(key != null && I18n.exists(key))
            {
                return Component.translatable(key);
            }
            return Component.literal(createLabel(entry.getEntryName()));
        }
    }

    public abstract class ConfigItem<T> extends Item
    {
        protected final IConfigValue<T> holder;
        protected final List<GuiEventListener> eventListeners = new ArrayList<>();
        protected final ConfiguredButton resetButton;
        protected Component validationHint;

        public ConfigItem(IConfigValue<T> holder)
        {
            super(createLabelFromHolder(holder));
            this.holder = holder;
            this.tooltip = this.createToolTip(holder);
            //TODO I need custom widths!
            int maxTooltipWidth = Math.max(ConfigScreen.this.width / 2 - 43, 170);
            this.resetButton = new IconButton(0, 0, 0, 0, onPress -> {
                this.holder.restore();
                this.onResetValue();
                ConfigScreen.this.updateButtons();
            });
            this.resetButton.setTooltip(Tooltip.create(Component.translatable("configured.gui.reset")), btn -> btn.isActive() && btn.isHoveredOrFocused());
            this.resetButton.active = !ConfigScreen.this.config.isReadOnly();
            this.eventListeners.add(this.resetButton);
        }

        protected void onResetValue()
        {
        }

        @Override
        public List<? extends GuiEventListener> children()
        {
            return this.eventListeners;
        }

        @Override
        public void render(GuiGraphics poseStack, int x, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            boolean showValidationHint = this.validationHint != null;
            int trimLength = showValidationHint ? 100 : 80;
            ChatFormatting labelStyle = this.holder.isChanged() ? Config.CLIENT.changedFormatting.get() : ChatFormatting.RESET;
            //TODO check for new
            //Minecraft.getInstance().font.draw(poseStack, this.getTrimmedLabel(width - trimLength).withStyle(labelStyle), left, top + 6, 0xFFFFFF);

            if(showValidationHint)
            {
                RenderSystem.setShaderTexture(0, IconButton.ICONS);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                poseStack.blit(IconButton.ICONS, left + width - 88, top + 3, 16, 16, 11, 11, 11, 11, 64, 64);
            }

            if(!ConfigScreen.this.config.isReadOnly())
            {
                if(this.holder.requiresGameRestart() || this.holder.requiresWorldRestart())
                {
                    boolean gameRestart = this.holder.requiresGameRestart();
                    RenderSystem.setShaderTexture(0, IconButton.ICONS);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    poseStack.blit(IconButton.ICONS, left - 18, top + 5, 11, 11, gameRestart ? 51 : 11, 22, 11, 11, 64, 64);

                    if(ScreenUtil.isMouseWithin(left - 18, top + 5, 11, 11, mouseX, mouseY))
                    {
                        String translationKey = gameRestart ? "configured.gui.requires_game_restart" : "configured.gui.requires_world_restart";
                        int outline = gameRestart ? 0xAA487CC0 : 0xAA56B252;
                        ConfigScreen.this.setActiveTooltip(Component.translatable(translationKey), outline);
                    }
                }
            }

            if(this.isMouseOver(mouseX, mouseY))
            {
                if(showValidationHint && ScreenUtil.isMouseWithin(left + width - 92, top, 23, 20, mouseX, mouseY))
                {
                    ConfigScreen.this.setActiveTooltip(this.validationHint, 0xAADD0000);
                }
                else if(mouseX < ConfigScreen.this.list.getRowLeft() + ConfigScreen.this.list.getRowWidth() - 69)
                {
                    ConfigScreen.this.setActiveTooltip(this.tooltip);
                }
            }

            this.resetButton.active = !this.holder.isDefault() && !ConfigScreen.this.config.isReadOnly();
            this.resetButton.setX(left + width - 21);
            this.resetButton.setY(top);
            this.resetButton.render(poseStack, mouseX, mouseY, partialTicks);
        }

        private MutableComponent getTrimmedLabel(int maxWidth)
        {
            if(ConfigScreen.this.minecraft.font.width(this.label) > maxWidth)
            {
                return Component.literal(ConfigScreen.this.minecraft.font.substrByWidth(this.label, maxWidth).getString() + "...");
            }
            return this.label.copy();
        }

        @Nullable
        private List<FormattedCharSequence> createToolTip(IConfigValue<T> holder)
        {
            Component comment = holder.getComment();
            if(comment != null)
            {
                Font font = Minecraft.getInstance().font;
                List<FormattedText> lines = font.getSplitter().splitLines(comment, 200, Style.EMPTY);
                String name = holder.getName();
                lines.add(0, Component.literal(name).withStyle(ChatFormatting.YELLOW));
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
                        lines.set(i, Component.literal(lines.get(i).getString()).withStyle(ChatFormatting.GRAY));
                    }
                }
                return Language.getInstance().getVisualOrder(lines);
            }
            return null;
        }

        public void setValidationHint(Component text)
        {
            this.validationHint = text;
        }
    }

    public abstract class NumberItem<T extends Number> extends ConfigItem<T>
    {
        private final FocusedEditBox textField;
        private long lastTick;

        @SuppressWarnings("unchecked")
        public NumberItem(IConfigValue<T> holder, Function<String, Number> parser)
        {
            super(holder);
            String text = ConfigScreen.this.cachedTextMap.getOrDefault(holder.getName(), holder.get().toString());
            this.textField = new FocusedEditBox(ConfigScreen.this.font, 0, 0, 44, 18, this.label);
            this.textField.setResponder((s) ->
            {
                ConfigScreen.this.cachedTextMap.put(holder.getName(), s);
                try
                {
                    Number n = parser.apply(s);
                    if(holder.isValid((T) n))
                    {
                        this.textField.setTextColor(14737632);
                        holder.set((T) n);
                        ConfigScreen.this.updateButtons();
                        this.setValidationHint(null);
                    }
                    else
                    {
                        this.textField.setTextColor(16711680);
                        this.setValidationHint(holder.getValidationHint());
                    }
                }
                catch(Exception ignored)
                {
                    this.textField.setTextColor(16711680);
                    this.setValidationHint(Component.translatable("configured.validator.not_a_number"));
                }
            });
            this.textField.setValue(text);
            this.textField.setEditable(!ConfigScreen.this.config.isReadOnly());
            this.eventListeners.add(this.textField);
        }

        @Override
        public void render(GuiGraphics poseStack, int index, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            super.render(poseStack, index, top, left, width, p_230432_6_, mouseX, mouseY, hovered, partialTicks);
            long time = Util.getMillis();
            if(time - this.lastTick >= 50)
            {
                this.textField.tick();
                this.lastTick = time;
            }
            this.textField.setX(left + width - 68);
            this.textField.setY(top + 1);
            this.textField.render(poseStack, mouseX, mouseY, partialTicks);
        }

        @Override
        public void onResetValue()
        {
            this.textField.setValue(this.holder.get().toString());
        }
    }

    public class IntegerItem extends NumberItem<Integer>
    {
        public IntegerItem(IConfigValue<Integer> holder)
        {
            super(holder, Integer::parseInt);
        }
    }

    public class DoubleItem extends NumberItem<Double>
    {
        public DoubleItem(IConfigValue<Double> holder)
        {
            super(holder, Double::parseDouble);
        }
    }

    public class LongItem extends NumberItem<Long>
    {
        public LongItem(IConfigValue<Long> holder)
        {
            super(holder, Long::parseLong);
        }
    }

    public class BooleanItem extends ConfigItem<Boolean>
    {
        private final Button button;

        public BooleanItem(IConfigValue<Boolean> holder)
        {
            super(holder);
            this.button = ScreenUtil.button(10, 5, 46, 20, CommonComponents.optionStatus(holder.get()), button -> {
                holder.set(!holder.get());
                button.setMessage(CommonComponents.optionStatus(holder.get()));
                ConfigScreen.this.updateButtons();
            });
            this.button.active = !ConfigScreen.this.config.isReadOnly();
            this.eventListeners.add(this.button);
        }

        @Override
        public void render(GuiGraphics poseStack, int index, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            super.render(poseStack, index, top, left, width, p_230432_6_, mouseX, mouseY, hovered, partialTicks);
            this.button.setX(left + width - 69);
            this.button.setY(top);
            this.button.render(poseStack, mouseX, mouseY, partialTicks);
        }

        @Override
        public void onResetValue()
        {
            this.button.setMessage(CommonComponents.optionStatus(this.holder.get()));
        }
    }

    public class StringItem extends ConfigItem<String>
    {
        private final Button button;

        public StringItem(IConfigValue<String> holder)
        {
            super(holder);
            Component buttonText = ConfigScreen.this.config.isReadOnly() ? Component.translatable("configured.gui.view") : Component.translatable("configured.gui.edit");
            this.button = ScreenUtil.button(10, 5, 46, 20, buttonText, button -> Minecraft.getInstance().setScreen(new EditStringScreen(ConfigScreen.this, ConfigScreen.this.config, ConfigScreen.this.background, this.label, holder.get(), s -> {
                return holder.isValid(s) ? Pair.of(true, CommonComponents.EMPTY) : Pair.of(false, holder.getValidationHint());
            }, s -> {
                holder.set(s);
                ConfigScreen.this.updateButtons();
            })));
            this.eventListeners.add(this.button);
        }

        @Override
        public void render(GuiGraphics poseStack, int index, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            super.render(poseStack, index, top, left, width, p_230432_6_, mouseX, mouseY, hovered, partialTicks);
            this.button.setX(left + width - 69);
            this.button.setY(top);
            this.button.render(poseStack, mouseX, mouseY, partialTicks);
        }
    }

    public class ListItem extends ConfigItem<List<?>>
    {
        private final Button button;

        public ListItem(IConfigValue<List<?>> holder)
        {
            super(holder);
            Component buttonText = ConfigScreen.this.config.isReadOnly() ? Component.translatable("configured.gui.view") : Component.translatable("configured.gui.edit");
            this.button = ScreenUtil.button(10, 5, 46, 20, buttonText, button -> Minecraft.getInstance().setScreen(new EditListScreen(ConfigScreen.this, ConfigScreen.this.config, this.label, holder, ConfigScreen.this.background)));
            this.eventListeners.add(this.button);
        }

        @Override
        public void render(GuiGraphics poseStack, int index, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            super.render(poseStack, index, top, left, width, p_230432_6_, mouseX, mouseY, hovered, partialTicks);
            this.button.setX(left + width - 69);
            this.button.setY(top);
            this.button.render(poseStack, mouseX, mouseY, partialTicks);
        }
    }

    public class EnumItem extends ConfigItem<Enum<?>>
    {
        private final Button button;

        public EnumItem(IConfigValue<Enum<?>> holder)
        {
            super(holder);
            Component buttonText = ConfigScreen.this.config.isReadOnly() ? Component.translatable("configured.gui.view") : Component.translatable("configured.gui.change");
            this.button = ScreenUtil.button(10, 5, 46, 20, buttonText, button -> Minecraft.getInstance().setScreen(new ChangeEnumScreen(ConfigScreen.this, ConfigScreen.this.config, this.label, ConfigScreen.this.background, holder.get(), holder, e -> {
                holder.set(e);
                ConfigScreen.this.updateButtons();
            })));
            this.eventListeners.add(this.button);
        }

        @Override
        public void render(GuiGraphics poseStack, int index, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            super.render(poseStack, index, top, left, width, p_230432_6_, mouseX, mouseY, hovered, partialTicks);
            this.button.setX(left + width - 69);
            this.button.setY(top);
            this.button.render(poseStack, mouseX, mouseY, partialTicks);
        }
    }

    /**
     * Tries to create a readable label from the given config value and spec. This will
     * first attempt to create a label from the translation key in the spec, otherwise it
     * will create a readable label from the raw config value name.
     *
     * @param holder the config value holder
     * @return a readable label string
     */
    private static String createLabelFromHolder(IConfigValue<?> holder)
    {
        if(holder.getTranslationKey() != null && I18n.exists(holder.getTranslationKey()))
        {
            return Component.translatable(holder.getTranslationKey()).getString();
        }
        return createLabel(holder.getName());
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
        return this.config == null || this.config.getType() != ConfigType.WORLD;
    }

    /**
     * Checks if the entry has been changed compared to its default value.
     *
     * @param entry the config entry to check
     * @return true if this entry is different from its default
     */
    public boolean isModified(IConfigEntry entry)
    {
        if(entry.isLeaf())
        {
            IConfigValue<?> value = entry.getValue();
            return value != null && !value.isDefault();
        }
        for(IConfigEntry child : entry.getChildren())
        {
            if(this.isModified(child)) return true;
        }
        return false;
    }

    /**
     * Checks if the entry has been changed compared to its initial value.
     *
     * @param entry the config entry to check
     * @return true if this entry is different from its initial value
     */
    public boolean isChanged(IConfigEntry entry)
    {
        if(entry.isLeaf())
        {
            IConfigValue<?> value = entry.getValue();
            return value != null && value.isChanged();
        }
        for(IConfigEntry child : entry.getChildren())
        {
            if(this.isChanged(child)) return true;
        }
        return false;
    }
}
