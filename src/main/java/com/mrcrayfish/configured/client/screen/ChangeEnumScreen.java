package com.mrcrayfish.configured.client.screen;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mrcrayfish.configured.client.util.ScreenUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public class ChangeEnumScreen extends Screen implements IBackgroundTexture
{
    private final Screen parent;
    private final Consumer<Enum<?>> onSave;
    private final ResourceLocation background;
    private Enum<?> selectedValue;
    private EnumList list;
    private List<Entry> entries;
    private EditBox searchTextField;
    private List<FormattedCharSequence> activeTooltip;

    protected ChangeEnumScreen(Screen parent, Component title, ResourceLocation background, Enum<?> value, Consumer<Enum<?>> onSave)
    {
        super(title);
        this.parent = parent;
        this.onSave = onSave;
        this.background = background;
        this.selectedValue = value;
    }

    @Override
    protected void init()
    {
        this.constructEntries();
        this.list = new EnumList(this.entries);
        this.list.setRenderBackground(!ListMenuScreen.isPlayingGame());
        this.list.setSelected(this.list.children().stream().filter(entry -> entry.getEnumValue() == this.selectedValue).findFirst().orElse(null));
        this.addWidget(this.list);

        this.searchTextField = new EditBox(this.font, this.width / 2 - 110, 22, 220, 20, new TextComponent("Search"));
        this.searchTextField.setResponder(s ->
        {
            ScreenUtil.updateSearchTextFieldSuggestion(this.searchTextField, s, this.entries);
            this.list.replaceEntries(s.isEmpty() ? this.entries : this.entries.stream().filter(entry -> entry.getFormattedLabel().getString().toLowerCase(Locale.ENGLISH).contains(s.toLowerCase(Locale.ENGLISH))).collect(Collectors.toList()));
            if(!s.isEmpty())
            {
                this.list.setScrollAmount(0);
            }
        });
        this.addWidget(this.searchTextField);
        ScreenUtil.updateSearchTextFieldSuggestion(this.searchTextField, "", this.entries);

        this.addRenderableWidget(new Button(this.width / 2 - 155, this.height - 29, 150, 20, CommonComponents.GUI_DONE, button ->
        {
            if(this.list.getSelected() != null)
            {
                this.onSave.accept(this.list.getSelected().enumValue);
            }
            this.minecraft.setScreen(this.parent);
        }));

        this.addRenderableWidget(new Button(this.width / 2 - 155 + 160, this.height - 29, 150, 20, CommonComponents.GUI_CANCEL, button -> this.minecraft.setScreen(this.parent)));
    }

    private void constructEntries()
    {
        List<Entry> entries = new ArrayList<>();
        Object value = this.selectedValue;
        if(value != null)
        {
            Object[] enums = ((Enum<?>) value).getDeclaringClass().getEnumConstants();
            for(Object e : enums)
            {
                entries.add(new Entry((Enum<?>) e));
            }
        }
        entries.sort(Comparator.comparing(entry -> entry.getFormattedLabel().getString()));
        this.entries = ImmutableList.copyOf(entries);
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
        RenderSystem.setShaderTexture(0, ListMenuScreen.CONFIGURED_LOGO);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        blit(poseStack, 10, 13, this.getBlitOffset(), 0, 0, 23, 23, 32, 32);
        if(ScreenUtil.isMouseWithin(10, 13, 23, 23, mouseX, mouseY))
        {
            this.activeTooltip = this.minecraft.font.split(new TranslatableComponent("configured.gui.info"), 200);
        }
        if(this.activeTooltip != null)
        {
            this.renderTooltip(poseStack, this.activeTooltip, mouseX, mouseY);
        }
    }

    @Override
    public ResourceLocation getBackgroundTexture()
    {
        return this.background;
    }

    public class EnumList extends AbstractSelectionList<Entry> implements IBackgroundTexture
    {
        public EnumList(List<ChangeEnumScreen.Entry> entries)
        {
            super(ChangeEnumScreen.this.minecraft, ChangeEnumScreen.this.width, ChangeEnumScreen.this.height, 50, ChangeEnumScreen.this.height - 36, 20);
            entries.forEach(this::addEntry);
        }

        @Override
        public void replaceEntries(Collection<ChangeEnumScreen.Entry> entries)
        {
            super.replaceEntries(entries);
        }

        @Override
        public ResourceLocation getBackgroundTexture()
        {
            return background;
        }

        @Override
        public void updateNarration(NarrationElementOutput output)
        {
            if(this.getSelected() != null)
            {
                output.add(NarratedElementType.TITLE, this.getSelected().label);
            }
        }
    }

    public class Entry extends AbstractSelectionList.Entry<Entry> implements ILabelProvider
    {
        private final Enum<?> enumValue;
        private final Component label;

        public Entry(Enum<?> enumValue)
        {
            this.enumValue = enumValue;
            this.label = new TextComponent(ConfigScreen.createLabel(enumValue.name().toLowerCase(Locale.ENGLISH)));
        }

        public Enum<?> getEnumValue()
        {
            return this.enumValue;
        }

        @Override
        public String getLabel()
        {
            return this.label.getString();
        }

        public Component getFormattedLabel()
        {
            return this.label;
        }

        @Override
        public void render(PoseStack poseStack, int index, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            Component label = new TextComponent(this.label.getString()).withStyle(ChangeEnumScreen.this.list.getSelected() == this ? ChatFormatting.YELLOW : ChatFormatting.WHITE);
            Screen.drawString(poseStack, ChangeEnumScreen.this.minecraft.font, label, left + 5, top + 4, 0xFFFFFF);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button)
        {
            ChangeEnumScreen.this.list.setSelected(this);
            ChangeEnumScreen.this.selectedValue = this.enumValue;
            return true;
        }
    }
}
