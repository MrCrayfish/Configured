package com.mrcrayfish.configured.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mrcrayfish.configured.api.IModConfig;
import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Author: MrCrayfish
 */
public class RequestScreen extends ListMenuScreen implements IEditing
{
    private static final Component REQUESTING_LABEL = new TranslatableComponent("configured.gui.requesting_config");
    private static final Component FAILED_LABEL = new TranslatableComponent("configured.gui.failed_request");
    private static final int TIMEOUT = 20 * 5; // 5 Seconds

    private int time;
    private boolean requested;
    private boolean failed;
    private final IModConfig config;

    protected RequestScreen(Screen parent, Component title, ResourceLocation background, IModConfig config)
    {
        super(parent, title, background, 20);
        this.config = config;
    }

    @Override
    public IModConfig getActiveConfig()
    {
        return this.config;
    }

    @Override
    protected void constructEntries(List<Item> entries) {}

    @Override
    protected void init()
    {
        super.init();
        if(!this.requested)
        {
            this.config.requestFromServer();
            this.requested = true;
        }
        this.addRenderableWidget(new Button(this.width / 2 - 75, this.height - 29, 150, 20, CommonComponents.GUI_CANCEL, button -> this.minecraft.setScreen(this.parent)));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float deltaTick)
    {
        super.render(poseStack, mouseX, mouseY, deltaTick);
        if(this.failed)
        {
            drawCenteredString(poseStack, this.font, FAILED_LABEL, this.width / 2, this.height / 2, 8421504);
        }
        else if(this.requested)
        {
            String label = switch((int) (Util.getMillis() / 300L % 4L)) {
                default -> "O o o";
                case 1, 3 -> "o O o";
                case 2 -> "o o O";
            };
            drawCenteredString(poseStack, this.font, REQUESTING_LABEL, this.width / 2, this.height / 2 - this.font.lineHeight, 0xFFFFFFFF);
            drawCenteredString(poseStack, this.font, label, this.width / 2, this.height / 2 + 5, 8421504);
        }
    }

    @Override
    public void tick()
    {
        if(!this.failed && this.time++ >= TIMEOUT)
        {
            this.failed = true;
        }
    }

    public void handleResponse(@Nullable IModConfig config)
    {
        if(config != null)
        {
            this.minecraft.setScreen(new ConfigScreen(this.parent, this.title, config, this.background));
        }
        else
        {
            this.failed = true;
        }
    }
}
