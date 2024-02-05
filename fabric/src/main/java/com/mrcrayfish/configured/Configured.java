package com.mrcrayfish.configured;

import com.mrcrayfish.configured.impl.framework.message.MessageFramework;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Author: MrCrayfish
 */
public class Configured implements ModInitializer
{
    @Override
    public void onInitialize()
    {
        Bootstrap.init();

        ServerPlayNetworking.registerGlobalReceiver(MessageFramework.Sync.ID, (server, player, handler, buf, responseSender) -> {
            MessageFramework.Sync.handle(MessageFramework.Sync.decode(buf), server::execute, player, handler::disconnect);
        });
        ServerPlayNetworking.registerGlobalReceiver(MessageFramework.Request.ID, (server, player, handler, buf, responseSender) -> {
            MessageFramework.Request.handle(MessageFramework.Request.decode(buf), server::execute, player, handler::disconnect);
        });
    }
}
