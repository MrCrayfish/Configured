package com.mrcrayfish.configured;

import com.mrcrayfish.configured.impl.simple.SimpleConfigManager;
import com.mrcrayfish.configured.network.Channels;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.network.FriendlyByteBuf;
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
                boolean local = handler.getConnection().isMemoryConnection();
                SimpleConfigManager.getInstance().getMessagesForLogin(local).forEach(pair ->
                {
                    FriendlyByteBuf buf = PacketByteBufs.create();
                    pair.getRight().encode(buf);
                    sender.sendPacket(Channels.CONFIG_DATA, buf);
                });
            }));
        });

        ServerLoginNetworking.registerGlobalReceiver(Channels.CONFIG_DATA, (server, handler, understood, buf, synchronizer, responseSender) -> {});

        SimpleConfigManager.registerEvents();
    }
}
