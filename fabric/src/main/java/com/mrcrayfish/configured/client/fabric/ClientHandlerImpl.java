package com.mrcrayfish.configured.client.fabric;

import com.mrcrayfish.configured.client.ClientHandler;
import net.fabricmc.api.ClientModInitializer;

public class ClientHandlerImpl implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientHandler.init();
    }
}
