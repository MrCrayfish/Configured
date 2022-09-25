package com.mrcrayfish.configured.client;

import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.api.IConfigProvider;
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
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Author: MrCrayfish
 */
public class ClientHandler implements ClientModInitializer
{
    private static final Set<IConfigProvider> PROVIDERS = new HashSet<>();

    @Override
    public void onInitializeClient()
    {
        ClientLoginNetworking.registerGlobalReceiver(Channels.CONFIG_DATA, (client, handler, buf, listenerAdder) -> CompletableFuture.supplyAsync(() -> {
            HandshakeMessages.S2CConfigData message = HandshakeMessages.S2CConfigData.decode(buf);
            if(!SimpleConfigManager.getInstance().processConfigData(message)) {
                handler.getConnection().disconnect(Component.translatable("configured.gui.handshake_process_failed"));
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

        this.loadProviders();
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

    private void loadProviders()
    {
        FabricLoader.getInstance().getAllMods().forEach(container ->
        {
            CustomValue value = container.getMetadata().getCustomValue("configured");
            if(value != null && value.getType() == CustomValue.CvType.OBJECT)
            {
                CustomValue.CvObject configuredObj = value.getAsObject();
                CustomValue providersValue = configuredObj.get("providers");
                if(providersValue != null)
                {
                    if(providersValue.getType() == CustomValue.CvType.ARRAY)
                    {
                        CustomValue.CvArray array = providersValue.getAsArray();
                        array.forEach(providerValue -> {
                            if(providerValue.getType() == CustomValue.CvType.STRING)
                            {
                                String providerClass = providerValue.getAsString();
                                PROVIDERS.add(createProviderInstance(container, providerClass));
                                Configured.LOGGER.info("Successfully loaded config provider: {}", providerClass);
                            }
                            else
                            {
                                throw new RuntimeException("Config provider definition must be a String");
                            }
                        });
                    }
                    else if(providersValue.getType() == CustomValue.CvType.STRING)
                    {
                        String providerClass = providersValue.getAsString();
                        PROVIDERS.add(createProviderInstance(container, providerClass));
                        Configured.LOGGER.info("Successfully loaded config provider: {}", providerClass);
                    }
                    else
                    {
                        throw new RuntimeException("Config provider definition must be either a String or Array of Strings");
                    }
                }
            }
        });
    }

    private static IConfigProvider createProviderInstance(ModContainer container, String classPath)
    {
        try
        {
            Class<?> providerClass = Class.forName(classPath);
            Object obj = providerClass.getDeclaredConstructor().newInstance();
            if(!(obj instanceof IConfigProvider provider))
                throw new RuntimeException("Config providers must implement IConfigProvider");
            return provider;
        }
        catch(Exception e)
        {
            Configured.LOGGER.error("Failed to load config provider from mod: {}", container.getMetadata().getId());
            throw new RuntimeException("Failed to load config provider", e);
        }
    }

    public static Set<IConfigProvider> getProviders()
    {
        return PROVIDERS;
    }
}
