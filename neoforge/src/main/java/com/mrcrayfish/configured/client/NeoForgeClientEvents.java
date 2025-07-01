package com.mrcrayfish.configured.client;

import com.mojang.datafixers.util.Either;
import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.client.screen.TooltipScreen;
import com.mrcrayfish.configured.impl.framework.message.MessageFramework;
import com.mrcrayfish.configured.network.message.MessageSessionData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.ModListScreen;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Author: MrCrayfish
 */
@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class NeoForgeClientEvents
{
    @SubscribeEvent
    private static void onKeyPress(InputEvent.Key event)
    {
        if(event.getAction() == GLFW.GLFW_PRESS && ClientHandler.KEY_OPEN_MOD_LIST.isDown())
        {
            Minecraft minecraft = Minecraft.getInstance();
            if(minecraft.player == null)
                return;
            Screen oldScreen = minecraft.screen;
            minecraft.setScreen(new ModListScreen(oldScreen));
        }
    }

    @SubscribeEvent
    private static void onGatherTooltipComponents(RenderTooltipEvent.GatherComponents event)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if(!(minecraft.screen instanceof TooltipScreen screen))
            return;

        if(screen.tooltipText == null)
            return;

        event.getTooltipElements().clear();

        for(FormattedCharSequence text : screen.tooltipText)
        {
            event.getTooltipElements().add(Either.right(new TooltipScreen.ListMenuTooltipComponent(text)));
        }
    }

    @SubscribeEvent
    private static void onGetTooltipColor(RenderTooltipEvent.Texture event)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if(!(minecraft.screen instanceof TooltipScreen screen))
            return;

        if(screen.tooltipText == null)
            return;

        if(screen.tooltipStyle == null)
            return;

        event.setTexture(screen.tooltipStyle.getTexture());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenOpen(ScreenEvent.Opening event)
    {
        EditingTracker.instance().onScreenOpen(event.getScreen());
    }

    @SubscribeEvent
    private static void onRegisterClientPayloadHandler(RegisterClientPayloadHandlersEvent event)
    {
        event.register(MessageSessionData.TYPE, (payload, context) -> {
            MessageSessionData.handle(payload, context::enqueueWork);
        });

        if(ModList.get().isLoaded("framework"))
        {
            event.register(MessageFramework.Response.TYPE, (payload, context) -> {
                MessageFramework.Response.handle(payload, context::enqueueWork, context.player(), context::disconnect);
            });
        }
    }
}
