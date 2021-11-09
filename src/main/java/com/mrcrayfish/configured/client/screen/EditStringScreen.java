package com.mrcrayfish.configured.client.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.opengl.GL11;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Author: MrCrayfish
 */
public class EditStringScreen extends Screen
{
    private final Screen parent;
    private final ResourceLocation background;
    private TextFieldWidget textField;
    private String value;
    private final Function<Object, Boolean> validator;
    private final Consumer<String> onSave;

    protected EditStringScreen(Screen parent, ResourceLocation background, ITextComponent component, String value, Function<Object, Boolean> validator, Consumer<String> onSave)
    {
        super(component);
        this.parent = parent;
        this.background = background;
        this.value = value;
        this.validator = validator;
        this.onSave = onSave;
    }

    @Override
    protected void init()
    {
        this.textField = new TextFieldWidget(this.font, this.width / 2 - 150, this.height / 2 - 25, 300, 20, StringTextComponent.EMPTY);
        this.textField.setMaxStringLength(32500);
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

    @Override
    public void renderDirtBackground(int vOffset)
    {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        this.minecraft.getTextureManager().bindTexture(this.background);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        float size = 32.0F;
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        buffer.pos(0.0, this.height, 0.0).tex(0.0F, this.height / size + vOffset).color(64, 64, 64, 255).endVertex();
        buffer.pos(this.width, this.height, 0.0).tex(this.width / size, this.height / size + vOffset).color(64, 64, 64, 255).endVertex();
        buffer.pos(this.width, 0.0, 0.0).tex(this.width / size, vOffset).color(64, 64, 64, 255).endVertex();
        buffer.pos(0.0, 0.0, 0.0).tex(0.0F, vOffset).color(64, 64, 64, 255).endVertex();
        tessellator.draw();
        MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.BackgroundDrawnEvent(this, new MatrixStack()));
    }
}
