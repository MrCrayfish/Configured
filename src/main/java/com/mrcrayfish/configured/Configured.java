package com.mrcrayfish.configured;

import com.mrcrayfish.configured.client.ClientHandler;
import com.mrcrayfish.configured.config.ConfigManager;
import com.mrcrayfish.configured.config.EditingTracker;
import com.mrcrayfish.configured.network.PacketHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.NetworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Author: MrCrayfish
 */
@Mod(value = Reference.MOD_ID)
public class Configured
{
    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);

    public Configured()
    {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.clientSpec);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onLoadComplete);
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        MinecraftForge.EVENT_BUS.register(ConfigManager.getInstance());
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.register(EditingTracker.instance()));
    }

    private void onCommonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(PacketHandler::registerMessages);
    }

    private void onLoadComplete(FMLLoadCompleteEvent event)
    {
        if(FMLLoader.getDist() == Dist.CLIENT)
        {
            ClientHandler.init();
            ClientHandler.generateConfigFactories();
        }
    }
}
