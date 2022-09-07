package com.mrcrayfish.configured.network;

import com.mrcrayfish.configured.Reference;
import com.mrcrayfish.configured.impl.simple.SimpleConfigManager;
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
                .buildLoginPacketList(SimpleConfigManager.getInstance()::getMessagesForLogin)
                .add();
    }

    public static SimpleChannel getHandshakeChannel()
    {
        return HANDSHAKE_CHANNEL;
    }
}
