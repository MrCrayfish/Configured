package com.mrcrayfish.configured;

import com.google.common.collect.ImmutableMap;
import com.mrcrayfish.configured.client.ClientHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fmllegacy.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Function;

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
        }
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onConstructEvent);
        //FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onProcessMessage);
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> FMLNetworkConstants.IGNORESERVERONLY, (a, b) -> true));
    }

    private void onConstructEvent(FMLLoadCompleteEvent event)
    {
        if(FMLLoader.getDist() == Dist.CLIENT)
        {
            ClientHandler.onFinishedLoading();
        }
    }

    /**
     * Allows modders to pass
     */
    /*private void onProcessMessage(InterModProcessEvent event)
    {
        ImmutableMap.Builder<ForgeConfigSpec.ConfigValue<?>, Function<?, ?>> builder = new ImmutableMap.Builder<>();
        event.getIMCStream(s -> s.equals("parser")).forEach(message ->
        {
            Object object = message.messageSupplier().get();
            if(!(object instanceof Pair<?, ?> pair))
                return;

            if(!(pair.getLeft() instanceof ForgeConfigSpec.ConfigValue<?> configValue))
                return;

            if(!(pair.getRight() instanceof Function<?, ?> parser))
                return;

            builder.put(configValue, parser);
        });
    }*/
}
