package com.mrcrayfish.configured.impl.framework.handler;

import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.client.SessionData;
import com.mrcrayfish.configured.client.screen.RequestScreen;
import com.mrcrayfish.configured.impl.framework.FrameworkModConfig;
import com.mrcrayfish.configured.impl.framework.message.MessageFramework;
import com.mrcrayfish.configured.util.ConfigHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.function.Consumer;

/**
 * Author: MrCrayfish
 */
public class FrameworkClientHandler
{
    public static void handleResponse(MessageFramework.Response message, Consumer<Component> disconnect)
    {
        // Cancel handling if the player is not in a request screen
        Minecraft minecraft = Minecraft.getInstance();
        if(!(minecraft.screen instanceof RequestScreen requestScreen))
            return;

        // This is already checked on the server before sending the response, we just do an additional check on client
        Player player = minecraft.player;
        if(!ConfigHelper.isOperator(player) || !SessionData.isDeveloper(player)) {
            requestScreen.handleResponse(null, Component.translatable("configured.gui.no_permission"));
            return;
        }

        // The pending config should only be a Framework config
        IModConfig pendingConfig = requestScreen.getActiveConfig();
        if(!(pendingConfig instanceof FrameworkModConfig frameworkConfig)) {
            requestScreen.handleResponse(null, Component.translatable("configured.gui.request.invalid_config"));
            return;
        }

        // Ensure the response is only for the correct config type. Sync configs are not accept.
        if(!pendingConfig.getType().isServer() || pendingConfig.getType().isSync() || pendingConfig.getType() == ConfigType.DEDICATED_SERVER) {
            requestScreen.handleResponse(null, Component.translatable("configured.gui.request.invalid_config_type"));
            return;
        }

        // Try load the config data. Error will happen if data is incorrect
        if(!frameworkConfig.loadDataFromResponse(message)) {
            requestScreen.handleResponse(null, Component.translatable("configured.gui.request.process_error"));
            return;
        }

        // Finally handle response with loaded config instance
        requestScreen.handleResponse(frameworkConfig, null);
    }
}
