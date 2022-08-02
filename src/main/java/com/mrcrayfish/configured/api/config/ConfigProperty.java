package com.mrcrayfish.configured.api.config;

import com.electronwill.nightconfig.core.ConfigSpec;

/**
 * Author: MrCrayfish
 */
public abstract sealed class ConfigProperty<T> permits DoubleProperty, IntProperty
{
    protected final T defaultValue;
    private T value;
    boolean loaded;

    public ConfigProperty(T defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    public T get()
    {
        if(!this.loaded)
            throw new IllegalStateException("Config property is not loaded yet");
        return this.value;
    }

    public void set(T value)
    {
        this.value = value;
    }

    public T getDefaultValue()
    {
        return this.defaultValue;
    }

    public abstract void defineSpec(ConfigSpec spec, String path);
}
