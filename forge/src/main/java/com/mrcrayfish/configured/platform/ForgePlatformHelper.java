package com.mrcrayfish.configured.platform;

import com.mrcrayfish.configured.Config;
import com.mrcrayfish.configured.api.Environment;
import com.mrcrayfish.configured.impl.framework.message.MessageFramework;
import com.mrcrayfish.configured.network.ForgeNetwork;
import com.mrcrayfish.configured.network.message.MessageSessionData;
import com.mrcrayfish.configured.platform.services.IPlatformHelper;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLConfig;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;

import java.nio.file.Path;

public class ForgePlatformHelper implements IPlatformHelper
{
    @Override
    public String getPlatformName()
    {
        return "Forge";
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
        ForgeNetwork.getChannel().send(new MessageSessionData(developer, lan), PacketDistributor.PLAYER.with(player));
    }

    @Override
    public void sendFrameworkConfigToServer(ResourceLocation id, byte[] data)
    {
        if(!this.isModLoaded("framework"))
            return;
        ForgeNetwork.getChannel().send(new MessageFramework.Sync(id, data), PacketDistributor.SERVER.noArg());
    }

    @Override
    public void sendFrameworkConfigRequest(ResourceLocation id)
    {
        if(!this.isModLoaded("framework"))
            return;
        ForgeNetwork.getChannel().send(new MessageFramework.Request(id), PacketDistributor.SERVER.noArg());
    }

    @Override
    public void sendFrameworkConfigResponse(ServerPlayer player, byte[] data)
    {
        if(!this.isModLoaded("framework"))
            return;
        ForgeNetwork.getChannel().send(new MessageFramework.Response(data), PacketDistributor.PLAYER.with(player));
    }

    @Override
    public boolean isConnectionActive(ClientPacketListener listener)
    {
        return ForgeNetwork.getChannel().isRemotePresent(listener.getConnection());
    }
}