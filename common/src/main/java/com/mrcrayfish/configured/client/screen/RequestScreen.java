package com.mrcrayfish.configured.client.screen;

import com.mrcrayfish.configured.api.ActionResult;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.client.util.ScreenUtil;
import net.minecraft.util.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Author: MrCrayfish
 */
public class RequestScreen extends ListMenuScreen implements IEditing
{
    private static final Component REQUESTING_LABEL = Component.translatable("configured.gui.requesting_config");
    private static final Component FAILED_LABEL = Component.translatable("configured.gui.failed_request");
    private static final int TIMEOUT = 20 * 5; // 5 Seconds

    private int time;
    private boolean requested;
    private boolean failed;
    private Component message = null;
    private final IModConfig config;
    private final Runnable task;
    private IModConfig response;

    protected RequestScreen(Screen parent, Component title, IModConfig config)
    {
        super(parent, title, 20);
        this.config = config;
        this.task = config.requestFromServerTask().orElseThrow(() -> {
            return new IllegalArgumentException("Cannot supply a non-requestable config to be requested");
        });
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
            // Permission is also checked on the server, this is just for client experience
            ActionResult permission = this.config.canPlayerEdit(this.minecraft.player);
            if(permission.asBoolean())
            {
                this.task.run();
            }
            else
            {
                this.handleResponse(null, Component.translatable("configured.gui.no_permission"));
            }
            this.requested = true;
        }
        this.addRenderableWidget(ScreenUtil.button(this.width / 2 - 75, this.height - 29, 150, 20, CommonComponents.GUI_CANCEL, button -> this.minecraft.setScreen(this.parent)));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float deltaTick)
    {
        super.render(graphics, mouseX, mouseY, deltaTick);
        if(this.failed)
        {
            graphics.drawCenteredString(this.font, this.message != null ? this.message : FAILED_LABEL, this.width / 2, this.height / 2, 8421504);
        }
        else if(this.requested)
        {
            String label = switch((int) (Util.getMillis() / 300L % 4L)) {
                default -> "O o o";
                case 1, 3 -> "o O o";
                case 2 -> "o o O";
            };
            graphics.drawCenteredString(this.font, REQUESTING_LABEL, this.width / 2, this.height / 2 - this.font.lineHeight, 0xFFFFFFFF);
            graphics.drawCenteredString(this.font, label, this.width / 2, this.height / 2 + 5, 8421504);
        }
    }

    @Override
    public void tick()
    {
        if(!this.failed && this.time++ >= TIMEOUT)
        {
            this.failed = true;
        }

        if(!this.failed && this.response != null && this.time >= 10)
        {
            this.minecraft.setScreen(new ConfigScreen(this.parent, this.title, this.response));
            this.response = null;
        }
    }

    public void handleResponse(@Nullable IModConfig config, @Nullable Component message)
    {
        if(this.failed)
            return;

        if(config != null)
        {
            this.response = config;
            return;
        }

        this.failed = true;
        this.message = message;
    }
}
