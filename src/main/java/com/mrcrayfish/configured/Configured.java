package com.mrcrayfish.configured;

import com.mrcrayfish.configured.client.ClientHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Author: MrCrayfish
 */
@Mod(value = "configured")
public class Configured
{
    public static final Logger LOGGER = LogManager.getLogger("configured");

    public Configured()
    {
        if(!FMLLoader.isProduction()) //Only load config if development environment
        {
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.clientSpec);
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.customSpec, "custom_client.toml");
        }
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onConstructEvent);
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
    }

    private void onConstructEvent(FMLLoadCompleteEvent event)
    {
        if(FMLLoader.getDist() == Dist.CLIENT)
        {
            ClientHandler.generateConfigFactories();
        }
    }
}
