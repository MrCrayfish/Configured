package com.mrcrayfish.configured.client.screen;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mrcrayfish.configured.client.screen.widget.IconButton;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.list.AbstractOptionList;
import net.minecraft.client.gui.widget.list.ExtendedList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public class EditStringListScreen extends Screen
{
    private final Screen parent;
    private final List<StringHolder> values = new ArrayList<>();
    private final ForgeConfigSpec.ConfigValue<List<?>> listValue;
    private final ForgeConfigSpec.ValueSpec valueSpec;
    private final ResourceLocation background;
    private StringList list;

    public EditStringListScreen(Screen parent, ITextComponent titleIn, ForgeConfigSpec.ConfigValue<List<?>> listValue, ForgeConfigSpec.ValueSpec valueSpec, ResourceLocation background)
    {
        super(titleIn);
        this.parent = parent;
        this.listValue = listValue;
        this.valueSpec = valueSpec;
        this.values.addAll(listValue.get().stream().map(o -> new StringHolder(o.toString())).collect(Collectors.toList()));
        this.background = background;
    }

    @Override
    protected void init()
    {
        this.list = new StringList();
        this.list.func_244605_b(this.minecraft.world == null);
        this.children.add(this.list);
        this.addButton(new Button(this.width / 2 - 140, this.height - 29, 90, 20, DialogTexts.GUI_DONE, (button) -> {
            List<String> newValues = this.values.stream().map(StringHolder::getValue).collect(Collectors.toList());
            this.valueSpec.correct(newValues);
            this.listValue.set(newValues);
            this.minecraft.displayGuiScreen(this.parent);
        }));
        this.addButton(new Button(this.width / 2 - 45, this.height - 29, 90, 20, new TranslationTextComponent("configured.gui.add_value"), (button) -> {
            this.minecraft.displayGuiScreen(new EditStringScreen(EditStringListScreen.this, background, new TranslationTextComponent("configured.gui.edit_value"), "", o -> true, s -> {
                StringHolder holder = new StringHolder(s);
                this.values.add(holder);
                this.list.addEntry(new StringEntry(this.list, holder));
            }));
        }));
        this.addButton(new Button(this.width / 2 + 50, this.height - 29, 90, 20, DialogTexts.GUI_CANCEL, (button) -> {
            this.minecraft.displayGuiScreen(this.parent);
        }));
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(matrixStack);
        this.list.render(matrixStack, mouseX, mouseY, partialTicks);
        drawCenteredString(matrixStack, this.font, this.title, this.width / 2, 14, 0xFFFFFF);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @OnlyIn(Dist.CLIENT)
    public class StringList extends ExtendedList<StringEntry> implements IBackgroundTexture
    {
        public StringList()
        {
            super(EditStringListScreen.this.minecraft, EditStringListScreen.this.width, EditStringListScreen.this.height, 36, EditStringListScreen.this.height - 36, 24);
            EditStringListScreen.this.values.forEach(value -> {
                this.addEntry(new StringEntry(this, value));
            });
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
        public int addEntry(StringEntry entry)
        {
            return super.addEntry(entry);
        }

        @Override
        public boolean removeEntry(StringEntry entry)
        {
            return super.removeEntry(entry);
        }

        @Override
        public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
        {
            super.render(matrixStack, mouseX, mouseY, partialTicks);
            this.getEventListeners().forEach(entry ->
            {
                entry.getEventListeners().forEach(o ->
                {
                    if(o instanceof Button)
                    {
                        ((Button)o).renderToolTip(matrixStack, mouseX, mouseY);
                    }
                });
            });
        }

        @Override
        public ResourceLocation getBackgroundTexture()
        {
            return background;
        }
    }

    public class StringEntry extends AbstractOptionList.Entry<StringEntry>
    {
        private StringHolder holder;
        private final StringList list;
        private final Button editButton;
        private final Button deleteButton;

        public StringEntry(StringList list, StringHolder holder)
        {
            this.list = list;
            this.holder = holder;
            this.editButton = new Button(0, 0, 42, 20, new StringTextComponent("Edit"), onPress -> {
                EditStringListScreen.this.minecraft.displayGuiScreen(new EditStringScreen(EditStringListScreen.this, background, new TranslationTextComponent("configured.gui.edit_value"), this.holder.getValue(), o -> true, s -> {
                    this.holder.setValue(s);
                }));
            });
            Button.ITooltip tooltip = (button, matrixStack, mouseX, mouseY) -> {
                if(button.active && button.isHovered()) {
                    EditStringListScreen.this.renderTooltip(matrixStack, EditStringListScreen.this.minecraft.fontRenderer.trimStringToWidth(new TranslationTextComponent("configured.gui.remove"), Math.max(EditStringListScreen.this.width / 2 - 43, 170)), mouseX, mouseY);
                }
            };
            this.deleteButton = new IconButton(0, 0, 20, 20, 11, 0, tooltip, onPress -> {
                EditStringListScreen.this.values.remove(this.holder);
                this.list.removeEntry(this);
            });
        }

        @Override
        public void render(MatrixStack matrixStack, int x, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean selected, float partialTicks)
        {
            EditStringListScreen.this.minecraft.fontRenderer.func_243246_a(matrixStack, new StringTextComponent(this.holder.getValue()), left + 5, top + 6, 0xFFFFFF);
            this.editButton.visible = true;
            this.editButton.x = left + width - 65;
            this.editButton.y = top;
            this.editButton.render(matrixStack, mouseX, mouseY, partialTicks);
            this.deleteButton.visible = true;
            this.deleteButton.x = left + width - 21;
            this.deleteButton.y = top;
            this.deleteButton.render(matrixStack, mouseX, mouseY, partialTicks);
        }

        @Override
        public List<? extends IGuiEventListener> getEventListeners()
        {
            return ImmutableList.of(this.editButton, this.deleteButton);
        }
    }

    public static class StringHolder
    {
        private String value;

        public StringHolder(String value)
        {
            this.value = value;
        }

        public String getValue()
        {
            return this.value;
        }

        public void setValue(String value)
        {
            this.value = value;
        }
    }
}
