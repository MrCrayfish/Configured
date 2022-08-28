package com.mrcrayfish.configured.api.simple;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.EnumGetMethod;
import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Author: MrCrayfish
 */
public final class EnumProperty<T extends Enum<T>> extends ConfigProperty<T>
{
    private final EnumGetMethod method;
    private final List<T> allowedValues;

    EnumProperty(T defaultValue, EnumGetMethod method)
    {
        super(defaultValue, (config, path) -> config.getEnumOrElse(path, defaultValue));
        this.method = method;
        this.allowedValues = Arrays.asList(this.defaultValue.getDeclaringClass().getEnumConstants());
    }

    @Override
    public void defineSpec(ConfigSpec spec)
    {
        Preconditions.checkState(this.data != null, "Config property is not initialized yet");
        spec.defineRestrictedEnum(this.data.getPath(), this.defaultValue, this.allowedValues, this.method);
    }

    @Override
    public boolean isValid(T value)
    {
        return value != null && this.allowedValues.contains(value);
    }

    public static <T extends Enum<T>> EnumProperty<T> create(T defaultValue)
    {
        return new EnumProperty<>(defaultValue, EnumGetMethod.NAME_IGNORECASE);
    }

    public static <T extends Enum<T>> EnumProperty<T> create(T defaultValue, EnumGetMethod method)
    {
        return new EnumProperty<>(defaultValue, method);
    }
}
