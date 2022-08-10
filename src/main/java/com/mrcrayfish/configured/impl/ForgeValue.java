package com.mrcrayfish.configured.impl;

import com.mrcrayfish.configured.api.IConfigValue;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Objects;

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
        this.set(configValue.get());
    }

    @Override
    public T get()
    {
        return this.value;
    }

    @Override
    public void set(T value)
    {
        this.value = value;
    }

    @Override
    public boolean isDefault()
    {
        return Objects.equals(this.get(), this.valueSpec.getDefault());
    }

    @Override
    public boolean isChanged()
    {
        return !Objects.equals(this.get(), this.initialValue);
    }

    @Override
    public void restore()
    {
        this.set(this.getDefault());
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getDefault()
    {
        return (T) this.valueSpec.getDefault();
    }

    @Override
    public boolean isValid(T value)
    {
        return this.valueSpec.test(value);
    }

    @Override
    public String getComment()
    {
        return this.valueSpec.getComment();
    }

    @Override
    public String getTranslationKey()
    {
        return this.valueSpec.getTranslationKey();
    }

    @Override
    public String getName()
    {
        return lastValue(this.configValue.getPath(), "");
    }

    @Override
    public void cleanCache()
    {
        this.configValue.clearCache();
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
