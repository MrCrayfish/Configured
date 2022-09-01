package com.mrcrayfish.configured.api.simple;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.EnumGetMethod;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Author: MrCrayfish
 */
public final class EnumProperty<T extends Enum<T>> extends ConfigProperty<T>
{
    private final Set<T> allowedValues;

    EnumProperty(T defaultValue, Set<T> allowedValues)
    {
        super(defaultValue, (config, path) -> config.getEnumOrElse(path, defaultValue));
        this.allowedValues = ImmutableSet.copyOf(allowedValues);
    }

    @Override
    public void defineSpec(ConfigSpec spec)
    {
        Preconditions.checkState(this.data != null, "Config property is not initialized yet");
        spec.defineRestrictedEnum(this.data.getPath(), this.defaultValue, this.allowedValues, EnumGetMethod.NAME_IGNORECASE);
    }

    @Override
    public boolean isValid(T value)
    {
        return value != null && this.allowedValues.contains(value);
    }

    public static <T extends Enum<T>> EnumProperty<T> create(T defaultValue)
    {
        return create(defaultValue, Set.of(defaultValue.getDeclaringClass().getEnumConstants()));
    }

    public static <T extends Enum<T>> EnumProperty<T> create(T defaultValue, Set<T> allowedValues)
    {
        return new EnumProperty<>(defaultValue, allowedValues);
    }
}
