package com.mrcrayfish.configured.client.screen;

import com.electronwill.nightconfig.core.AbstractConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
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

import java.util.Collections;
import java.util.List;

/**
 * Author: MrCrayfish
 */
@OnlyIn(Dist.CLIENT)
public class ConfigScreen extends Screen
{
    private final Screen parent;
    private final ForgeConfigSpec spec;
    private ConfigList list;
    private List<Entry> entries;

    public ConfigScreen(Screen parent, String displayName, ForgeConfigSpec spec)
    {
        super(new StringTextComponent(displayName));
        this.parent = parent;
        this.spec = spec;
        this.constructEntries();
    }

    private void constructEntries()
    {
        ImmutableList.Builder<Entry> builder = ImmutableList.builder();
        this.spec.getValues().valueMap().forEach((s, o) ->
        {
            builder.add(new TitleEntry(StringUtils.capitalize(s)));
            AbstractConfig configValueConfig = (AbstractConfig) o;
            configValueConfig.valueMap().forEach((s1, o1) ->
            {
                ForgeConfigSpec.ConfigValue<?> configValue = (ForgeConfigSpec.ConfigValue<?>) o1;
                ForgeConfigSpec.ValueSpec spec = this.spec.getRaw(configValue.getPath());
                if(configValue instanceof ForgeConfigSpec.BooleanValue)
                {
                    builder.add(new BooleanEntry((ForgeConfigSpec.BooleanValue) configValue, spec));
                }
                else if(configValue instanceof ForgeConfigSpec.IntValue)
                {
                    builder.add(new IntegerEntry((ForgeConfigSpec.IntValue) configValue, spec));
                }
                else if(configValue instanceof ForgeConfigSpec.DoubleValue)
                {
                    builder.add(new DoubleEntry((ForgeConfigSpec.DoubleValue) configValue, spec));
                }
                else if(configValue instanceof ForgeConfigSpec.LongValue)
                {
                    builder.add(new LongEntry((ForgeConfigSpec.LongValue) configValue, spec));
                }
                else if(configValue instanceof ForgeConfigSpec.EnumValue)
                {
                    builder.add(new EnumEntry((ForgeConfigSpec.EnumValue) configValue, spec));
                }
                else
                {
                    builder.add(new StringEntry(configValue, spec));
                }
            });
        });
        this.entries = builder.build();
    }

    @Override
    protected void init()
    {
        this.list = new ConfigList(this.entries);
        this.children.add(this.list);
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(matrixStack);
        this.list.render(matrixStack, mouseX, mouseY, partialTicks);
        drawCenteredString(matrixStack, this.font, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    public abstract class Entry extends AbstractOptionList.Entry<Entry>
    {

    }

    public class TitleEntry extends Entry
    {
        private String title;

        public TitleEntry(String title)
        {
            this.title = title;
        }

        @Override
        public List<? extends IGuiEventListener> getEventListeners()
        {
            return Collections.emptyList();
        }

        @Override
        public void render(MatrixStack matrixStack, int x, int top, int left, int width, int p_230432_6_, int p_230432_7_, int p_230432_8_, boolean p_230432_9_, float p_230432_10_)
        {
            ITextComponent title = new StringTextComponent(this.title).mergeStyle(TextFormatting.BOLD).mergeStyle(TextFormatting.BLUE);
            AbstractGui.drawCenteredString(matrixStack, ConfigScreen.this.minecraft.fontRenderer, title, left + width / 2, top + 5, 16777215);
        }
    }

    public class SubMenu extends Entry
    {
        private String title;

        public SubMenu(String title)
        {
            this.title = title;
        }

        @Override
        public List<? extends IGuiEventListener> getEventListeners()
        {
            return Collections.emptyList();
        }

        @Override
        public void render(MatrixStack matrixStack, int x, int top, int left, int width, int p_230432_6_, int p_230432_7_, int p_230432_8_, boolean p_230432_9_, float p_230432_10_)
        {
            ITextComponent title = new StringTextComponent(this.title).mergeStyle(TextFormatting.BOLD).mergeStyle(TextFormatting.GRAY);
            ConfigScreen.this.minecraft.fontRenderer.func_243246_a(matrixStack, title, left - 15, top + 6, 0xFFFFFF);
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
            ITextComponent title = this.valueSpec.getTranslationKey() != null ? new TranslationTextComponent(this.valueSpec.getTranslationKey()) : new StringTextComponent(lastValue(this.configValue.getPath(), "YEP"));
            ConfigScreen.this.minecraft.fontRenderer.func_243246_a(matrixStack, title, left - 15, y + 6, 0xFFFFFF);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public class ConfigList extends AbstractOptionList<ConfigScreen.Entry>
    {
        public ConfigList(List<ConfigScreen.Entry> entries)
        {
            super(ConfigScreen.this.minecraft, ConfigScreen.this.width, ConfigScreen.this.height, 43, ConfigScreen.this.height - 32, 24);
            entries.forEach(this::addEntry);
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
}
