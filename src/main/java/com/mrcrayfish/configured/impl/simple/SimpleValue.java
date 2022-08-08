package com.mrcrayfish.configured.impl.simple;

import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.simple.ConfigProperty;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Author: MrCrayfish
 */
public class SimpleValue<T> implements IConfigValue<T>
{
    private final String name;
    private final String translationKey;
    private final ConfigProperty<T> property;
    private final T initialValue;
    protected T value;

    public SimpleValue(String name, String translationKey, ConfigProperty<T> property)
    {
        this.name = name;
        this.translationKey = translationKey;
        this.property = property;
        this.initialValue = property.get();
        this.set(property.get());
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
    public T getDefault()
    {
        return this.property.getDefaultValue();
    }

    @Override
    public boolean isValid(T value)
    {
        //TODO validation
        return true;
    }

    @Override
    public boolean isDefault()
    {
        return Objects.equals(this.get(), this.property.getDefaultValue());
    }

    @Override
    public boolean isChanged()
    {
        return !Objects.equals(this.get(), this.initialValue);
    }

    @Override
    public void restore()
    {
        this.set(this.property.getDefaultValue());
    }

    @Nullable
    @Override
    public String getComment()
    {
        //TODO comments
        return null;
    }

    @Nullable
    @Override
    public String getTranslationKey()
    {
        return this.translationKey;
    }

    @Override
    public String getPath()
    {
        return this.name;
    }

    @Override
    public void cleanCache()
    {
        this.property.invalidateCache();
    }
}
