package com.mrcrayfish.configured.platform.services;

import com.mrcrayfish.configured.api.Environment;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public interface IPlatformHelper
{
    /**
     * Gets the name of the current platform
     *
     * @return The name of the current platform.
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given id is loaded.
     *
     * @param modId The mod to check if it is loaded.
     * @return True if the mod is loaded, false otherwise.
     */
    boolean isModLoaded(String modId);

    /**
     * Check if the game is currently in a development environment.
     *
     * @return True if in a development environment, false otherwise.
     */
    boolean isDevelopmentEnvironment();

    /**
     * Gets the name of the environment type as a string.
     *
     * @return The name of the environment type.
     */
    default String getEnvironmentName()
    {
        return isDevelopmentEnvironment() ? "development" : "production";
    }

    Environment getEnvironment();

    Path getGamePath();

    Path getConfigPath();

    String getDefaultConfigPath();

    boolean isConnectionActive(ClientPacketListener listener);

    void sendSessionData(ServerPlayer player);

    void sendFrameworkConfigToServer(ResourceLocation id, byte[] data);

    void sendFrameworkConfigRequest(ResourceLocation name);

    void sendFrameworkConfigResponse(ServerPlayer player, byte[] byteArray);
}