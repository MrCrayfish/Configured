package com.mrcrayfish.configured;

import com.mrcrayfish.configured.client.ClientConfigured;
import com.mrcrayfish.configured.client.ClientHandler;
import com.mrcrayfish.configured.client.EditingTracker;
import com.mrcrayfish.configured.network.ForgeNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;

/**
 * Author: MrCrayfish
 */
@Mod(value = Constants.MOD_ID)
public class Configured
{
    public Configured()
    {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::onCommonSetup);
        bus.addListener(this::onLoadComplete);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            bus.addListener(ClientConfigured::onRegisterKeyMappings);
            bus.addListener(ClientConfigured::onRegisterTooltipComponentFactory);
        });
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> IExtensionPoint.DisplayTest.IGNORESERVERONLY, (a, b) -> true));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.register(EditingTracker.instance()));
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
    }

    private void onCommonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            Bootstrap.init();
            ForgeNetwork.init();
        });
    }

    private void onLoadComplete(FMLLoadCompleteEvent event)
    {
        event.enqueueWork(() ->
        {
            if(FMLLoader.getDist() == Dist.CLIENT)
            {
                ClientHandler.init();
                ClientConfigured.generateConfigFactories();
            }
        });
    }

    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if(event.getEntity() instanceof ServerPlayer player)
        {
            Events.onPlayerLoggedIn(player);
        }
    }
}
