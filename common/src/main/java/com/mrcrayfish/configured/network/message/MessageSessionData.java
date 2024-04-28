package com.mrcrayfish.configured.network.message;

import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.network.ClientPlayHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/**
 * Author: MrCrayfish
 */
public record MessageSessionData(boolean developer, boolean lan) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<MessageSessionData> TYPE = new Type<>(new ResourceLocation(Constants.MOD_ID, "session_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MessageSessionData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        MessageSessionData::developer,
        ByteBufCodecs.BOOL,
        MessageSessionData::lan,
        MessageSessionData::new
    );

    public static void handle(MessageSessionData message, Consumer<Runnable> executor)
    {
        executor.accept(() -> ClientPlayHandler.handleSessionData(message));
    }

    @Override
    public Type<MessageSessionData> type()
    {
        return TYPE;
    }
}
