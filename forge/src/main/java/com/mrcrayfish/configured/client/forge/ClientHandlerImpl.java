package com.mrcrayfish.configured.client.forge;

import com.mrcrayfish.configured.Config;
import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.Reference;
import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.IConfigProvider;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.api.util.ConfigScreenHelper;
import com.mrcrayfish.configured.client.ClientHandler;
import dev.architectury.platform.Platform;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.forgespi.language.IModInfo;

import java.util.*;
import java.util.stream.Collectors;

import static com.mrcrayfish.configured.client.ClientHandler.getProviders;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID, value = Dist.CLIENT)
public class ClientHandlerImpl {
    public ClientHandlerImpl() {
        ClientHandler.init();
        generateConfigFactories();
        for (IConfigProvider provider : getProviders()) {
            for (IModInfo mod : ModList.get().getMods()) {
                System.out.println(provider.getConfigurationsForMod(Platform.getMod(mod.getModId())));
            }
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
                long count = modConfigMap.values().stream().mapToLong(Set::size).sum();
                Configured.LOGGER.info("Registering config factory for mod {}. Found {} config(s)", modId, count);
                String displayName = container.getModInfo().getDisplayName();
                ResourceLocation backgroundTexture = getBackgroundTexture(container.getModInfo());
                container.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> {
                    return ConfigScreenHelper.createSelectionScreen(screen, Component.literal(displayName), modConfigMap, backgroundTexture);
                }));
            }
        });
    }

    public static Map<ConfigType, Set<IModConfig>> createConfigMap(ModContainer container)
    {
        Map<ConfigType, Set<IModConfig>> modConfigMap = new HashMap<>();
        Set<IModConfig> configs = getProviders().stream().flatMap(p -> p.getConfigurationsForMod(Platform.getMod(container.getModId())).stream()).collect(Collectors.toSet());
        configs.forEach(config -> modConfigMap.computeIfAbsent(config.getType(), type -> new LinkedHashSet<>()).add(config));
        return modConfigMap;
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
}
