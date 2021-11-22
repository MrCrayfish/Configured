package com.mrcrayfish.configured.client.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import java.util.List;
import java.util.function.Function;

/**
 * A simple versatile confirmation screen
 * <p>
 * Author: MrCrayfish
 */
public class ConfirmationScreen extends Screen implements IBackgroundTexture
{
    private final Screen parent;
    private final ITextComponent message;
    private final Function<Boolean, Boolean> handler;
    private ITextComponent positiveText = DialogTexts.GUI_YES;
    private ITextComponent negativeText = DialogTexts.GUI_NO;
    private ResourceLocation background = AbstractGui.BACKGROUND_LOCATION;

    public ConfirmationScreen(Screen parent, ITextComponent message, Function<Boolean, Boolean> handler)
    {
        super(message);
        this.parent = parent;
        this.message = message;
        this.handler = handler;
    }

    @Override
    protected void init()
    {
        List<IReorderingProcessor> lines = this.font.trimStringToWidth(this.message, 300);
        int messageOffset = (lines.size() * (this.font.FONT_HEIGHT + 2)) / 2;
        this.addButton(new Button(this.width / 2 - 105, this.height / 2 + messageOffset, 100, 20, this.positiveText, button ->
        {
            if(this.handler.apply(true))
            {
                this.minecraft.displayGuiScreen(this.parent);
            }
        }));
        this.addButton(new Button(this.width / 2 + 5, this.height / 2 + messageOffset, 100, 20, this.negativeText, button ->
        {
            if(this.handler.apply(false))
            {
                this.minecraft.displayGuiScreen(this.parent);
            }
        }));
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        List<IReorderingProcessor> lines = this.font.trimStringToWidth(this.message, 300);
        for(int i = 0; i < lines.size(); i++)
        {
            int lineWidth = this.font.func_243245_a(lines.get(i));
            this.font.func_238422_b_(matrixStack, lines.get(i), this.width / 2 - lineWidth / 2, this.height / 2 - 20 - (lines.size() * (this.font.FONT_HEIGHT + 2)) / 2 + i * (this.font.FONT_HEIGHT + 2), 0xFFFFFF);
        }
    }

    @Override
    public ResourceLocation getBackgroundTexture()
    {
        return this.background;
    }

    /**
     * Sets the text for the positive button. This must be called before the screen is displayed.
     *
     * @param positiveText the text component to display as the positive button label
     */
    public void setPositiveText(ITextComponent positiveText)
    {
        this.positiveText = positiveText;
    }

    /**
     * Sets the text for the negative button. This must be called before the screen is displayed.
     *
     * @param negativeText the text component to display as the negative button label
     */
    public void setNegativeText(ITextComponent negativeText)
    {
        this.negativeText = negativeText;
    }

    /**
     * Sets the image to use as the background for this screen
     *
     * @param background a resource location pointing to a texture
     */
    public void setBackground(ResourceLocation background)
    {
        this.background = background;
    }
}
