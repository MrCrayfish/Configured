package com.mrcrayfish.configured.api.simple;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.common.base.Preconditions;
import com.mrcrayfish.configured.api.simple.validate.Validator;
import com.mrcrayfish.configured.config.ConfigManager;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Author: MrCrayfish
 */
public abstract sealed class ConfigProperty<T> implements ConfigManager.IMapEntry permits BoolProperty, DoubleProperty, EnumProperty, IntProperty, ListProperty, LongProperty, StringProperty
{
    protected final T defaultValue;
    protected final BiFunction<UnmodifiableConfig, List<String>, T> getFunction;
    protected final Validator<T> validator;
    private T value;
    private boolean cached;
    protected ConfigManager.ValueProxy proxy;
    protected ConfigManager.PropertyData data;

    ConfigProperty(T defaultValue)
    {
        this(defaultValue, (Validator<T>) null);
    }

    ConfigProperty(T defaultValue, Validator<T> validator)
    {
        this(defaultValue, (config, path) -> config.getOrElse(path, defaultValue), validator);
    }

    ConfigProperty(T defaultValue, BiFunction<UnmodifiableConfig, List<String>, T> getFunction)
    {
        this(defaultValue, getFunction, null);
    }

    ConfigProperty(T defaultValue, BiFunction<UnmodifiableConfig, List<String>, T> getFunction, Validator<T> validator)
    {
        this.defaultValue = defaultValue;
        this.getFunction = getFunction;
        this.validator = validator;
    }

    /**
     * Internal method. Used for setting up config specifications
     */
    public abstract void defineSpec(ConfigSpec spec);

    /**
     * @param value The value to test
     * @return True if the given value is valid for this property
     */
    public abstract boolean isValid(T value);

    /**
     * @return The validator for this property
     */
    @Nullable
    public Validator<T> getValidator()
    {
        return this.validator;
    }

    private boolean isLinked()
    {
        return this.proxy != null && this.proxy.isLinked();
    }

    /**
     * Gets the value of this property. If this method is called and the property is not linked, an
     * exception will be thrown. The link between the property and the config changes based on the
     * config type. A universal config type will always be linked, while a server config type is
     * only linked when the world/server is running.
     *
     * This value is cached, which means recalling this method will use the cached value. Invalidating
     * the cache is handled automatically, however if the cache needs to be cleared manually, see
     * {@link #invalidateCache()}.
     *
     * @return The value of this property
     */
    public T get()
    {
        if(!this.isLinked())
            throw new IllegalStateException("Config property is not linked yet");
        if(!this.cached) {
            this.value = this.proxy.get(this.getFunction);
            this.cached = true;
        }
        return this.value;
    }

    /**
     * Sets the value of this property. If the config which owns this property is read-only, the
     * call will simply be ignored. If this method is called and the property is not linked, an
     * exception will be thrown. The link between the property and the config changes based on the
     * config type. A universal config type will always be linked, while a server config type is
     * only linked when the world/server is running.
     */
    public void set(T value)
    {
        if(!this.isLinked())
            throw new IllegalStateException("Config property is not linked yet");
        if(this.proxy.isWritable()) {
            this.value = value;
            this.proxy.set(value);
        }
    }

    /**
     * @return The default value of this property
     */
    public T getDefaultValue()
    {
        return this.defaultValue;
    }

    /**
     * Restores this property to its default value. See {@link #set(Object)} for information about
     * when calling this method is acceptable. An exception will be thrown if called during the
     * wrong state.
     */
    public void restoreDefault()
    {
        this.set(this.getDefaultValue());
    }

    /**
     * Checks if this property is the same as the default value. Since this method calls {@link #get()},
     * see the documentation of that method to determine when it's acceptable to call this method. An
     * exception will be thrown if called during the wrong state.
     *
     * @return True if the current value matches the default value, otherwise false.
     */
    public boolean isDefault()
    {
        return Objects.equals(this.get(), this.getDefaultValue());
    }

    /**
     * Invalidates the cache of this property. The value of the property will be updated next time
     * {@link #get()} is called.
     */
    public void invalidateCache()
    {
        this.cached = false;
    }

    /**
     * Gets the name of this property. The name is only available after this property has been
     * initialized with it's required data. An exception will be thrown if called before initialization.
     *
     * @return The name of this property
     */
    public String getName()
    {
        Preconditions.checkState(this.data != null, "Config property is not initialized yet");
        return this.data.getName();
    }

    /**
     * Gets the path of this property. The path is only available after this property has been
     * initialized with it's required data. An exception will be thrown if called before initialization.
     *
     * @return The path of this property
     */
    public List<String> getPath()
    {
        Preconditions.checkState(this.data != null, "Config property is not initialized yet");
        return this.data.getPath();
    }

    /**
     * Gets the translation key of this property. The translation key  is only available after this
     * property has been initialized with it's required data. An exception will be thrown if called
     * before initialization.
     *
     * @return The translation key of this property
     */
    public String getTranslationKey()
    {
        Preconditions.checkState(this.data != null, "Config property is not initialized yet");
        return this.data.getTranslationKey();
    }

    /**
     * Gets the comment of this property. The comment is only available after this property has been
     * initialized with it's required data. An exception will be thrown if called before initialization.
     *
     * @return The comment of this property
     */
    public String getComment()
    {
        Preconditions.checkState(this.data != null, "Config property is not initialized yet");
        return this.data.getComment();
    }

    /**
     * @return True if this property needs the world to be reloaded for changes to take effect
     */
    public boolean requiresWorldRestart()
    {
        Preconditions.checkState(this.data != null, "Config property is not initialized yet");
        return this.data.requiresWorldRestart();
    }

    /**
     * @return True if this property needs the game needs to be restarted for changes to take effect
     */
    public boolean requiresGameRestart()
    {
        Preconditions.checkState(this.data != null, "Config property is not initialized yet");
        return this.data.requiresGameRestart();
    }

    /**
     * Internal method. Used for linking this property to a config
     */
    public final void updateProxy(ConfigManager.ValueProxy proxy)
    {
        Preconditions.checkNotNull(proxy, "Tried to update config property with a null value proxy");
        this.proxy = proxy;
        this.invalidateCache();
    }

    /**
     * Internal method. Used for initializing the property with data required for it to work
     */
    public final void initProperty(ConfigManager.PropertyData data)
    {
        Preconditions.checkNotNull(data, "Tried to update path with a null path object");
        if(this.data == null)
        {
            this.data = data;
        }
    }
}
