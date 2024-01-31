package com.mrcrayfish.configured.network.handler;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.network.message.play.MessageSyncForgeConfig;
import com.mrcrayfish.configured.util.ForgeConfigHelper;
import com.mrcrayfish.framework.api.network.MessageContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.io.ByteArrayInputStream;

/**
 * Author: MrCrayfish
 */
public class ForgeClientPlayHandler
{
    public static void handleSyncServerConfigMessage(MessageContext context, MessageSyncForgeConfig message)
    {
        // Avoid updating config if packet was sent to self
        if(Minecraft.getInstance().isLocalServer())
            return;

        Constants.LOG.info("Received forge config sync from server");

        ModConfig config = ForgeConfigHelper.getForgeConfig(message.fileName());
        if(config == null)
        {
            Constants.LOG.error("Server sent data for a forge config that doesn't exist: {}", message.fileName());
            context.getNetworkManager().disconnect(Component.translatable("configured.multiplayer.disconnect.process_config"));
            return;
        }

        if(config.getType() != ModConfig.Type.SERVER)
        {
            Constants.LOG.error("Server sent data for a config that isn't a server type: {}", message.fileName());
            context.getNetworkManager().disconnect(Component.translatable("configured.multiplayer.disconnect.process_config"));
            return;
        }

        try
        {
            CommentedConfig data = TomlFormat.instance().createParser().parse(new ByteArrayInputStream(message.data()));
            config.getSpec().acceptConfig(data);
            ForgeConfigHelper.fireForgeConfigEvent(config, new ModConfigEvent.Reloading(config));
        }
        catch(ParsingException e)
        {
            throw new RuntimeException(e);
        }
    }
}
