package com.mrcrayfish.configured;

import com.mrcrayfish.configured.impl.simple.SimpleConfigManager;
import com.mrcrayfish.configured.network.Channels;
import com.mrcrayfish.configured.network.ServerMessages;
import com.mrcrayfish.configured.network.message.MessageSessionData;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import net.fabricmc.api.EnvType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Author: MrCrayfish
 * Author: Jab125
 */
public class Configured {
    public static final Logger LOGGER = LoggerFactory.getLogger(Configured.class);
    public Configured() {
        initEvents();

        ServerMessages.register();

        SimpleConfigManager.registerEvents();
    }

    private void doNothing(FriendlyByteBuf buf, NetworkManager.PacketContext context) {
    }

    private void onPlayerJoin(ServerPlayer player) {
        if(player.level.isClientSide) // checks the effective side
            return;

        boolean developer = Platform.getEnv() == EnvType.SERVER && Config.DEVELOPER.enabled.get() && Config.DEVELOPER.developers.get().contains(player.getStringUUID());
        boolean lanServer = player.getServer() != null && !player.getServer().isDedicatedServer();
        NetworkManager.sendToPlayer(player, MessageSessionData.ID, MessageSessionData.create(developer, lanServer));
    }

    private void initEvents() {
        PlayerEvent.PLAYER_JOIN.register(this::onPlayerJoin);
        registerQueryStart();
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, Channels.CONFIG_DATA, this::doNothing);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, Channels.CONFIG_DATA, this::doNothing);
    }

    @ExpectPlatform
    private static void registerQueryStart() {
        throw new AssertionError();
    }

    public static void init() {
        new Configured();
    }
}
