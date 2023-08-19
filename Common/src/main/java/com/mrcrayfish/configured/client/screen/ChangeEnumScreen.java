package com.mrcrayfish.configured.client.screen;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mrcrayfish.configured.api.IAllowedEnums;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.client.util.ScreenUtil;
import com.mrcrayfish.configured.util.ConfigHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

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
public class ChangeEnumScreen extends TooltipScreen implements IBackgroundTexture, IEditing
{
    private final Screen parent;
    private final IModConfig config;
    private final Consumer<Enum<?>> onSave;
    private final ResourceLocation background;
    private final IConfigValue<Enum<?>> holder;
    private Enum<?> selectedValue;
    private EnumList list;
    private List<Entry> entries;
    private EditBox searchTextField;

    protected ChangeEnumScreen(Screen parent, IModConfig config, Component title, ResourceLocation background, Enum<?> value, IConfigValue<Enum<?>> holder, Consumer<Enum<?>> onSave)
    {
        super(title);
        this.parent = parent;
        this.config = config;
        this.onSave = onSave;
        this.background = background;
        this.holder = holder;
        this.selectedValue = value;
    }

    @Override
    protected void init()
    {
        this.constructEntries();
        this.list = new EnumList(this.entries);
        this.list.setRenderBackground(!ConfigHelper.isPlayingGame());
        this.list.setSelected(this.list.children().stream().filter(entry -> entry.getEnumValue() == this.selectedValue).findFirst().orElse(null));
        this.addWidget(this.list);

        this.searchTextField = new EditBox(this.font, this.width / 2 - 110, 22, 220, 20, Component.translatable("configured.gui.search"));
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

        if(!this.config.isReadOnly())
        {
            this.addRenderableWidget(ScreenUtil.button(this.width / 2 - 155, this.height - 29, 150, 20, CommonComponents.GUI_DONE, btn ->
            {
                if(this.list.getSelected() != null)
                {
                    this.onSave.accept(this.list.getSelected().enumValue);
                }
                this.minecraft.setScreen(this.parent);
            }));
        }

        int cancelOffset = this.config.isReadOnly() ? -75 : -155 + 160;
        Component cancelLabel = this.config.isReadOnly() ? CommonComponents.GUI_BACK : CommonComponents.GUI_CANCEL;
        this.addRenderableWidget(ScreenUtil.button(this.width / 2 + cancelOffset, this.height - 29, 150, 20, cancelLabel, button -> this.minecraft.setScreen(this.parent)));
    }

    private void constructEntries()
    {
        List<Entry> entries = new ArrayList<>();
        if(this.holder instanceof IAllowedEnums<?>)
        {
            ((IAllowedEnums<?>) this.holder).getAllowedValues().forEach(e -> entries.add(new Entry((Enum<?>) e)));
        }
        else
        {
            Enum<?> value = this.selectedValue;
            if(value != null)
            {
                Object[] enums = value.getDeclaringClass().getEnumConstants();
                for(Object e : enums)
                {
                    entries.add(new Entry((Enum<?>) e));
                }
            }
        }
        entries.sort(Comparator.comparing(entry -> entry.getFormattedLabel().getString()));
        this.entries = ImmutableList.copyOf(entries);
    }

    @Override
    public void render(GuiGraphics poseStack, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(poseStack);
        this.list.render(poseStack, mouseX, mouseY, partialTicks);
        this.searchTextField.render(poseStack, mouseX, mouseY, partialTicks);
        poseStack.drawCenteredString(this.font, this.title, this.width / 2, 7, 0xFFFFFF);
        super.render(poseStack, mouseX, mouseY, partialTicks);
        RenderSystem.setShaderTexture(0, ListMenuScreen.CONFIGURED_LOGO);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.blit(ListMenuScreen.CONFIGURED_LOGO, 10, 13, 0, 0, 0, 23, 23, 32, 32);
        if(ScreenUtil.isMouseWithin(10, 13, 23, 23, mouseX, mouseY))
        {
            this.setActiveTooltip(Component.translatable("configured.gui.info"));
        }
        this.drawTooltip(poseStack, mouseX, mouseY);
    }

    @Override
    public IModConfig getActiveConfig()
    {
        return this.config;
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
            return ChangeEnumScreen.this.background;
        }

        @Override
        public void updateNarration(NarrationElementOutput output)
        {
            if(this.getSelected() != null)
            {
                output.add(NarratedElementType.TITLE, this.getSelected().label);
            }
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY)
        {
            if(ChangeEnumScreen.this.config.isReadOnly())
                return false;
            return super.isMouseOver(mouseX, mouseY);
        }
    }

    public class Entry extends ObjectSelectionList.Entry<Entry> implements ILabelProvider
    {
        private final Enum<?> enumValue;
        private final Component label;

        public Entry(Enum<?> enumValue)
        {
            this.enumValue = enumValue;
            this.label = Component.literal(ConfigScreen.createLabel(enumValue.name().toLowerCase(Locale.ENGLISH)));
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
        public void render(GuiGraphics poseStack, int index, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean hovered, float partialTicks)
        {
            Component label = Component.literal(this.label.getString()).withStyle(ChangeEnumScreen.this.list.getSelected() == this ? ChatFormatting.YELLOW : ChatFormatting.WHITE);
            poseStack.drawString(ChangeEnumScreen.this.minecraft.font, label, left + 5, top + 4, 0xFFFFFF);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button)
        {
            ChangeEnumScreen.this.list.setSelected(this);
            ChangeEnumScreen.this.selectedValue = this.enumValue;
            return true;
        }

        @Override
        public Component getNarration()
        {
            return this.label;
        }
    }
}
