package com.mrcrayfish.configured.impl.jei;

import com.mrcrayfish.configured.api.IConfigValue;
import mezz.jei.api.runtime.config.IJeiConfigValue;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Author: MrCrayfish
 */
public class JeiValue<T> implements IConfigValue<T>
{
    protected final IJeiConfigValue<T> configValue;
    protected final T initialValue;
    protected T value;

    public JeiValue(IJeiConfigValue<T> configValue)
    {
        this.configValue = configValue;
        this.initialValue = configValue.getValue();
        this.value = configValue.getValue();
    }

    @Override
    public T get()
    {
        return this.value;
    }

    @Override
    public T getDefault()
    {
        return this.configValue.getDefaultValue();
    }

    @Override
    public void set(T value)
    {
        this.value = value;
    }

    @Override
    public boolean isValid(T value)
    {
        return this.value.getClass().isInstance(value) &&
                this.configValue.isValid(value);
    }

    @Override
    public boolean isDefault()
    {
        return Objects.equals(this.get(), this.configValue.getDefaultValue());
    }

    @Override
    public boolean isChanged()
    {
        return !Objects.equals(this.get(), this.initialValue);
    }

    @Override
    public void restore()
    {
        this.set(this.configValue.getDefaultValue());
    }

    @Override
    public @Nullable Component getComment()
    {
        return Component.literal(this.configValue.getDescription());
    }

    @Override
    public @Nullable String getTranslationKey()
    {
        return null;
    }

    @Override
    public @Nullable Component getValidationHint()
    {
        String validValuesDescription = this.configValue.getValidValuesDescription();
        return Component.literal(validValuesDescription);
    }

    @Override
    public String getName()
    {
        return this.configValue.getName();
    }

    @Override
    public void cleanCache()
    {
    }

    @Override
    public boolean requiresWorldRestart()
    {
        return false;
    }

    @Override
    public boolean requiresGameRestart()
    {
        return false;
    }

    public void updateConfigValue()
    {
        this.configValue.set(this.value);
    }
}
