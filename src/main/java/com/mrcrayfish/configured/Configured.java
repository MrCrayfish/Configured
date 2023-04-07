package com.mrcrayfish.configured;

import com.mrcrayfish.configured.impl.simple.SimpleConfigManager;
import com.mrcrayfish.configured.network.Channels;
import com.mrcrayfish.configured.network.ServerMessages;
import com.mrcrayfish.configured.network.message.MessageSessionData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Author: MrCrayfish
 */
public class Configured implements ModInitializer
{
    public static final Logger LOGGER = LoggerFactory.getLogger(Configured.class);

    public Configured()
    {
        SimpleConfigManager.getInstance();
    }

    @Override
    public void onInitialize()
    {
        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) ->
        {
            synchronizer.waitFor(server.submit(() ->
            {
                boolean local = handler.connection.isMemoryConnection();
                SimpleConfigManager.getInstance().getMessagesForLogin(local).forEach(pair ->
                {
                    FriendlyByteBuf buf = PacketByteBufs.create();
                    pair.getRight().encode(buf);
                    sender.sendPacket(Channels.CONFIG_DATA, buf);
                });
            }));
        });

        ServerLoginNetworking.registerGlobalReceiver(Channels.CONFIG_DATA, (server, handler, understood, buf, synchronizer, responseSender) -> {});

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
        {
            ServerPlayer player = handler.getPlayer();
            boolean developer = FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER && Config.DEVELOPER.enabled.get() && Config.DEVELOPER.developers.get().contains(player.getStringUUID());
            boolean lanServer = player.getServer() != null && !player.getServer().isDedicatedServer();
            ServerPlayNetworking.send(player, MessageSessionData.ID, MessageSessionData.create(developer, lanServer));
        });

        ServerMessages.register();

        SimpleConfigManager.registerEvents();
    }
}
