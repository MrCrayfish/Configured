package com.mrcrayfish.configured.platform;

import com.mrcrayfish.configured.Config;
import com.mrcrayfish.configured.api.Environment;
import com.mrcrayfish.configured.impl.framework.message.MessageFramework;
import com.mrcrayfish.configured.network.message.MessageSessionData;
import com.mrcrayfish.configured.platform.services.IPlatformHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public class FabricPlatformHelper implements IPlatformHelper
{
    @Override
    public String getPlatformName()
    {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId)
    {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment()
    {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public Environment getEnvironment()
    {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ? Environment.CLIENT : Environment.DEDICATED_SERVER;
    }

    @Override
    public Path getGamePath()
    {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public Path getConfigPath()
    {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public String getDefaultConfigPath()
    {
        return "defaultconfigs";
    }

    @Override
    public void sendSessionData(ServerPlayer player)
    {
        boolean developer = FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER && Config.isDeveloperEnabled() && Config.getDevelopers().contains(player.getUUID());
        boolean lan = player.getServer() != null && !player.getServer().isDedicatedServer();
        ServerPlayNetworking.send(player, new MessageSessionData(developer, lan));
    }

    @Override
    public void sendFrameworkConfigToServer(ResourceLocation id, byte[] data)
    {
        if(!this.isModLoaded("framework"))
            return;
        ClientPlayNetworking.send(new MessageFramework.Sync(id, data));
    }

    @Override
    public void sendFrameworkConfigRequest(ResourceLocation id)
    {
        if(!this.isModLoaded("framework"))
            return;
        ClientPlayNetworking.send(new MessageFramework.Request(id));
    }

    @Override
    public void sendFrameworkConfigResponse(ServerPlayer player, byte[] data)
    {
        if(!this.isModLoaded("framework"))
            return;
        ServerPlayNetworking.send(player, new MessageFramework.Response(data));
    }

    @Override
    public boolean isConnectionActive(ClientPacketListener listener)
    {
        return ClientPlayNetworking.getReceived().contains(MessageSessionData.TYPE.id());
    }
}
