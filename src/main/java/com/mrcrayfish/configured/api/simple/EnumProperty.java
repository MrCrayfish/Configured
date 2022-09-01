package com.mrcrayfish.configured.api.simple;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.EnumGetMethod;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.mrcrayfish.configured.api.simple.validate.Validator;

import java.util.Set;

/**
 * Author: MrCrayfish
 */
public final class EnumProperty<T extends Enum<T>> extends ConfigProperty<T>
{
    private final Set<T> allowedValues; //TODO only show these values on change enum screen

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

    /**
     * Creates an EnumProperty with the given default value. By default, all enum constants from
     * the default value's declaring class will be considered acceptable values. If more control is
     * needed, use {@link #create(Enum, Set)}
     *
     * @param defaultValue the default value of this property
     * @return a new EnumProperty instance
     */
    public static <T extends Enum<T>> EnumProperty<T> create(T defaultValue)
    {
        return create(defaultValue, Set.of(defaultValue.getDeclaringClass().getEnumConstants()));
    }

    /**
     * Creates an EnumProperty with the given default value and the enum values that are considered
     * acceptable values. This method allows the enum property to be restricted to a subset of
     * the enum values instead of all the enum constants from the default value's declaring class.
     *
     * @param defaultValue  the default value of this property
     * @param allowedValues the enum values that are allowed values
     * @param <T>           the enum type
     * @return a new EnumProperty instance
     */
    public static <T extends Enum<T>> EnumProperty<T> create(T defaultValue, Set<T> allowedValues)
    {
        return new EnumProperty<>(defaultValue, allowedValues);
    }
}
