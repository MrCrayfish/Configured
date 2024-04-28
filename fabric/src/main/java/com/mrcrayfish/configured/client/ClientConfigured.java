package com.mrcrayfish.configured.client;

import com.mrcrayfish.configured.impl.framework.message.MessageFramework;
import com.mrcrayfish.configured.network.message.MessageSessionData;
import com.mrcrayfish.configured.platform.Services;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Author: MrCrayfish
 */
public class ClientConfigured implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        ClientHandler.init();

        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            EditingTracker.instance().onScreenOpen(screen);
        });

        if(this.isModListInstalled())
        {
            KeyBindingHelper.registerKeyBinding(ClientHandler.KEY_OPEN_MOD_LIST);
            ClientTickEvents.END_CLIENT_TICK.register(client ->
            {
                while(ClientHandler.KEY_OPEN_MOD_LIST.consumeClick())
                {
                    this.openModList();
                }
            });
        }

        PayloadTypeRegistry.playC2S().register(MessageSessionData.TYPE, MessageSessionData.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(MessageSessionData.TYPE, MessageSessionData.STREAM_CODEC);
        ClientPlayNetworking.registerGlobalReceiver(MessageSessionData.TYPE, (payload, context) -> {
            MessageSessionData.handle(payload, Minecraft.getInstance()::execute);
        });

        if(Services.PLATFORM.isModLoaded("framework"))
        {
            PayloadTypeRegistry.playC2S().register(MessageFramework.Response.TYPE, MessageFramework.Response.STREAM_CODEC);
            PayloadTypeRegistry.playS2C().register(MessageFramework.Response.TYPE, MessageFramework.Response.STREAM_CODEC);
            ClientPlayNetworking.registerGlobalReceiver(MessageFramework.Response.TYPE, (payload, context) -> {
                Minecraft mc = context.client();
                MessageFramework.Response.handle(payload, mc::execute, mc.player, context.responseSender()::disconnect);
            });
        }
    }

    private boolean isModListInstalled()
    {
        return FabricLoader.getInstance().isModLoaded("catalogue") || FabricLoader.getInstance().isModLoaded("modmenu");
    }

    private void openModList()
    {
        Minecraft minecraft = Minecraft.getInstance();
        if(minecraft.level == null || minecraft.screen != null)
            return;
        Screen newScreen = null;
        if(FabricLoader.getInstance().isModLoaded("catalogue"))
        {
            newScreen = createModListScreen("com.mrcrayfish.catalogue.client.screen.CatalogueModListScreen");
        }
        else if(FabricLoader.getInstance().isModLoaded("modmenu"))
        {
            newScreen = createModListScreen("com.terraformersmc.modmenu.gui.ModsScreen");
        }
        if(newScreen != null)
        {
            minecraft.setScreen(newScreen);
        }
    }

    private Screen createModListScreen(String className)
    {
        try
        {
            Class<?> clazz = Class.forName(className);
            return (Screen) clazz.getDeclaredConstructor(Screen.class).newInstance(null);
        }
        catch(Exception e)
        {
            return null;
        }
    }
}
