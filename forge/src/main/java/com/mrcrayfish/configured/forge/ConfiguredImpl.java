package com.mrcrayfish.configured.forge;

import com.mrcrayfish.configured.Config;
import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.Reference;
import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.api.util.ConfigScreenHelper;
import com.mrcrayfish.configured.client.ClientHandler;
import com.mrcrayfish.configured.client.forge.ClientHandlerImpl;
import dev.architectury.platform.Platform;
import dev.architectury.platform.forge.EventBuses;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.network.NetworkConstants;

@Mod(value = Reference.MOD_ID)
public class ConfiguredImpl {
    public ConfiguredImpl() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(Reference.MOD_ID, bus);
        bus.addListener(this::onLoadComplete);
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));

    }

    private void onLoadComplete(FMLLoadCompleteEvent event)
    {
        event.enqueueWork(() ->
        {
            if(FMLLoader.getDist() == Dist.CLIENT)
            {
                new ClientHandlerImpl();
            }
        });
    }

    public static void registerQueryStart() {
        System.out.println("do something i guess?");
    }
}
