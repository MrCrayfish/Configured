package com.mrcrayfish.configured.client;

import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.api.IConfigProvider;
import com.mrcrayfish.configured.impl.simple.SimpleConfigManager;
import com.mrcrayfish.configured.network.Channels;
import com.mrcrayfish.configured.network.ClientMessages;
import com.mrcrayfish.configured.network.HandshakeMessages;
import com.mrcrayfish.configured.util.CustomValueUtil;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Mod;
import dev.architectury.platform.Platform;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import io.netty.buffer.Unpooled;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
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
public class ClientHandler {

    private static final Set<IConfigProvider> PROVIDERS = new HashSet<>();

    public ClientHandler()
    {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, Channels.CONFIG_DATA, (buf, context) -> CompletableFuture.supplyAsync(() -> {
            HandshakeMessages.S2CConfigData message = HandshakeMessages.S2CConfigData.decode(buf);
            if(!SimpleConfigManager.getInstance().processConfigData(message)) {
                Minecraft.getInstance().getConnection().getConnection().disconnect(Component.translatable("configured.gui.handshake_process_failed"));
            }
            return new FriendlyByteBuf(Unpooled.buffer());
        }));

        EditingTracker.registerEvents();

        if(this.isModListInstalled())
        {
            KeyMapping keyOpenModList = new KeyMapping("key.configured.open_mod_list", -1, "key.categories.configured");
            KeyMappingRegistry.register(keyOpenModList);
            ClientTickEvent.CLIENT_POST.register(client ->
            {
                while(keyOpenModList.consumeClick())
                {
                    this.openModList();
                }
            });
        }

        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
            if (player != null)
            SimpleConfigManager.getInstance().onClientDisconnect(player.connection.getConnection());
        });

        ClientMessages.register();

        this.loadProviders();
    }

    public static void init() {
        new ClientHandler();
    }

    private boolean isModListInstalled()
    {
        return Platform.isModLoaded("catalogue") || Platform.isModLoaded("modmenu");
    }

    private void openModList()
    {
        Minecraft minecraft = Minecraft.getInstance();
        if(minecraft.level == null || minecraft.screen != null)
            return;
        Screen newScreen = null;
        if(Platform.isModLoaded("catalogue"))
        {
            newScreen = createModListScreen("com.mrcrayfish.catalogue.client.screen.CatalogueModListScreen");
        }
        else if(Platform.isModLoaded("modmenu"))
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
        if (Platform.isForge()) {
            Platform.getMods().forEach(mod -> {
                CustomValueUtil.CustomValue value = CustomValueUtil.getCustomValue(mod, "configuredProviders");
                if (value != null) {
                    if (value.getType() == CustomValueUtil.CvType.STRING) {
                        String providerClass = value.getAsString();
                        PROVIDERS.add(createProviderInstance(mod, providerClass));
                        Configured.LOGGER.info("Successfully loaded config provider: {}", providerClass);
                    } else if (value.getType() == CustomValueUtil.CvType.ARRAY) {
                        CustomValueUtil.CvArray array = value.getAsArray();
                        array.forEach(providerValue -> {
                            if (providerValue.getType() == CustomValueUtil.CvType.STRING) {
                                String providerClass = providerValue.getAsString();
                                PROVIDERS.add(createProviderInstance(mod, providerClass));
                                Configured.LOGGER.info("Successfully loaded config provider: {}", providerClass);
                            } else {
                                throw new RuntimeException("Config provider definition must be a String");
                            }
                        });
                    }
                } else {
                    throw new RuntimeException("Config provider definition must be either a String or Array of Strings");
                }
            });
        } else {
            Platform.getMods().forEach(container ->
            {
                CustomValueUtil.CustomValue value = CustomValueUtil.getCustomValue(container, "configured");
                if (value != null && value.getType() == CustomValueUtil.CvType.OBJECT) {
                    CustomValueUtil.CvObject configuredObj = value.getAsObject();
                    CustomValueUtil.CustomValue providersValue = configuredObj.get("providers");
                    if (providersValue != null) {
                        if (providersValue.getType() == CustomValueUtil.CvType.ARRAY) {
                            CustomValueUtil.CvArray array = providersValue.getAsArray();
                            array.forEach(providerValue -> {
                                if (providerValue.getType() == CustomValueUtil.CvType.STRING) {
                                    String providerClass = providerValue.getAsString();
                                    PROVIDERS.add(createProviderInstance(container, providerClass));
                                    Configured.LOGGER.info("Successfully loaded config provider: {}", providerClass);
                                } else {
                                    throw new RuntimeException("Config provider definition must be a String");
                                }
                            });
                        } else if (providersValue.getType() == CustomValueUtil.CvType.STRING) {
                            String providerClass = providersValue.getAsString();
                            PROVIDERS.add(createProviderInstance(container, providerClass));
                            Configured.LOGGER.info("Successfully loaded config provider: {}", providerClass);
                        } else {
                            throw new RuntimeException("Config provider definition must be either a String or Array of Strings");
                        }
                    }
                }
            });
        }
    }

    private static IConfigProvider createProviderInstance(Mod container, String classPath)
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
            Configured.LOGGER.error("Failed to load config provider from mod: {}", container.getModId());
            throw new RuntimeException("Failed to load config provider", e);
        }
    }

    public static Set<IConfigProvider> getProviders()
    {
        return PROVIDERS;
    }
}
