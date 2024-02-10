package com.mrcrayfish.configured.impl.neoforge;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.client.SessionData;
import com.mrcrayfish.configured.network.payload.SyncNeoForgeConfigPayload;
import com.mrcrayfish.configured.util.ConfigHelper;
import com.mrcrayfish.configured.util.NeoForgeConfigHelper;
import net.minecraft.Util;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.config.ConfigFileTypeHandler;
import net.neoforged.fml.config.IConfigEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public class NeoForgeConfig implements IModConfig
{
    protected static final EnumMap<ModConfig.Type, ConfigType> TYPE_RESOLVER = Util.make(new EnumMap<>(ModConfig.Type.class), (map) -> {
        map.put(ModConfig.Type.CLIENT, ConfigType.CLIENT);
        map.put(ModConfig.Type.COMMON, ConfigType.UNIVERSAL);
        map.put(ModConfig.Type.SERVER, ConfigType.WORLD_SYNC);
    });

    protected final ModConfig config;
    protected final List<ForgeValueEntry> allConfigValues;

    public NeoForgeConfig(ModConfig config)
    {
        this.config = config;
        this.allConfigValues = getAllConfigValues(config);
    }

    protected NeoForgeConfig(ModConfig config, List<ForgeValueEntry> allConfigValues)
    {
        this.config = config;
        this.allConfigValues = allConfigValues;
    }

    @Override
    public void update(IConfigEntry entry)
    {
        Set<IConfigValue<?>> changedValues = ConfigHelper.getChangedValues(entry);
        if(!changedValues.isEmpty())
        {
            CommentedConfig newConfig = CommentedConfig.copy(this.config.getConfigData());
            changedValues.forEach(value ->
            {
                if(value instanceof NeoForgeValue<?> forge)
                {
                    if(forge instanceof NeoForgeListValue<?> forgeList)
                    {
                        List<?> converted = forgeList.getConverted();
                        if(converted != null)
                        {
                            newConfig.set(forge.configValue.getPath(), converted);
                            return;
                        }
                    }
                    newConfig.set(forge.configValue.getPath(), value.get());
                }
            });
            this.config.getConfigData().putAll(newConfig);
        }

        if(this.getType() == ConfigType.WORLD_SYNC)
        {
            if(!ConfigHelper.isPlayingGame())
            {
                // Unload server configs since still in main menu
                NeoForgeConfigHelper.unload(this.config);
            }
            else
            {
                this.syncToServer();
            }

        }
        else if(!changedValues.isEmpty())
        {
            Constants.LOG.info("Sending config reloading event for {}", this.config.getFileName());
            this.config.getSpec().afterReload();
            IConfigEvent.reloading(this.config).post();
        }
    }

    @Override
    public IConfigEntry getRoot()
    {
        return new NeoForgeFolderEntry(((ModConfigSpec) this.config.getSpec()).getValues(), (ModConfigSpec) this.config.getSpec());
    }

    @Override
    public ConfigType getType()
    {
        return TYPE_RESOLVER.get(this.config.getType());
    }

    @Override
    public String getFileName()
    {
        return this.config.getFileName();
    }

    @Override
    public String getModId()
    {
        return this.config.getModId();
    }

    @Override
    public void loadWorldConfig(Path path, Consumer<IModConfig> result)
    {
        final CommentedFileConfig data = ConfigFileTypeHandler.TOML.reader(path).apply(this.config);
        NeoForgeConfigHelper.setConfigData(this.config, data);
        result.accept(this);
    }

    @Override
    public void stopEditing()
    {
        // Attempts to unload the server config if player simply just went back
        if(this.config != null && this.getType() == ConfigType.WORLD)
        {
            if(!ConfigHelper.isPlayingGame())
            {
                // Unload server configs since still in main menu
                NeoForgeConfigHelper.unload(this.config);
            }
        }
    }

    @Override
    public boolean isChanged()
    {
        // Block world configs since the path is dynamic
        if(ConfigHelper.isWorldConfig(this) && this.config.getConfigData() == null)
            return false;

        // Check if any config value doesn't equal it's default
        return this.allConfigValues.stream().anyMatch(entry -> {
            return !Objects.equals(entry.value.get(), entry.spec.getDefault());
        });
    }

    @Override
    public void restoreDefaults()
    {
        // Block world configs since the path is dynamic
        if(ConfigHelper.isWorldConfig(this) && this.config.getConfigData() == null)
            return;

        // Creates a copy of the config data then pushes all at once to avoid multiple IO ops
        CommentedConfig newConfig = CommentedConfig.copy(this.config.getConfigData());
        this.allConfigValues.forEach(entry -> newConfig.set(entry.value.getPath(), entry.spec.getDefault()));
        this.config.getConfigData().putAll(newConfig);

        // Finally clear cache of all config values
        this.allConfigValues.forEach(pair -> pair.value.clearCache());
    }

    private void syncToServer()
    {
        if(this.config == null)
            return;

        if(!ConfigHelper.isPlayingGame())
            return;

        if(!ConfigHelper.isConfiguredInstalledOnServer())
            return;

        if(this.getType() != ConfigType.WORLD_SYNC) // Forge only supports this type
            return;

        // Checked on server too
        Player player = ConfigHelper.getClientPlayer();
        if(!ConfigHelper.isOperator(player) || !SessionData.isDeveloper(player))
            return;

        try
        {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            TomlFormat.instance().createWriter().write(this.config.getConfigData(), stream);
            SyncNeoForgeConfigPayload.of(this.config.getFileName(), stream.toByteArray()).sendToServer();
            stream.close();
        }
        catch(IOException e)
        {
            Constants.LOG.error("Failed to close byte stream when sending config to server");
        }
    }

    protected List<ForgeValueEntry> getAllConfigValues(ModConfig config)
    {
        return NeoForgeConfigHelper.gatherAllConfigValues(config).stream().map(pair -> new ForgeValueEntry(pair.getLeft(), pair.getRight())).toList();
    }

    protected record ForgeValueEntry(ModConfigSpec.ConfigValue<?> value, ModConfigSpec.ValueSpec spec) {}
}
