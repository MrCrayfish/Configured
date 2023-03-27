package com.mrcrayfish.configured.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mrcrayfish.configured.Config;
import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.Reference;
import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.IConfigProvider;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.api.util.ConfigScreenHelper;
import com.mrcrayfish.configured.client.screen.IBackgroundTexture;
import com.mrcrayfish.configured.client.screen.ListMenuScreen;
import com.mrcrayfish.configured.impl.simple.SimpleConfigManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.ModListScreen;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Author: MrCrayfish
 */
@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class ClientHandler
{
    public static final KeyMapping KEY_OPEN_MOD_LIST = new KeyMapping("key.configured.open_mod_list", -1, "key.categories.configured");
    private static final Set<IConfigProvider> PROVIDERS = new HashSet<>();

    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event)
    {
        event.register(ClientHandler.KEY_OPEN_MOD_LIST);
    }

    public static void onRegisterTooltipComponentFactory(RegisterClientTooltipComponentFactoriesEvent event)
    {
        ListMenuScreen.registerTooltipFactory(event);
    }

    public static void init()
    {
        loadProviders();
        generateConfigFactories();
    }

    private static void loadProviders()
    {
        ModList.get().forEachModContainer((id, container) ->
        {
            Object raw = container.getModInfo().getModProperties().get("configuredProviders");
            if(raw instanceof String)
            {
                IConfigProvider provider = createProviderInstance(container, raw.toString());
                if(provider == null)
                    return;
                PROVIDERS.add(provider);
                Configured.LOGGER.info("Successfully loaded config provider: {}", raw.toString());
            }
            else if(raw instanceof List<?> providers)
            {
                for(Object obj : providers)
                {
                    IConfigProvider provider = createProviderInstance(container, obj.toString());
                    if(provider == null)
                        continue;
                    PROVIDERS.add(provider);
                    Configured.LOGGER.info("Successfully loaded config provider: {}", obj.toString());
                }
            }
            else if(raw != null)
            {
                throw new RuntimeException("Config provider definition must be either a String or Array of Strings");
            }
        });
    }

    @Nullable
    private static IConfigProvider createProviderInstance(ModContainer container, String classPath)
    {
        try
        {
            Class<?> providerClass = Class.forName(classPath);
            Object obj = providerClass.getDeclaredConstructor().newInstance();
            if(!(obj instanceof IConfigProvider provider))
            {
                throw new RuntimeException("Config providers must implement IConfigProvider");
            }
            return provider;
        }
        catch(Exception e)
        {
            Configured.LOGGER.error("Failed to load config provider from mod: {}", container.getModId());
            return null;
        }
    }

    // This is where the magic happens
    private static void generateConfigFactories()
    {
        Configured.LOGGER.info("Creating config GUI factories...");
        ModList.get().forEachModContainer((modId, container) ->
        {
            // Ignore mods that already implement their own custom factory
            if(container.getCustomExtension(ConfigScreenHandler.ConfigScreenFactory.class).isPresent() && !Config.CLIENT.forceConfiguredMenu.get())
                return;

            Map<ConfigType, Set<IModConfig>> modConfigMap = createConfigMap(container);
            if(!modConfigMap.isEmpty()) // Only add if at least one config exists
            {
                int count = modConfigMap.values().stream().mapToInt(Set::size).sum();
                Configured.LOGGER.info("Registering config factory for mod {}. Found {} config(s)", modId, count);
                String displayName = container.getModInfo().getDisplayName();
                ResourceLocation backgroundTexture = getBackgroundTexture(container.getModInfo());
                container.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) ->
                {
                    return ConfigScreenHelper.createSelectionScreen(screen, Component.literal(displayName), modConfigMap, backgroundTexture);
                }));
            }
        });
    }

    public static Map<ConfigType, Set<IModConfig>> createConfigMap(ModContainer container)
    {
        Map<ConfigType, Set<IModConfig>> modConfigMap = new HashMap<>();
        Set<IModConfig> configs = PROVIDERS.stream().flatMap(p -> streamConfigsFromProvider(container, p)).collect(Collectors.toSet());
        configs.forEach(config -> modConfigMap.computeIfAbsent(config.getType(), type -> new LinkedHashSet<>()).add(config));
        return modConfigMap;
    }

    private static Stream<IModConfig> streamConfigsFromProvider(ModContainer container, IConfigProvider provider)
    {
        try
        {
            return provider.getConfigurationsForMod(container).stream();
        }
        catch(Exception e)
        {
            Configured.LOGGER.error("An error occurred when loading configs from provider: {}", provider.getClass().getName(), e);
        }
        return Stream.empty();
    }

    private static ResourceLocation getBackgroundTexture(IModInfo info)
    {
        String configBackground = (String) info.getModProperties().get("configuredBackground");

        if(configBackground != null)
        {
            return new ResourceLocation(configBackground);
        }

        if(info instanceof ModInfo)
        {
            // Fallback to old method to getting config background (since mods might not have updated)
            Optional<String> optional = ((ModInfo) info).getConfigElement("configBackground");
            if(optional.isPresent())
            {
                return new ResourceLocation(optional.get());
            }
        }

        return Screen.BACKGROUND_LOCATION;
    }

    /**
     * Linked via ASM. Do not delete!
     */
    @SuppressWarnings("unused")
    public static void updateAbstractListTexture(AbstractSelectionList<?> list)
    {
        if(list instanceof IBackgroundTexture)
        {
            RenderSystem.setShaderTexture(0, ((IBackgroundTexture) list).getBackgroundTexture());
        }
    }

    /**
     * Linked via ASM. Do not delete!
     */
    @SuppressWarnings("unused")
    public static void updateScreenTexture(Screen screen)
    {
        if(screen instanceof IBackgroundTexture)
        {
            RenderSystem.setShaderTexture(0, ((IBackgroundTexture) screen).getBackgroundTexture());
        }
    }

    @SubscribeEvent
    public static void onKeyPress(InputEvent.Key event)
    {
        if(event.getAction() == GLFW.GLFW_PRESS && KEY_OPEN_MOD_LIST.isDown())
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
}
