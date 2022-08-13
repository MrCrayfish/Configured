package com.mrcrayfish.configured.api.simple;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.google.common.base.Preconditions;
import com.mrcrayfish.configured.config.ConfigManager;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Author: MrCrayfish
 */
public abstract sealed class ConfigProperty<T> implements ConfigManager.IMapEntry permits DoubleProperty, IntProperty
{
    protected final T defaultValue;
    private T value;
    private boolean cached;
    private ConfigManager.ValueProxy proxy;
    protected ConfigManager.PropertyData data;

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

    public String getName()
    {
        Preconditions.checkState(this.data != null, "Config property is not initialized yet");
        return this.data.getName();
    }

    public List<String> getPath()
    {
        Preconditions.checkState(this.data != null, "Config property is not initialized yet");
        return this.data.getPath();
    }

    public String getTranslationKey()
    {
        Preconditions.checkState(this.data != null, "Config property is not initialized yet");
        return this.data.getTranslationKey();
    }

    public String getComment()
    {
        Preconditions.checkState(this.data != null, "Config property is not initialized yet");
        return this.data.getComment();
    }

    public final void updateProxy(ConfigManager.ValueProxy proxy)
    {
        Preconditions.checkNotNull(proxy, "Tried to update config property with a null value proxy");
        this.proxy = proxy;
        this.invalidateCache();
    }

    public final void initProperty(ConfigManager.PropertyData data)
    {
        Preconditions.checkNotNull(data, "Tried to update path with a null path object");
        if(this.data == null)
        {
            this.data = data;
        }
    }

    public abstract void defineSpec(ConfigSpec spec);
}