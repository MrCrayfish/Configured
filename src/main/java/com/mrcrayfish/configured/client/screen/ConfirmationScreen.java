package com.mrcrayfish.configured.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/**
 * A simple versatile confirmation screen
 * <p>
 * Author: MrCrayfish
 */
public class ConfirmationScreen extends Screen
{
    private final Screen parent;
    private final Component message;
    private final Consumer<Boolean> handler;
    private Component positiveText = CommonComponents.GUI_YES;
    private Component negativeText = CommonComponents.GUI_NO;
    private ResourceLocation background = Screen.BACKGROUND_LOCATION;

    public ConfirmationScreen(Screen parent, TranslatableComponent message, Consumer<Boolean> handler)
    {
        super(message);
        this.parent = parent;
        this.message = message;
        this.handler = handler;
    }

    @Override
    protected void init()
    {
        this.addRenderableWidget(new Button(this.width / 2 - 105, this.height / 2, 100, 20, this.positiveText, button -> {
            this.handler.accept(true);
            this.minecraft.setScreen(this.parent);
        }));
        this.addRenderableWidget(new Button(this.width / 2 + 5, this.height / 2, 100, 20, this.negativeText, button -> {
            this.handler.accept(false);
            this.minecraft.setScreen(this.parent);
        }));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
    {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTicks);
        drawCenteredString(poseStack, this.font, this.message, this.width / 2, this.height / 2 - 20, 0xFFFFFF);
    }

    @Override
    public void renderDirtBackground(int vOffset)
    {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder builder = tesselator.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, this.background);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        float size = 32.0F;
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        builder.vertex(0.0D, this.height, 0.0D).uv(0.0F, this.height / size + vOffset).color(64, 64, 64, 255).endVertex();
        builder.vertex(this.width, this.height, 0.0D).uv(this.width / size, this.height / size + vOffset).color(64, 64, 64, 255).endVertex();
        builder.vertex(this.width, 0.0D, 0.0D).uv(this.width / size, vOffset).color(64, 64, 64, 255).endVertex();
        builder.vertex(0.0D, 0.0D, 0.0D).uv(0.0F, vOffset).color(64, 64, 64, 255).endVertex();
        tesselator.end();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.GuiScreenEvent.BackgroundDrawnEvent(this, new PoseStack()));
    }

    /**
     * Sets the text for the positive button. This must be called before the screen is displayed.
     *
     * @param positiveText the text component to display as the positive button label
     */
    public void setPositiveText(Component positiveText)
    {
        this.positiveText = positiveText;
    }

    /**
     * Sets the text for the negative button. This must be called before the screen is displayed.
     *
     * @param negativeText the text component to display as the negative button label
     */
    public void setNegativeText(Component negativeText)
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
