package com.mrcrayfish.configured.client;

import com.mrcrayfish.configured.impl.simple.SimpleConfigManager;
import com.mrcrayfish.configured.network.Channels;
import com.mrcrayfish.configured.network.ClientMessages;
import com.mrcrayfish.configured.network.HandshakeMessages;
import com.mrcrayfish.configured.network.message.MessageResponseSimpleConfig;
import com.mrcrayfish.configured.network.message.MessageSessionData;
import com.mrcrayfish.configured.network.message.MessageSyncSimpleConfig;
import com.mrcrayfish.configured.network.play.ClientPlayHandler;
import com.mrcrayfish.configured.network.play.ServerPlayHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletableFuture;

/**
 * Author: MrCrayfish
 */
public class ClientHandler implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        ClientLoginNetworking.registerGlobalReceiver(Channels.CONFIG_DATA, (client, handler, buf, listenerAdder) -> CompletableFuture.supplyAsync(() -> {
            HandshakeMessages.S2CConfigData message = HandshakeMessages.S2CConfigData.decode(buf);
            if(!SimpleConfigManager.getInstance().processConfigData(message)) {
                handler.getConnection().disconnect(Component.literal("Received invalid config data from server"));
            }
            return PacketByteBufs.create();
        }, client));

        EditingTracker.registerEvents();

        if(this.isModListInstalled())
        {
            KeyMapping keyOpenModList = new KeyMapping("key.configured.open_mod_list", -1, "key.categories.configured");
            KeyBindingHelper.registerKeyBinding(keyOpenModList);
            ClientTickEvents.END_CLIENT_TICK.register(client ->
            {
                while(keyOpenModList.consumeClick())
                {
                    this.openModList();
                }
            });
        }

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            SimpleConfigManager.getInstance().onClientDisconnect(handler.getConnection());
        });

        ClientMessages.register();
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
