package com.mrcrayfish.configured.api.config;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.mrcrayfish.configured.config.ConfigManager;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 */
public abstract sealed class ConfigProperty<T> permits DoubleProperty, IntProperty
{
    protected final T defaultValue;
    private T value;
    private boolean loaded;
    private boolean cached;
    private Supplier<T> getter;
    private Consumer<T> setter;

    public ConfigProperty(T defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    public T get()
    {
        if(!this.loaded)
            throw new IllegalStateException("Config property is not loaded yet");
        if(!this.cached) {
            this.value = this.getter.get();
            this.cached = true;
        }
        return this.value;
    }

    public void set(T value)
    {
        if(!this.loaded)
            throw new IllegalStateException("Config property is not loaded yet");
        this.value = value;
        this.setter.accept(value);
    }

    public T getDefaultValue()
    {
        return this.defaultValue;
    }

    public abstract void defineSpec(ConfigSpec spec, String path);

    public final void initialize(ConfigManager.ConfigSupplier<T> supplier)
    {
        if(this.loaded || supplier == null) return;
        this.getter = supplier.getGetter();
        this.setter = supplier.getSetter();
        this.loaded = true;
    }
}
