package com.mrcrayfish.configured.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Author: MrCrayfish
 */
public interface ConfiguredPayload extends CustomPacketPayload
{
    default void sendToServer()
    {
        PacketDistributor.SERVER.noArg().send(this);
    }

    default void sendToAll()
    {
        PacketDistributor.ALL.noArg().send(this);
    }

    default void sendToPlayer(ServerPlayer player)
    {
        PacketDistributor.PLAYER.with(player).send(this);
    }
}
