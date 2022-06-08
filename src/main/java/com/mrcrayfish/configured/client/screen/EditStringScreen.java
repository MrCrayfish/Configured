package com.mrcrayfish.configured.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Author: MrCrayfish
 */
public class EditStringScreen extends Screen implements IBackgroundTexture
{
    private final Screen parent;
    private final ResourceLocation background;
    private final String originalValue;
    private final Function<String, Boolean> validator;
    private final Consumer<String> onSave;
    private Button doneButton;
    private EditBox textField;

    protected EditStringScreen(Screen parent, ResourceLocation background, Component component, String originalValue, Function<String, Boolean> validator, Consumer<String> onSave)
    {
        super(component);
        this.parent = parent;
        this.background = background;
        this.originalValue = originalValue;
        this.validator = validator;
        this.onSave = onSave;
    }

    @Override
    protected void init()
    {
        this.textField = new EditBox(this.font, this.width / 2 - 150, this.height / 2 - 25, 300, 20, CommonComponents.EMPTY);
        this.textField.setMaxLength(32500);
        this.textField.setValue(this.originalValue);
        this.textField.setResponder(s -> this.updateValidation());
        this.addRenderableWidget(this.textField);

        this.doneButton = this.addRenderableWidget(new Button(this.width / 2 - 1 - 150, this.height / 2 + 3, 148, 20, CommonComponents.GUI_DONE, (button) -> {
            String text = this.textField.getValue();
            if(this.validator.apply(text)) {
                this.onSave.accept(text);
                this.minecraft.setScreen(this.parent);
            }
        }));
        this.addRenderableWidget(new Button(this.width / 2 + 3, this.height / 2 + 3, 148, 20, CommonComponents.GUI_CANCEL, (button) -> {
            this.minecraft.setScreen(this.parent);
        }));

        this.updateValidation();
    }

    @SuppressWarnings("ConstantConditions")
    protected void updateValidation()
    {
        boolean valid = this.validator.apply(this.textField.getValue());
        this.doneButton.active = valid;
        this.textField.setTextColor(valid || this.textField.getValue().isEmpty() ? ChatFormatting.WHITE.getColor() : ChatFormatting.RED.getColor());
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(poseStack);
        this.textField.render(poseStack, mouseX, mouseY, partialTicks);
        drawCenteredString(poseStack, this.font, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);
        super.render(poseStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public ResourceLocation getBackgroundTexture()
    {
        return this.background;
    }
}
