package com.mrcrayfish.configured.client;

import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.impl.simple.SimpleConfigManager;
import com.mrcrayfish.configured.network.Channels;
import com.mrcrayfish.configured.network.HandshakeMessages;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.CompletableFuture;

/**
 * Author: MrCrayfish
 */
public class ClientHandler implements ClientModInitializer
{
    public static final KeyMapping KEY_OPEN_MOD_LIST = new KeyMapping("key.configured.open_mod_list", -1, "key.categories.configured");

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

        KeyBindingHelper.registerKeyBinding(KEY_OPEN_MOD_LIST);
        EditingTracker.registerEvents();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while(KEY_OPEN_MOD_LIST.consumeClick()) {
                //TODO open mod list? Catalogue or Modmenu
                /*Minecraft minecraft = Minecraft.getInstance();
                if(minecraft.player == null)
                    return;
                Screen oldScreen = minecraft.screen;
                minecraft.setScreen(new ModListScreen(oldScreen));*/
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            SimpleConfigManager.getInstance().onClientDisconnect(handler.getConnection());
        });
    }
}
