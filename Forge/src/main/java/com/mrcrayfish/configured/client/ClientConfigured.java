package com.mrcrayfish.configured.client;

import com.mojang.datafixers.util.Either;
import com.mrcrayfish.configured.Config;
import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.api.ModContext;
import com.mrcrayfish.configured.api.util.ConfigScreenHelper;
import com.mrcrayfish.configured.client.screen.TooltipScreen;
import com.mrcrayfish.configured.impl.simple.SimpleConfigManager;
import com.mrcrayfish.configured.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.ModListScreen;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.Set;

/**
 * Author: MrCrayfish
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public class ClientConfigured
{
    // This is where the magic happens
    public static void generateConfigFactories()
    {
        Constants.LOG.info("Creating config GUI factories...");
        ModList.get().forEachModContainer((modId, container) ->
        {
            // Ignore mods that already implement their own custom factory
            if(container.getCustomExtension(ConfigScreenHandler.ConfigScreenFactory.class).isPresent() && !Config.CLIENT.forceConfiguredMenu.get())
                return;

            Map<ConfigType, Set<IModConfig>> modConfigMap = ClientHandler.createConfigMap(new ModContext(modId));
            if(!modConfigMap.isEmpty()) // Only add if at least one config exists
            {
                int count = modConfigMap.values().stream().mapToInt(Set::size).sum();
                Constants.LOG.info("Registering config factory for mod {}. Found {} config(s)", modId, count);
                String displayName = container.getModInfo().getDisplayName();
                ResourceLocation backgroundTexture = Services.CONFIG.getBackgroundTexture(modId);
                container.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) ->
                {
                    return ConfigScreenHelper.createSelectionScreen(screen, Component.literal(displayName), modConfigMap, backgroundTexture);
                }));
            }
        });
    }

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event)
    {
        event.register(ClientHandler.KEY_OPEN_MOD_LIST);
    }

    public static void onRegisterTooltipComponentFactory(RegisterClientTooltipComponentFactoriesEvent event)
    {
        event.register(TooltipScreen.ListMenuTooltipComponent.class, TooltipScreen.ListMenuTooltipComponent::asClientTextTooltip);
    }

    @SubscribeEvent
    public static void onKeyPress(InputEvent.Key event)
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
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event)
    {
        SimpleConfigManager.getInstance().onClientDisconnect(event.getConnection());
    }

    @SubscribeEvent
    public static void onGatherTooltipComponents(RenderTooltipEvent.GatherComponents event)
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
    public static void onGetTooltipColor(RenderTooltipEvent.Color event)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if(!(minecraft.screen instanceof TooltipScreen screen))
            return;

        if(screen.tooltipText == null)
            return;

        if(screen.tooltipOutlineColour == null)
            return;

        event.setBorderStart(screen.tooltipOutlineColour);
        event.setBorderEnd(screen.tooltipOutlineColour);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenOpen(ScreenEvent.Opening event)
    {
        EditingTracker.instance().onScreenOpen(event.getScreen());
    }
}
