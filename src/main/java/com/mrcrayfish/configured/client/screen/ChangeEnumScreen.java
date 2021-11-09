package com.mrcrayfish.configured.client.screen;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mrcrayfish.configured.client.util.ConfigUtil;
import com.mrcrayfish.configured.client.util.ScreenUtil;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.list.ExtendedList;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public class ChangeEnumScreen extends Screen
{
    private final Screen parent;
    private final ForgeConfigSpec.ConfigValue<Enum> configValue;
    private EnumList list;
    private List<Entry> entries;
    private TextFieldWidget searchTextField;
    private List<IReorderingProcessor> activeTooltip;

    protected ChangeEnumScreen(Screen parent, ITextComponent title, ResourceLocation background, ForgeConfigSpec.ConfigValue<Enum> configValue)
    {
        super(title);
        this.parent = parent;
        this.configValue = configValue;
    }

    @Override
    protected void init()
    {
        this.constructEntries();
        this.list = new EnumList(this.entries);
        this.list.setSelected(this.list.getEventListeners().stream().filter(entry -> entry.getEnumValue() == this.configValue.get()).findFirst().orElse(null));
        this.children.add(this.list);

        this.searchTextField = new TextFieldWidget(this.font, this.width / 2 - 110, 22, 220, 20, new StringTextComponent("Search"));
        this.searchTextField.setResponder(s -> {
            this.updateSearchField(s);
            this.list.replaceEntries(s.isEmpty() ? this.entries : this.entries.stream().filter(entry -> entry.getLabel().getString().toLowerCase(Locale.ENGLISH).contains(s.toLowerCase(Locale.ENGLISH))).collect(Collectors.toList()));
            if(!s.isEmpty())
            {
                this.list.setScrollAmount(0);
            }
        });
        this.children.add(this.searchTextField);
        this.updateSearchField(this.searchTextField.getText());

        this.addButton(new Button(this.width / 2 - 155, this.height - 29, 150, 20, DialogTexts.GUI_DONE, (button) -> {
            if(this.list.getSelected() != null) {
                this.configValue.set(this.list.getSelected().enumValue);
            }
            this.minecraft.displayGuiScreen(this.parent);
        }));

        this.addButton(new Button(this.width / 2 - 155 + 160, this.height - 29, 150, 20, DialogTexts.GUI_CANCEL, (button) -> {
            this.minecraft.displayGuiScreen(this.parent);
        }));
    }

    private void constructEntries()
    {
        List<Entry> entries = new ArrayList<>();
        Object value = this.configValue.get();
        if(value instanceof Enum)
        {
            Object[] enums = ((Enum) value).getDeclaringClass().getEnumConstants();
            for(Object e : enums)
            {
                entries.add(new Entry((Enum) e));
            }
        }
        this.entries = ImmutableList.copyOf(entries);
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
        this.minecraft.getTextureManager().bindTexture(ConfigScreen.LOGO_TEXTURE);
        blit(matrixStack, 10, 13, this.getBlitOffset(), 0, 0, 23, 23, 32, 32);
        if(ScreenUtil.isMouseWithin(10, 13, 23, 23, mouseX, mouseY))
        {
            this.activeTooltip = this.minecraft.fontRenderer.trimStringToWidth(new TranslationTextComponent("configured.gui.info"), 200);
        }
        if(this.activeTooltip != null)
        {
            this.renderTooltip(matrixStack, this.activeTooltip, mouseX, mouseY);
        }
    }

    private void updateSearchField(String value)
    {
        if(value.isEmpty())
        {
            this.searchTextField.setSuggestion(new TranslationTextComponent("configured.gui.search").getString());
        }
        else
        {
            Optional<Entry> optional = this.entries.stream().filter(info -> {
                return info.getLabel().getString().toLowerCase(Locale.ENGLISH).startsWith(value.toLowerCase(Locale.ENGLISH));
            }).min(Comparator.comparing(entry -> entry.getLabel().getString()));
            if(optional.isPresent())
            {
                int length = value.length();
                String displayName = optional.get().getLabel().getString();
                this.searchTextField.setSuggestion(displayName.substring(length));
            }
            else
            {
                this.searchTextField.setSuggestion("");
            }
        }
    }

    public class EnumList extends ExtendedList<Entry>
    {
        public EnumList(List<ChangeEnumScreen.Entry> entries)
        {
            super(ChangeEnumScreen.this.minecraft, ChangeEnumScreen.this.width, ChangeEnumScreen.this.height, 50, ChangeEnumScreen.this.height - 36, 20);
            entries.forEach(this::addEntry);
        }

        @Override
        public void replaceEntries(Collection<Entry> entries)
        {
            super.replaceEntries(entries);
        }
    }

    public class Entry extends ExtendedList.AbstractListEntry<Entry>
    {
        private final Enum enumValue;
        private StringTextComponent label;

        public Entry(Enum enumValue)
        {
            this.enumValue = enumValue;
            this.label = new StringTextComponent(ConfigScreen.createLabel(enumValue.name().toLowerCase(Locale.ENGLISH)));
        }

        public Enum getEnumValue()
        {
            return enumValue;
        }

        public StringTextComponent getLabel()
        {
            return this.label;
        }

        @Override
        public void render(MatrixStack matrixStack, int index, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            ITextComponent label = new StringTextComponent(this.label.getUnformattedComponentText()).mergeStyle(ChangeEnumScreen.this.list.getSelected() == this ? TextFormatting.YELLOW : TextFormatting.WHITE);
            AbstractGui.drawString(matrixStack, ChangeEnumScreen.this.minecraft.fontRenderer, label, left + 5, top + 4, 0xFFFFFF);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button)
        {
            ChangeEnumScreen.this.list.setSelected(this);
            return true;
        }
    }
}
