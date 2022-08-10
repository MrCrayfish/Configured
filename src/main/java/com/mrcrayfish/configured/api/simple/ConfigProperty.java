package com.mrcrayfish.configured.api.simple;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.google.common.base.Preconditions;
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
    private ConfigManager.ValuePath valuePath;

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

    @Override
    public String getPath()
    {
        return this.valuePath != null ? this.valuePath.getPath() : "";
    }

    public abstract void defineSpec(ConfigSpec spec);

    public final void updateProxy(ConfigManager.ValueProxy proxy)
    {
        Preconditions.checkNotNull(proxy, "Tried to update config property with a null value proxy");
        this.proxy = proxy;
        this.invalidateCache();
    }

    public final void initPath(ConfigManager.ValuePath valuePath)
    {
        Preconditions.checkNotNull(valuePath, "Tried to update path with a null path object");
        Preconditions.checkState(this.valuePath == null, "A path can only be set once");
        this.valuePath = valuePath;
    }
}
