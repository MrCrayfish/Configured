package com.mrcrayfish.configured.platform;

import com.mrcrayfish.configured.Config;
import com.mrcrayfish.configured.api.Environment;
import com.mrcrayfish.configured.network.message.MessageSessionData;
import com.mrcrayfish.configured.network.payload.FrameworkPayload;
import com.mrcrayfish.configured.network.payload.SessionDataPayload;
import com.mrcrayfish.configured.platform.services.IPlatformHelper;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
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
        boolean developer = FMLLoader.getDist().isDedicatedServer() && Config.isDeveloperEnabled() && Config.getDevelopers().contains(player.getStringUUID());
        boolean lan = player.getServer() != null && !player.getServer().isDedicatedServer();
        SessionDataPayload.of(developer, lan).sendToPlayer(player);
    }

    @Override
    public void sendFrameworkConfigToServer(ResourceLocation id, byte[] data)
    {
        if(!this.isModLoaded("framework"))
            return;
        FrameworkPayload.Sync.of(id, data).sendToServer();
    }

    @Override
    public void sendFrameworkConfigRequest(ResourceLocation id)
    {
        if(!this.isModLoaded("framework"))
            return;
        FrameworkPayload.Request.of(id).sendToServer();
    }

    @Override
    public void sendFrameworkConfigResponse(ServerPlayer player, byte[] data)
    {
        if(!this.isModLoaded("framework"))
            return;
        FrameworkPayload.Response.of(data).sendToPlayer(player);
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public boolean isConnectionActive(ClientPacketListener listener)
    {
        return NetworkRegistry.getInstance().isConnected(listener, MessageSessionData.ID);
    }
}