package com.mrcrayfish.configured.client;

import com.mrcrayfish.configured.Config;
import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.api.ModContext;
import com.mrcrayfish.configured.api.util.ConfigScreenHelper;
import com.mrcrayfish.configured.client.screen.TooltipScreen;
import com.mrcrayfish.configured.platform.Services;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.ConfigScreenHandler;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

import java.util.Map;
import java.util.Set;

/**
 * Author: MrCrayfish
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientConfigured
{
    // This is where the magic happens
    public static void generateConfigFactories()
    {
        Constants.LOG.info("Creating config GUI factories...");
        ModList.get().forEachModContainer((modId, container) ->
        {
            // Ignore mods that already implement their own custom factory
            if(container.getCustomExtension(ConfigScreenHandler.ConfigScreenFactory.class).isPresent() && !Config.isForceConfiguredMenu())
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

    @SubscribeEvent
    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event)
    {
        event.register(ClientHandler.KEY_OPEN_MOD_LIST);
    }

    @SubscribeEvent
    private static void onRegisterTooltipComponentFactory(RegisterClientTooltipComponentFactoriesEvent event)
    {
        event.register(TooltipScreen.ListMenuTooltipComponent.class, TooltipScreen.ListMenuTooltipComponent::asClientTextTooltip);
    }
}
