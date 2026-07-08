package com.mrcrayfish.configured.client.screen;

import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.client.screen.widget.IconButton;
import com.mrcrayfish.configured.client.util.ScreenUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Author: MrCrayfish
 */
public class EditStringScreen extends TooltipScreen implements IEditing
{
    private final Screen parent;
    private final IModConfig config;
    private final String originalValue;
    private final Function<String, Pair<Boolean, Component>> validator;
    private final Consumer<String> onSave;
    private Button doneButton;
    private EditBox textField;
    private Component validationHint;
    private String value;

    protected EditStringScreen(Screen parent, IModConfig config, Component component, String originalValue, Function<String, Pair<Boolean, Component>> validator, Consumer<String> onSave)
    {
        super(component);
        this.parent = parent;
        this.config = config;
        this.originalValue = originalValue;
        this.validator = validator;
        this.onSave = onSave;
        this.value = this.originalValue;
    }

    @Override
    protected void init()
    {
        this.textField = new EditBox(this.font, this.width / 2 - 130, this.height / 2 - 25, 260, 20, CommonComponents.EMPTY);
        this.textField.setMaxLength(32500);
        this.textField.setValue(this.value);
        this.textField.setResponder(s -> {
            this.value = s;
            this.updateValidation();
        });
        this.textField.setEditable(!this.config.isReadOnly());
        this.addRenderableWidget(this.textField);

        this.doneButton = this.addRenderableWidget(new IconButton(this.width / 2 - 1 - 130, this.height / 2 + 13, 0, 44, 128, Component.translatable("configured.gui.apply"), (button) -> {
            String text = this.textField.getValue();
            if(this.validator.apply(text).getLeft()) {
                this.onSave.accept(text);
                this.minecraft.gui.setScreen(this.parent);
            }
        }));
        this.addRenderableWidget(ScreenUtil.button(this.width / 2 + 3, this.height / 2 + 13, 128, 20, CommonComponents.GUI_CANCEL, (button) -> {
            this.minecraft.gui.setScreen(this.parent);
        }));

        this.updateValidation();
    }

    @SuppressWarnings("ConstantConditions")
    protected void updateValidation()
    {
        Pair<Boolean, Component> result = this.validator.apply(this.textField.getValue());
        boolean valid = result.getLeft();
        this.doneButton.active = !this.config.isReadOnly() && valid;
        this.textField.setTextColor(valid || this.textField.getValue().isEmpty() ? 0xFFFFFFFF : 0xFFFF0000);
        this.validationHint = !valid ? result.getRight() : null;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTicks)
    {
        this.resetTooltip();

        super.extractRenderState(extractor, mouseX, mouseY, partialTicks);
        ConfirmationScreen.extractListBackground(extractor, 0, this.width, this.textField.getY() - 10, this.textField.getY() + 20 + 10);
        this.textField.extractRenderState(extractor, mouseX, mouseY, partialTicks);
        extractor.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 50, 0xFFFFFFFF);

        boolean showValidationHint = this.validationHint != null;
        if(showValidationHint)
        {
            extractor.blit(RenderPipelines.GUI_TEXTURED, IconButton.ICONS, this.textField.getX() - 20, this.textField.getY() + 3, 11, 11, 16, 16, 11, 11, 64, 64);

            if(ScreenUtil.isMouseWithin(this.textField.getX() - 20, this.textField.getY() + 3, 16, 16, mouseX, mouseY))
            {
                this.setActiveTooltip(extractor, this.validationHint, mouseX, mouseY, TooltipStyle.ERROR);
            }
        }
    }

    @Override
    public IModConfig getActiveConfig()
    {
        return this.config;
    }
}
