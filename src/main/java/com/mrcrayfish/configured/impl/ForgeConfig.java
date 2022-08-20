package com.mrcrayfish.configured.impl;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.config.ConfigUtil;
import com.mrcrayfish.configured.util.ConfigHelper;
import net.minecraft.Util;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class ForgeConfig implements IModConfig
{
    private static final EnumMap<ModConfig.Type, ConfigType> TYPE_RESOLVER = Util.make(new EnumMap<>(ModConfig.Type.class), (map) -> {
        map.put(ModConfig.Type.CLIENT, ConfigType.CLIENT);
        map.put(ModConfig.Type.COMMON, ConfigType.UNIVERSAL);
        map.put(ModConfig.Type.SERVER, ConfigType.WORLD_SYNC);
    });

    private final ModConfig config;
    private final List<Pair<ForgeConfigSpec.ConfigValue<?>, ForgeConfigSpec.ValueSpec>> allConfigValues;

    public ForgeConfig(ModConfig config)
    {
        this.config = config;
        this.allConfigValues = ConfigHelper.gatherAllConfigValues(config);
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
                if(value instanceof ForgeValue<?> forge)
                {
                    if(forge instanceof ForgeListValue forgeList)
                    {
                        Function<List<?>, List<?>> converter = forgeList.getConverter();
                        if(converter != null)
                        {
                            newConfig.set(forge.configValue.getPath(), converter.apply(forgeList.get()));
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
                this.config.getHandler().unload(this.config.getFullPath().getParent(), this.config);
                ConfigHelper.setModConfigData(this.config, null);
            }
            else if(!changedValues.isEmpty())
            {
                this.syncToServer();
            }
        }
        else if(!changedValues.isEmpty())
        {
            Configured.LOGGER.info("Sending config reloading event for {}", this.config.getFileName());
            this.config.getSpec().afterReload();
            ConfigHelper.fireEvent(this.config, new ModConfigEvent.Reloading(this.config));
        }
    }

    @Override
    public IConfigEntry getRoot()
    {
        return new ForgeFolderEntry("Root", ((ForgeConfigSpec) this.config.getSpec()).getValues(), (ForgeConfigSpec) this.config.getSpec(), true);
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
        final CommentedFileConfig data = this.config.getHandler().reader(path).apply(this.config);
        ConfigHelper.setModConfigData(this.config, data);
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
                this.config.getHandler().unload(this.config.getFullPath().getParent(), this.config);
                ConfigHelper.setModConfigData(this.config, null);
            }
        }
    }

    @Override
    public boolean isChanged()
    {
        // Block world configs since the path is dynamic
        if(ConfigHelper.isWorldConfig(this))
            return false;

        // Check if any config value doesn't equal it's default
        return this.allConfigValues.stream().anyMatch(pair -> {
            return !Objects.equals(pair.getLeft().get(), pair.getRight().getDefault());
        });
    }

    @Override
    public void restoreDefaults()
    {
        // Block world configs since the path is dynamic
        if(ConfigHelper.isWorldConfig(this) && this.config == null)
            return;

        // Creates a copy of the config data then pushes all at once to avoid multiple IO ops
        CommentedConfig newConfig = CommentedConfig.copy(this.config.getConfigData());
        this.allConfigValues.forEach(pair ->
        {
            // Restore default value
            ForgeConfigSpec.ConfigValue<?> configValue = pair.getLeft();
            ForgeConfigSpec.ValueSpec valueSpec = pair.getRight();
            newConfig.set(configValue.getPath(), valueSpec.getDefault());
        });
        this.config.getConfigData().putAll(newConfig);

        // Finally clear cache of all config values
        this.allConfigValues.forEach(pair -> pair.getLeft().clearCache());
    }

    @Override
    public void syncToServer()
    {
        if(EffectiveSide.get().isClient())
        {
            ConfigHelper.sendForgeModConfigDataToServer(this.config);
        }
    }
}
