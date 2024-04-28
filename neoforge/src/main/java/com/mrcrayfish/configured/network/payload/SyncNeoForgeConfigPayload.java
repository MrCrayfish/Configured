package com.mrcrayfish.configured.network.payload;

import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.network.ConfiguredCodecs;
import com.mrcrayfish.configured.network.handler.NeoForgeClientPlayHandler;
import com.mrcrayfish.configured.network.handler.NeoForgeServerPlayHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Author: MrCrayfish
 */
public record SyncNeoForgeConfigPayload(String fileName, byte[] data) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<SyncNeoForgeConfigPayload> TYPE = new Type<>(new ResourceLocation(Constants.MOD_ID, "sync_neoforge_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncNeoForgeConfigPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        SyncNeoForgeConfigPayload::fileName,
        ConfiguredCodecs.BYTE_ARRAY,
        SyncNeoForgeConfigPayload::data,
        SyncNeoForgeConfigPayload::new
    );

    public static void handle(SyncNeoForgeConfigPayload payload, IPayloadContext context)
    {
        Player player = context.player();
        if(context.flow() == PacketFlow.SERVERBOUND)
        {
            if(player instanceof ServerPlayer serverPlayer)
            {
                context.enqueueWork(() -> NeoForgeServerPlayHandler.handleSyncServerConfigMessage(serverPlayer, payload));
            }
        }
        else
        {
            context.enqueueWork(() -> NeoForgeClientPlayHandler.handleSyncServerConfigMessage(context::disconnect, payload));
        }
    }

    @Override
    public Type<SyncNeoForgeConfigPayload> type()
    {
        return TYPE;
    }
}
