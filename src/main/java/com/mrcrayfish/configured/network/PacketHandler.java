package com.mrcrayfish.configured.network;

import com.mrcrayfish.configured.Reference;
import com.mrcrayfish.configured.config.ConfigManager;
import com.mrcrayfish.configured.network.message.IMessage;
import com.mrcrayfish.configured.network.message.MessageSyncServerConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

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
                .consumer(net.minecraftforge.network.HandshakeHandler.indexFirst((handler, msg, s) -> HandshakeHandler.handleAcknowledge(msg, s)))
                .add();

        HANDSHAKE_CHANNEL.messageBuilder(HandshakeMessages.S2CConfigData.class, 1)
                .loginIndex(HandshakeMessages.LoginIndexedMessage::getLoginIndex, HandshakeMessages.LoginIndexedMessage::setLoginIndex)
                .decoder(HandshakeMessages.S2CConfigData::decode)
                .encoder(HandshakeMessages.S2CConfigData::encode)
                .consumer(net.minecraftforge.network.HandshakeHandler.biConsumerFor((handler, msg, supplier) -> HandshakeHandler.handleConfigData(msg, supplier)))
                .buildLoginPacketList(ConfigManager.getInstance()::getMessagesForLogin)
                .add();

        registerPlayMessage(MessageSyncServerConfig.class, new MessageSyncServerConfig());
    }

    private static <T> void registerPlayMessage(Class<T> clazz, IMessage<T> message)
    {
        PLAY_CHANNEL.registerMessage(nextId++, clazz, message::encode, message::decode, message::handle);
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
