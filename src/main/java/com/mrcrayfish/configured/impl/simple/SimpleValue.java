package com.mrcrayfish.configured.impl.simple;

import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.simple.ConfigProperty;
import com.mrcrayfish.configured.api.simple.validate.Validator;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Author: MrCrayfish
 */
public class SimpleValue<T> implements IConfigValue<T>
{
    private final ConfigProperty<T> property;
    private final T initialValue;
    private final T defaultValue;
    protected T value;

    public SimpleValue(ConfigProperty<T> property)
    {
        this.property = property;
        this.initialValue = property.get();
        this.defaultValue = property.getDefaultValue();
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
        return this.property.isValid(value);
    }

    @Override
    public boolean isDefault()
    {
        return Objects.equals(this.get(), this.defaultValue);
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
        String key = this.getTranslationKey() + ".tooltip";
        if(I18n.exists(key))
        {
            return I18n.get(key);
        }
        return this.property.getComment();
    }

    @Nullable
    @Override
    public String getTranslationKey()
    {
        return this.property.getTranslationKey();
    }

    @Nullable
    @Override
    public Component getValidationHint()
    {
        Validator<T> validator = this.property.getValidator();
        return validator != null ? validator.getHint() : null;
    }

    @Override
    public String getName()
    {
        return this.property.getName();
    }

    @Override
    public void cleanCache()
    {
        this.property.invalidateCache();
    }

    @Nullable
    public List<String> getPath()
    {
        return this.property.getPath();
    }
}
