package com.mrcrayfish.configured.impl;

import java.util.List;
import java.util.Objects;

import com.mrcrayfish.configured.api.IConfigValue;

import net.minecraftforge.common.ForgeConfigSpec;

public class ForgeValue<T> implements IConfigValue<T>
{
    public final ForgeConfigSpec.ConfigValue<T> configValue;
    public final ForgeConfigSpec.ValueSpec valueSpec;
    private final T initialValue;
    protected T value;
    
    public ForgeValue(ForgeConfigSpec.ConfigValue<T> configValue, ForgeConfigSpec.ValueSpec valueSpec)
    {
        this.configValue = configValue;
        this.valueSpec = valueSpec;
        this.initialValue = configValue.get();
        set(configValue.get());
    }
    
	@Override
	public T get()
	{
		return value;
	}

	@Override
	public void set(T value)
	{
		this.value = value;
	}

	@Override
	public boolean isDefault()
	{
		return Objects.equals(get(), valueSpec.getDefault());
	}

	@Override
	public boolean isChanged()
	{
		return !Objects.equals(get(), initialValue);
	}
	
	@Override
	public void restore()
	{
		set(getDefault());
	}

	@Override
	@SuppressWarnings("unchecked")
	public T getDefault()
	{
		return (T)valueSpec.getDefault();
	}

	@Override
	public boolean isValid(T value)
	{
		return valueSpec.test(value);
	}

	@Override
	public String getComment()
	{
		return valueSpec.getComment();
	}

	@Override
	public String getTranslationKey()
	{
		return valueSpec.getTranslationKey();
	}

	@Override
	public String getPath()
	{
		return lastValue(configValue.getPath(), "");
	}
	
	@Override
	public void cleanCache()
	{
		configValue.clearCache();
	}

	/**
     * Gets the last element in a list
     *
     * @param list         the list of get the value from
     * @param defaultValue if the list is empty, return this value instead
     * @param <V>          the type of list
     * @return the last element
     */
    public static <V> V lastValue(List<V> list, V defaultValue)
    {
        if(list.size() > 0)
        {
            return list.get(list.size() - 1);
        }
        return defaultValue;
    }
}
