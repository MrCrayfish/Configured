package com.mrcrayfish.configured.client.screen;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.client.screen.widget.IconButton;
import com.mrcrayfish.configured.client.util.ScreenUtil;
import joptsimple.internal.Strings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Author: MrCrayfish
 */
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

    protected final IConfigEntry folderEntry;
    protected IModConfig config;
    protected Button saveButton;
    protected Button restoreButton;

    private ConfigScreen(Screen parent, Component title, ResourceLocation background, IConfigEntry folderEntry)
    {
        super(parent, title, background, 24);
        this.folderEntry = folderEntry;
    }

    public ConfigScreen(Screen parent, Component title, IModConfig config, ResourceLocation background)
    {
        super(parent, title, background, 24);
        this.config = config;
        this.folderEntry = config.getRoot();
    }

	@Override
    @SuppressWarnings("unchecked")
    protected void constructEntries(List<Item> entries)
    {
        List<Item> configEntries = new ArrayList<>();
        this.folderEntry.getChildren().forEach(c ->
        {
            if(c.isLeaf())
            {
            	IConfigValue<?> entry = c.getValue();
            	if(entry == null) return;

                Object value = entry.get();
                if(value instanceof Boolean)
                {
                    configEntries.add(new BooleanItem((IConfigValue<Boolean>)entry));
                }
                else if(value instanceof Integer)
                {
                    configEntries.add(new IntegerItem((IConfigValue<Integer>)entry));
                }
                else if(value instanceof Double)
                {
                    configEntries.add(new DoubleItem((IConfigValue<Double>)entry));
                }
                else if(value instanceof Long)
                {
                    configEntries.add(new LongItem((IConfigValue<Long>)entry));
                }
                else if(value instanceof Enum)
                {
                    configEntries.add(new EnumItem((IConfigValue<Enum<?>>)entry));
                }
                else if(value instanceof String)
                {
                    configEntries.add(new StringItem((IConfigValue<String>)entry));
                }
                else if(value instanceof List<?>)
                {
                    configEntries.add(new ListItem((IConfigValue<List<?>>)entry));
                }
                else
                {
                    Configured.LOGGER.info("Unsupported config value: " + entry.getPath());
                }
            }
            else
            {
                configEntries.add(new FolderItem(c));
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
            this.saveButton = this.addRenderableWidget(new IconButton(this.width / 2 - 140, this.height - 29, 22, 0, 90, new TranslatableComponent("configured.gui.save"), (button) ->
            {
                if(this.config != null)
                {
                    this.saveConfig();
                }
                this.minecraft.setScreen(this.parent);
            }));
            this.restoreButton = this.addRenderableWidget(new IconButton(this.width / 2 - 45, this.height - 29, 0, 0, 90, new TranslatableComponent("configured.gui.reset_all"), (button) ->
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
                    this.minecraft.setScreen(new ConfirmationScreen(this, new TranslatableComponent("configured.gui.unsaved_changes"), result -> {
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
        if(!this.isChanged(this.folderEntry) || this.config == null)
            return;
        this.config.saveConfig(this.folderEntry);
    }

    private void showRestoreScreen()
    {
        ConfirmationScreen confirmScreen = new ConfirmationScreen(ConfigScreen.this, new TranslatableComponent("configured.gui.restore_message"), result ->
        {
            if(!result) return true;
            this.restoreDefaults(this.folderEntry);
            this.updateButtons();
            return true;
        });
        confirmScreen.setBackground(this.background);
        confirmScreen.setPositiveText(new TranslatableComponent("configured.gui.reset_all").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
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

        public FolderItem(IConfigEntry folderEntry)
        {
            super(new TextComponent(createLabel(folderEntry.getEntryName())));
            this.button = new Button(10, 5, 44, 20, new TextComponent(this.getLabel()).withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.WHITE), onPress -> {
                Component newTitle = ConfigScreen.this.title.copy().append(" > " + this.getLabel());
                ConfigScreen.this.minecraft.setScreen(new ConfigScreen(ConfigScreen.this, newTitle, ConfigScreen.this.background, folderEntry));
            });
        }

        @Override
        public List<? extends GuiEventListener> children()
        {
            return ImmutableList.of(this.button);
        }

        @Override
        public void render(PoseStack poseStack, int x, int top, int left, int width, int height, int mouseX, int mouseY, boolean selected, float partialTicks)
        {
            this.button.x = left - 1;
            this.button.y = top;
            this.button.setWidth(width);
            this.button.render(poseStack, mouseX, mouseY, partialTicks);
        }
    }

    public abstract class ConfigItem<T> extends Item
    {
        protected final IConfigValue<T> holder;
        protected final List<GuiEventListener> eventListeners = new ArrayList<>();
        protected final Button resetButton;

        public ConfigItem(IConfigValue<T> holder)
        {
            super(createLabelFromHolder(holder));
            this.holder = holder;
            if(this.holder.getComment() != null)
            {
                this.tooltip = this.createToolTip(holder);
            }
            int maxTooltipWidth = Math.max(ConfigScreen.this.width / 2 - 43, 170);
            Button.OnTooltip tooltip = ScreenUtil.createButtonTooltip(ConfigScreen.this, new TranslatableComponent("configured.gui.reset"), maxTooltipWidth);
            this.resetButton = new IconButton(0, 0, 0, 0, onPress -> {
                this.holder.restore();
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

            this.resetButton.active = !this.holder.isDefault();
            this.resetButton.x = left + width - 21;
            this.resetButton.y = top;
            this.resetButton.render(poseStack, mouseX, mouseY, partialTicks);
        }

        private Component getTrimmedLabel(int maxWidth)
        {
            if(ConfigScreen.this.minecraft.font.width(this.label) > maxWidth)
            {
                return new TextComponent(ConfigScreen.this.minecraft.font.substrByWidth(this.label, maxWidth).getString() + "...");
            }
            return this.label;
        }

        private List<FormattedCharSequence> createToolTip(IConfigValue<T> holder)
        {
            Font font = Minecraft.getInstance().font;
            List<FormattedText> lines = font.getSplitter().splitLines(new TextComponent(holder.getComment()), 200, Style.EMPTY);
            String name = holder.getPath();
            lines.add(0, new TextComponent(name).withStyle(ChatFormatting.YELLOW));
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
                    lines.set(i, new TextComponent(lines.get(i).getString()).withStyle(ChatFormatting.GRAY));
                }
            }
            return Language.getInstance().getVisualOrder(lines);
        }
    }

    public abstract class NumberItem<T extends Number> extends ConfigItem<T>
    {
        private final FocusedEditBox textField;

        @SuppressWarnings("unchecked")
        public NumberItem(IConfigValue<T> holder, Function<String, Number> parser)
        {
            super(holder);
            this.textField = new FocusedEditBox(ConfigScreen.this.font, 0, 0, 44, 18, this.label);
            this.textField.setValue(holder.get().toString());
            this.textField.setResponder((s) ->
            {
                try
                {
                    Number n = parser.apply(s);
                    if(holder.isValid((T) n))
                    {
                        this.textField.setTextColor(14737632);
                        holder.set((T) n);
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
            this.button = new Button(10, 5, 46, 20, CommonComponents.optionStatus(holder.get()), button ->
            {
                holder.set(!holder.get());
                button.setMessage(CommonComponents.optionStatus(holder.get()));
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
            this.button.setMessage(CommonComponents.optionStatus(this.holder.get()));
        }
    }

    public class StringItem extends ConfigItem<String>
    {
        private final Button button;

        public StringItem(IConfigValue<String> holder)
        {
            super(holder);
            this.button = new Button(10, 5, 46, 20, new TranslatableComponent("configured.gui.edit"), button -> Minecraft.getInstance().setScreen(new EditStringScreen(ConfigScreen.this, ConfigScreen.this.background, this.label, holder.get(), holder::isValid, s -> {
                holder.set(s);
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

        public ListItem(IConfigValue<List<?>> holder)
        {
            super(holder);
            this.button = new Button(10, 5, 46, 20, new TranslatableComponent("configured.gui.edit"), button -> Minecraft.getInstance().setScreen(new EditListScreen(ConfigScreen.this, this.label, holder, ConfigScreen.this.background)));
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

        public EnumItem(IConfigValue<Enum<?>> holder)
        {
            super(holder);
            this.button = new Button(10, 5, 46, 20, new TranslatableComponent("configured.gui.change"), button -> Minecraft.getInstance().setScreen(new ChangeEnumScreen(ConfigScreen.this, this.label, ConfigScreen.this.background, holder.get(), e -> {
                holder.set(e);
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
            return new TranslatableComponent(holder.getTranslationKey()).getString();
        }
        return createLabel(holder.getPath());
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
        return this.config == null || this.config.getConfigType() != ModConfig.Type.SERVER;
    }

    public boolean isModified(IConfigEntry entry)
    {
    	if(entry.isLeaf())
    	{
    		IConfigValue<?> value = entry.getValue();
    		return value != null && value.isDefault();
    	}
    	for(IConfigEntry child : entry.getChildren())
    	{
    		if(this.isChanged(child)) return true;
    	}
        return false;
    }

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
