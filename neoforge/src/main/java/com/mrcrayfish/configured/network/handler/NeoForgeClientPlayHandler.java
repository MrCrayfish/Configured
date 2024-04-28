package com.mrcrayfish.configured.network.handler;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.network.payload.SyncNeoForgeConfigPayload;
import com.mrcrayfish.configured.util.NeoForgeConfigHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.IConfigEvent;
import net.neoforged.fml.config.ModConfig;

import java.io.ByteArrayInputStream;
import java.util.function.Consumer;

/**
 * Author: MrCrayfish
 */
public class NeoForgeClientPlayHandler
{
    public static void handleSyncServerConfigMessage(Consumer<Component> disconnect, SyncNeoForgeConfigPayload payload)
    {
        // Avoid updating config if packet was sent to self
        if(Minecraft.getInstance().isLocalServer())
            return;

        Constants.LOG.info("Received forge config sync from server");

        ModConfig config = NeoForgeConfigHelper.getModConfig(payload.fileName());
        if(config == null)
        {
            Constants.LOG.error("Server sent data for a forge config that doesn't exist: {}", payload.fileName());
            disconnect.accept(Component.translatable("configured.multiplayer.disconnect.process_config"));
            return;
        }

        if(config.getType() != ModConfig.Type.SERVER)
        {
            Constants.LOG.error("Server sent data for a config that isn't a server type: {}", payload.fileName());
            disconnect.accept(Component.translatable("configured.multiplayer.disconnect.process_config"));
            return;
        }

        try
        {
            CommentedConfig data = TomlFormat.instance().createParser().parse(new ByteArrayInputStream(payload.data()));
            if(!config.getSpec().isCorrect(data))
            {
                Constants.LOG.error("Server sent an incorrect config: {}", payload.fileName());
                disconnect.accept(Component.translatable("configured.multiplayer.disconnect.process_config"));
                return;
            }
            config.getSpec().acceptConfig(data);
            IConfigEvent.reloading(config).post();
        }
        catch(ParsingException e)
        {
            throw new RuntimeException(e);
        }
    }
}
