package com.mrcrayfish.configured.impl.jei;

import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.simple.validate.Validator;
import mezz.jei.common.config.file.ConfigValue;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Author: MrCrayfish
 */
public class JeiValue<T> implements IConfigValue<T>
{
    protected final ConfigValue<T> configValue;
    protected final Validator<T> validator;
    protected final T initialValue;
    protected T value;

    public JeiValue(ConfigValue<T> configValue)
    {
        this(configValue, null);
    }

    public JeiValue(ConfigValue<T> configValue, Validator<T> validator)
    {
        this.configValue = configValue;
        this.validator = validator;
        this.initialValue = configValue.getValue();
        this.set(configValue.getValue());
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
        return this.validator == null || this.validator.test(value);
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

    @Nullable
    @Override
    public Component getComment()
    {
        return Component.literal(this.configValue.getDescription());
    }

    @Nullable
    @Override
    public String getTranslationKey()
    {
        return null;
    }

    @Nullable
    @Override
    public Component getValidationHint()
    {
        return this.validator != null ? this.validator.getHint() : null;
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

    public ConfigValue<T> getConfigValue()
    {
        return this.configValue;
    }
}
