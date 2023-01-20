package com.mrcrayfish.configured.fabric;

import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.impl.simple.SimpleConfigManager;
import com.mrcrayfish.configured.network.Channels;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.minecraft.network.FriendlyByteBuf;

public class ConfiguredImpl implements ModInitializer {
    public static void registerQueryStart() {
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
    }

    @Override
    public void onInitialize() {
        Configured.init();
    }
}
