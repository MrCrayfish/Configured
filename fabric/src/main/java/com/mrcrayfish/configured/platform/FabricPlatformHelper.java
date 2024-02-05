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
        boolean developer = FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER && Config.isDeveloperEnabled() && Config.getDevelopers().contains(player.getStringUUID());
        boolean lan = player.getServer() != null && !player.getServer().isDedicatedServer();
        MessageSessionData msg = new MessageSessionData(developer, lan);
        FriendlyByteBuf buf = PacketByteBufs.create();
        MessageSessionData.encode(msg, buf);
        ServerPlayNetworking.send(player, MessageSessionData.ID, buf);
    }

    @Override
    public void sendFrameworkConfigToServer(ResourceLocation id, byte[] data)
    {
        if(!this.isModLoaded("framework"))
            return;

        MessageFramework.Sync message = new MessageFramework.Sync(id, data);
        FriendlyByteBuf buf = PacketByteBufs.create();
        MessageFramework.Sync.encode(message, buf);
        ClientPlayNetworking.send(MessageFramework.Sync.ID, buf);
    }

    @Override
    public void sendFrameworkConfigRequest(ResourceLocation id)
    {
        if(!this.isModLoaded("framework"))
            return;

        MessageFramework.Request message = new MessageFramework.Request(id);
        FriendlyByteBuf buf = PacketByteBufs.create();
        MessageFramework.Request.encode(message, buf);
        ClientPlayNetworking.send(MessageFramework.Request.ID, buf);
    }

    @Override
    public void sendFrameworkConfigResponse(ServerPlayer player, byte[] data)
    {
        if(!this.isModLoaded("framework"))
            return;

        MessageFramework.Response message = new MessageFramework.Response(data);
        FriendlyByteBuf buf = PacketByteBufs.create();
        MessageFramework.Response.encode(message, buf);
        ServerPlayNetworking.send(player, MessageFramework.Response.ID, buf);
    }

    @Override
    public boolean isConnectionActive(ClientPacketListener listener)
    {
        return ClientPlayNetworking.getReceived().contains(MessageSessionData.ID);
    }
}
