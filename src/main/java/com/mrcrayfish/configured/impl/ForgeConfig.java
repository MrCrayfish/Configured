package com.mrcrayfish.configured.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Function;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.client.screen.ListMenuScreen;
import com.mrcrayfish.configured.util.ConfigHelper;

import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.config.ModConfig.Type;

public class ForgeConfig implements IModConfig
{
	ModConfig config;
	
	public ForgeConfig(ModConfig config)
	{
		this.config = config;
	}

	@Override
	public void saveConfig(IConfigEntry entry)
	{
        CommentedConfig newConfig = CommentedConfig.copy(this.config.getConfigData());
        Queue<IConfigEntry> found = new ArrayDeque<>();
        found.add(entry);
        while(!found.isEmpty())
        {
        	IConfigEntry toSave = found.poll();
        	if(!toSave.isLeaf())
        	{
        		found.addAll(toSave.getChildren());
        		continue;
        	}
        	IConfigValue<?> value = toSave.getValue();
        	if(value == null || !value.isChanged()) continue;
        	if(value instanceof ForgeValue)
        	{
        		ForgeValue<?> forge = (ForgeValue<?>)value;
        		if(forge instanceof ForgeListValue)
        		{
        			ForgeListValue forgeList = (ForgeListValue)value;
                    Function<List<?>, List<?>> converter = forgeList.getConverter();
                    if(converter != null)
                    {
                        newConfig.set(forge.configValue.getPath(), converter.apply(forgeList.get()));
                        continue;
                    }
        		}
                newConfig.set(forge.configValue.getPath(), value.get());
        	}
        }
        this.config.getConfigData().putAll(newConfig);
        if(getConfigType() == Type.SERVER)
        {
            if(!ListMenuScreen.isPlayingGame())
            {
                // Unload server configs since still in main menu
                this.config.getHandler().unload(this.config.getFullPath().getParent(), this.config);
                ConfigHelper.setConfigData(this.config, null);
            }
            else
            {
                ConfigHelper.sendConfigDataToServer(this.config);
            }
        }
        else
        {
            Configured.LOGGER.info("Sending config reloading event for {}", this.config.getFileName());
            this.config.getSpec().afterReload();
            ConfigHelper.fireEvent(this.config, ConfigHelper.reloadingEvent(this.config));
        }
	}
	
	@Override
	public IConfigEntry getRoot()
	{
		return new ForgeFolderEntry("Root", config.getSpec().getValues(), config.getSpec(), true);
	}

	@Override
	public Type getConfigType()
	{
		return config.getType();
	}

	@Override
	public String getFileName()
	{
		return config.getFileName();
	}

	@Override
	public String getModId()
	{
		return config.getModId();
	}

	@Override
	public void loadServerConfig(Path path, Consumer<IModConfig> result) throws IOException
	{
        final CommentedFileConfig data = config.getHandler().reader(path).apply(config);
        ConfigHelper.setConfigData(config, data);
        result.accept(this);
	}
	
}
