package com.mrcrayfish.configured;

import com.mrcrayfish.configured.client.ClientHandler;
import com.mrcrayfish.configured.network.PacketHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fmllegacy.network.FMLNetworkConstants;
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
        if(!FMLLoader.isProduction()) //Only load config if development environment
        {
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.clientSpec);
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.customSpec, "custom_client.toml");
        }
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onLoadComplete);
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
    }

    private void onCommonSetup(FMLCommonSetupEvent event)
    {
        PacketHandler.registerPlayMessages();
    }

    private void onLoadComplete(FMLLoadCompleteEvent event)
    {
        if(FMLLoader.getDist() == Dist.CLIENT)
        {
            ClientHandler.registerKeyBindings();
            ClientHandler.generateConfigFactories();
        }
    }
}
