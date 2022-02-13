package com.mrcrayfish.configured.impl;

import java.util.List;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.common.collect.ImmutableList;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.ValueEntry;

import net.minecraftforge.common.ForgeConfigSpec;

public class ForgeFolderEntry implements IConfigEntry
{
    private final String label;
    private final UnmodifiableConfig config;
    private final ForgeConfigSpec spec;
    private final boolean root;
    private List<IConfigEntry> entries;
	
	public ForgeFolderEntry(String label, UnmodifiableConfig config, ForgeConfigSpec spec, boolean root)
	{
		this.label = label;
		this.config = config;
		this.spec = spec;
		this.root = root;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<IConfigEntry> getChildren()
	{
		if(entries == null)
		{
            ImmutableList.Builder<IConfigEntry> builder = ImmutableList.builder();
            this.config.valueMap().forEach((s, o) ->
            {
                if(o instanceof UnmodifiableConfig)
                {
                    builder.add(new ForgeFolderEntry(s, (UnmodifiableConfig) o, this.spec, false));
                }
                else if(o instanceof ForgeConfigSpec.ConfigValue<?>)
                {
                    ForgeConfigSpec.ConfigValue<?> configValue = (ForgeConfigSpec.ConfigValue<?>)o;
                    if(configValue.get() instanceof List)
                    {
                        builder.add(new ValueEntry(new ForgeListValue((ForgeConfigSpec.ConfigValue<List<?>>)configValue, spec.getRaw(configValue.getPath()))));                    	
                    }
                    else
                    {
                        builder.add(new ValueEntry(new ForgeValue<>(configValue, spec.getRaw(configValue.getPath()))));

                    }
                }
            });
            this.entries = builder.build();
		}
		return entries;
	}
	
	@Override
	public boolean isRoot()
	{
		return root;
	}
	
	@Override
	public boolean isLeaf()
	{
		return false;
	}
	
	@Override
	public IConfigValue<?> getValue()
	{
		return null;
	}
	
	@Override
	public String getEntryName()
	{
		return label;
	}
}