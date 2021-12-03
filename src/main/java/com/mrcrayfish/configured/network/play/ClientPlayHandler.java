package com.mrcrayfish.configured.network.play;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.network.message.MessageSyncServerConfig;
import com.mrcrayfish.configured.util.ConfigHelper;
import net.minecraft.client.Minecraft;

import java.io.ByteArrayInputStream;
import java.util.Optional;

/**
 * Author: MrCrayfish
 */
public class ClientPlayHandler
{
    public static void handleSyncServerConfigMessage(MessageSyncServerConfig message)
    {
        if(!Minecraft.getInstance().isIntegratedServerRunning())
        {
            Configured.LOGGER.debug("Received config sync from server");
            Optional.ofNullable(ConfigHelper.getModConfig(message.getFileName())).ifPresent(config ->
            {
                CommentedConfig data = TomlFormat.instance().createParser().parse(new ByteArrayInputStream(message.getData()));
                ConfigHelper.setConfigData(config, data);
                ConfigHelper.fireEvent(config, ConfigHelper.reloadingEvent(config));
            });
        }
    }
}
