package com.mrcrayfish.configured.network;

import com.mrcrayfish.configured.Reference;
import com.mrcrayfish.configured.impl.simple.SimpleConfigManager;
import com.mrcrayfish.configured.network.message.MessageSessionData;
import com.mrcrayfish.configured.network.message.MessageRequestSimpleConfig;
import com.mrcrayfish.configured.network.message.MessageResponseSimpleConfig;
import com.mrcrayfish.configured.network.message.MessageSyncForgeConfig;
import com.mrcrayfish.configured.network.message.MessageSyncSimpleConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 */
public class PacketHandler
{
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel HANDSHAKE_CHANNEL = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(Reference.MOD_ID, "handshake")).networkProtocolVersion(() -> PROTOCOL_VERSION).clientAcceptedVersions(s -> true).serverAcceptedVersions(s -> true).simpleChannel();
    private static final SimpleChannel PLAY_CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(Reference.MOD_ID, "play"), () -> PROTOCOL_VERSION, s -> true, PROTOCOL_VERSION::equals);
    private static int nextId = 0;

    public static void registerMessages()
    {
        HANDSHAKE_CHANNEL.messageBuilder(HandshakeMessages.C2SAcknowledge.class, 99)
                .loginIndex(HandshakeMessages.LoginIndexedMessage::getLoginIndex, HandshakeMessages.LoginIndexedMessage::setLoginIndex)
                .decoder(HandshakeMessages.C2SAcknowledge::decode)
                .encoder(HandshakeMessages.C2SAcknowledge::encode)
                .consumerNetworkThread(net.minecraftforge.network.HandshakeHandler.indexFirst((handler, msg, s) -> HandshakeHandler.handleAcknowledge(msg, s)))
                .add();

        HANDSHAKE_CHANNEL.messageBuilder(HandshakeMessages.S2CConfigData.class, 1)
                .loginIndex(HandshakeMessages.LoginIndexedMessage::getLoginIndex, HandshakeMessages.LoginIndexedMessage::setLoginIndex)
                .decoder(HandshakeMessages.S2CConfigData::decode)
                .encoder(HandshakeMessages.S2CConfigData::encode)
                .consumerNetworkThread(net.minecraftforge.network.HandshakeHandler.biConsumerFor((handler, msg, supplier) -> HandshakeHandler.handleConfigData(msg, supplier)))
                .buildLoginPacketList(SimpleConfigManager.getInstance()::getMessagesForLogin)
                .add();

        registerPlayMessage(null, MessageSyncForgeConfig.class, MessageSyncForgeConfig::encode, MessageSyncForgeConfig::decode, MessageSyncForgeConfig::handle);
        registerPlayMessage(null, MessageSyncSimpleConfig.class, MessageSyncSimpleConfig::encode, MessageSyncSimpleConfig::decode, MessageSyncSimpleConfig::handle);
        registerPlayMessage(NetworkDirection.PLAY_TO_SERVER, MessageRequestSimpleConfig.class, MessageRequestSimpleConfig::encode, MessageRequestSimpleConfig::decode, MessageRequestSimpleConfig::handle);
        registerPlayMessage(NetworkDirection.PLAY_TO_CLIENT, MessageResponseSimpleConfig.class, MessageResponseSimpleConfig::encode, MessageResponseSimpleConfig::decode, MessageResponseSimpleConfig::handle);
        registerPlayMessage(NetworkDirection.PLAY_TO_CLIENT, MessageSessionData.class, MessageSessionData::encode, MessageSessionData::decode, MessageSessionData::handle);
    }

    private static <T> void registerPlayMessage(@Nullable NetworkDirection direction, Class<T> clazz, BiConsumer<T, FriendlyByteBuf> encode, Function<FriendlyByteBuf, T> decode, BiConsumer<T, Supplier<NetworkEvent.Context>> consumer)
    {
        PLAY_CHANNEL.registerMessage(nextId++, clazz, encode, decode, consumer, Optional.ofNullable(direction));
    }

    public static SimpleChannel getHandshakeChannel()
    {
        return HANDSHAKE_CHANNEL;
    }

    public static SimpleChannel getPlayChannel()
    {
        return PLAY_CHANNEL;
    }
}
