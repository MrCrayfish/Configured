package com.mrcrayfish.configured.client.screen;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;

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
        this.textField = new TextFieldWidget(this.font, this.width / 2 - 150, this.height / 2 - 25, 300, 20, "");
        this.textField.setText(this.value);
        this.textField.setMaxStringLength(32500);
        this.children.add(this.textField);

        this.addButton(new Button(this.width / 2 - 1 - 150, this.height / 2 + 3, 148, 20, I18n.format("gui.done"), (button) -> {
            String text = this.textField.getText();
            if(this.validator.apply(text)) {
                this.onSave.accept(text);
                this.minecraft.displayGuiScreen(this.parent);
            }
        }));
        this.addButton(new Button(this.width / 2 + 3, this.height / 2 + 3, 148, 20, I18n.format("gui.cancel"), (button) -> {
            this.minecraft.displayGuiScreen(this.parent);
        }));
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground();
        this.textField.render(mouseX, mouseY, partialTicks);
        drawCenteredString(this.font, this.title.getFormattedText(), this.width / 2, this.height / 2 - 40, 0xFFFFFF);
        super.render(mouseX, mouseY, partialTicks);
    }
}
