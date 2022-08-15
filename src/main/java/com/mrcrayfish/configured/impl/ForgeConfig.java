package com.mrcrayfish.configured.impl;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.util.ConfigHelper;
import net.minecraft.Util;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
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

    ModConfig config;

    public ForgeConfig(ModConfig config)
    {
        this.config = config;
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
                ConfigHelper.sendForgeModConfigDataToServer(this.config);
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
        return ConfigHelper.isModified(this);
    }

    @Override
    public void restoreDefaults()
    {
        // Block world configs since the path is dynamic
        if(ConfigHelper.isWorldConfig(this))
            return;
        ConfigHelper.gatherAllConfigValues(this.getRoot()).forEach(value -> {
            value.restore();
            value.cleanCache();
        });
    }
}
