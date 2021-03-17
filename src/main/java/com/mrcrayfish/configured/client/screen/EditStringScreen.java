package com.mrcrayfish.configured.client.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Author: MrCrayfish
 */
public class EditStringScreen extends Screen
{
    private final Screen parent;
    private TextFieldWidget textField;
    private String value;
    private final Function<Object, Boolean> validator;
    private final Consumer<String> onSave;

    protected EditStringScreen(Screen parent, ITextComponent component, String value, Function<Object, Boolean> validator, Consumer<String> onSave)
    {
        super(component);
        this.parent = parent;
        this.value = value;
        this.validator = validator;
        this.onSave = onSave;
    }

    @Override
    protected void init()
    {
        this.textField = new TextFieldWidget(this.font, this.width / 2 - 150, this.height / 2 - 25, 300, 20, StringTextComponent.EMPTY);
        this.textField.setText(this.value);
        this.children.add(this.textField);

        this.addButton(new Button(this.width / 2 - 1 - 150, this.height / 2 + 3, 148, 20, DialogTexts.GUI_DONE, (button) -> {
            String text = this.textField.getText();
            if(this.validator.apply(text)) {
                this.onSave.accept(text);
                this.minecraft.displayGuiScreen(this.parent);
            }
        }));
        this.addButton(new Button(this.width / 2 + 3, this.height / 2 + 3, 148, 20, DialogTexts.GUI_CANCEL, (button) -> {
            this.minecraft.displayGuiScreen(this.parent);
        }));
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(matrixStack);
        this.textField.render(matrixStack, mouseX, mouseY, partialTicks);
        drawCenteredString(matrixStack, this.font, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }
}
