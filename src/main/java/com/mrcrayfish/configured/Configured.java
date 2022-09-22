package com.mrcrayfish.configured;

import com.mrcrayfish.configured.client.ClientHandler;
import com.mrcrayfish.configured.client.EditingTracker;
import com.mrcrayfish.configured.impl.simple.SimpleConfigManager;
import com.mrcrayfish.configured.network.PacketHandler;
import com.mrcrayfish.configured.network.message.MessageSessionData;
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
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.network.NetworkConstants;
import net.minecraftforge.network.PacketDistributor;
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
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::onCommonSetup);
        bus.addListener(this::onLoadComplete);
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        MinecraftForge.EVENT_BUS.register(SimpleConfigManager.getInstance());
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerJoin);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.register(EditingTracker.instance()));
    }

    private void onCommonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(PacketHandler::registerMessages);
    }

    private void onLoadComplete(FMLLoadCompleteEvent event)
    {
        event.enqueueWork(() ->
        {
            if(FMLLoader.getDist() == Dist.CLIENT)
            {
                ClientHandler.init();
            }
        });
    }

    /**
     * Sends the developer flag only to developers.
     */
    private void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event)
    {
        if(!EffectiveSide.get().isServer())
            return;

        ServerPlayer player = (ServerPlayer) event.getEntity();
        boolean developer = FMLEnvironment.dist.isDedicatedServer() && Config.DEVELOPER.enabled.get() && Config.DEVELOPER.developers.get().contains(player.getStringUUID());
        boolean lanServer = player.getServer() != null && !player.getServer().isDedicatedServer();
        PacketHandler.getPlayChannel().send(PacketDistributor.PLAYER.with(() -> player), new MessageSessionData(developer, lanServer));
    }
}
