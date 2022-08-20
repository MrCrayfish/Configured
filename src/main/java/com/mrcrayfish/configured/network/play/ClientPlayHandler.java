package com.mrcrayfish.configured.network.play;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.config.ConfigManager;
import com.mrcrayfish.configured.network.message.MessageSyncServerConfig;
import com.mrcrayfish.configured.network.message.MessageSyncSimpleConfig;
import com.mrcrayfish.configured.util.ConfigHelper;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.io.ByteArrayInputStream;
import java.util.Optional;

/**
 * Author: MrCrayfish
 */
public class ClientPlayHandler
{
    public static void handleSyncServerConfigMessage(MessageSyncServerConfig message)
    {
        if(Minecraft.getInstance().isLocalServer()) // Avoid updating config if packet was sent to self
            return;

        Configured.LOGGER.debug("Received forge config sync from server");
        Optional.ofNullable(ConfigHelper.getModConfig(message.getFileName())).ifPresent(config ->
        {
            CommentedConfig data = TomlFormat.instance().createParser().parse(new ByteArrayInputStream(message.getData()));
            ConfigHelper.setModConfigData(config, data);
            ConfigHelper.fireEvent(config, new ModConfigEvent.Reloading(config));
        });
    }

    public static void handleSyncSimpleConfigMessage(MessageSyncSimpleConfig message)
    {
        if(Minecraft.getInstance().isLocalServer()) // Avoid updating config if packet was sent to self
            return;

        Configured.LOGGER.debug("Received simple config sync from server");
        ConfigManager.getInstance().processSyncData(message, entry -> {});
        //TODO events
        //ConfigHelper.fireEvent(config, new ModConfigEvent.Reloading(config));
    }
}
