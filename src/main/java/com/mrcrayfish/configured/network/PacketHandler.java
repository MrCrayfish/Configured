package com.mrcrayfish.configured.network;

import com.mrcrayfish.configured.Reference;
import com.mrcrayfish.configured.network.message.IMessage;
import com.mrcrayfish.configured.network.message.MessageSyncServerConfig;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

/**
 * Author: MrCrayfish
 */
public class PacketHandler
{
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel PLAY_CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(Reference.MOD_ID, "play"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
    private static int nextId = 0;

    public static void registerPlayMessages()
    {
        registerPlayMessage(MessageSyncServerConfig.class, new MessageSyncServerConfig());
    }

    private static <T> void registerPlayMessage(Class<T> clazz, IMessage<T> message)
    {
        PLAY_CHANNEL.registerMessage(nextId++, clazz, message::encode, message::decode, message::handle);
    }

    public static SimpleChannel getPlayChannel()
    {
        return PLAY_CHANNEL;
    }
}
