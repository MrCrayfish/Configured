package com.mrcrayfish.configured.api.config;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.mrcrayfish.configured.config.ConfigManager;

/**
 * Author: MrCrayfish
 */
public abstract sealed class ConfigProperty<T> implements ConfigManager.IMapEntry permits DoubleProperty, IntProperty
{
    protected final T defaultValue;
    private T value;
    private boolean cached;
    private ConfigManager.ValueProxy proxy;

    public ConfigProperty(T defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    private boolean isLinked()
    {
        return this.proxy != null && this.proxy.isLinked();
    }

    public T get()
    {
        if(!this.isLinked())
            throw new IllegalStateException("Config property is not linked yet");
        if(!this.cached) {
            this.value = this.proxy.get();
            this.cached = true;
        }
        return this.value;
    }

    public void set(T value)
    {
        if(!this.isLinked())
            throw new IllegalStateException("Config property is not linked yet");
        this.value = value;
        this.proxy.set(value);
    }

    public T getDefaultValue()
    {
        return this.defaultValue;
    }

    public void invalidateCache()
    {
        this.cached = false;
    }

    public abstract void defineSpec(ConfigSpec spec, String path);

    public final void updateProxy(ConfigManager.ValueProxy proxy)
    {
        if(proxy != null)
        {
            this.proxy = proxy;
            this.invalidateCache();
        }
    }
}
