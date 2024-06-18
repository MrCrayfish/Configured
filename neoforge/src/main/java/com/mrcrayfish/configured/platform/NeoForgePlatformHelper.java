package com.mrcrayfish.configured.platform;

import com.mrcrayfish.configured.Config;
import com.mrcrayfish.configured.api.Environment;
import com.mrcrayfish.configured.impl.framework.message.MessageFramework;
import com.mrcrayfish.configured.network.message.MessageSessionData;
import com.mrcrayfish.configured.platform.services.IPlatformHelper;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import java.nio.file.Path;

public class NeoForgePlatformHelper implements IPlatformHelper
{
    @Override
    public String getPlatformName()
    {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId)
    {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment()
    {
        return !FMLLoader.isProduction();
    }

    @Override
    public Environment getEnvironment()
    {
        return FMLLoader.getDist().isClient() ? Environment.CLIENT : Environment.DEDICATED_SERVER;
    }

    @Override
    public Path getGamePath()
    {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public Path getConfigPath()
    {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public String getDefaultConfigPath()
    {
        return FMLConfig.defaultConfigPath();
    }

    @Override
    public void sendSessionData(ServerPlayer player)
    {
        boolean developer = FMLLoader.getDist().isDedicatedServer() && Config.isDeveloperEnabled() && Config.getDevelopers().contains(player.getUUID());
        boolean lan = player.getServer() != null && !player.getServer().isDedicatedServer();
        PacketDistributor.sendToPlayer(player, new MessageSessionData(developer, lan));
    }

    @Override
    public void sendFrameworkConfigToServer(ResourceLocation id, byte[] data)
    {
        if(!this.isModLoaded("framework"))
            return;
        PacketDistributor.sendToServer(new MessageFramework.Sync(id, data));
    }

    @Override
    public void sendFrameworkConfigRequest(ResourceLocation id)
    {
        if(!this.isModLoaded("framework"))
            return;
        PacketDistributor.sendToServer(new MessageFramework.Request(id));
    }

    @Override
    public void sendFrameworkConfigResponse(ServerPlayer player, byte[] data)
    {
        if(!this.isModLoaded("framework"))
            return;
        PacketDistributor.sendToPlayer(player, new MessageFramework.Response(data));
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public boolean isConnectionActive(ClientPacketListener listener)
    {
        return NetworkRegistry.hasChannel(listener, MessageSessionData.TYPE.id());
    }
}