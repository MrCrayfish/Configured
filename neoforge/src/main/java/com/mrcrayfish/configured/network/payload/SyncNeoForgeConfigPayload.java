package com.mrcrayfish.configured.network.payload;

import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.network.ConfiguredPayload;
import com.mrcrayfish.configured.network.handler.NeoForgeClientPlayHandler;
import com.mrcrayfish.configured.network.handler.NeoForgeServerPlayHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

/**
 * Author: MrCrayfish
 */
public final class SyncNeoForgeConfigPayload implements ConfiguredPayload
{
    public static final ResourceLocation ID = new ResourceLocation(Constants.MOD_ID, "sync_neoforge_config");

    private final String fileName;
    private final byte[] data;

    private SyncNeoForgeConfigPayload(String fileName, byte[] data)
    {
        this.fileName = fileName;
        this.data = data;
    }

    public SyncNeoForgeConfigPayload(FriendlyByteBuf buf)
    {
        this(buf.readUtf(), buf.readByteArray());
    }

    @Override
    public void write(FriendlyByteBuf buf)
    {
        buf.writeUtf(this.fileName);
        buf.writeByteArray(this.data);
    }

    @Override
    public ResourceLocation id()
    {
        return ID;
    }

    public String fileName()
    {
        return this.fileName;
    }

    public byte[] data()
    {
        return this.data;
    }

    public static void handle(SyncNeoForgeConfigPayload payload, PlayPayloadContext context)
    {
        Player player = context.player().orElse(null);
        if(context.flow() == PacketFlow.SERVERBOUND)
        {
            if(player instanceof ServerPlayer serverPlayer)
            {
                context.workHandler().submitAsync(() -> NeoForgeServerPlayHandler.handleSyncServerConfigMessage(serverPlayer, payload));
            }
        }
        else
        {
            context.workHandler().submitAsync(() -> NeoForgeClientPlayHandler.handleSyncServerConfigMessage(context.packetHandler()::disconnect, payload));
        }
    }

    public static SyncNeoForgeConfigPayload of(String fileName, byte[] data)
    {
        return new SyncNeoForgeConfigPayload(fileName, data);
    }
}
