package com.mrcrayfish.configured.api.simple;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.EnumGetMethod;
import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.Collection;

/**
 * Author: MrCrayfish
 */
public final class EnumProperty<T extends Enum<T>> extends ConfigProperty<T>
{
    private final EnumGetMethod method;

    EnumProperty(T defaultValue, EnumGetMethod method)
    {
        super(defaultValue, (config, path) -> config.getEnumOrElse(path, defaultValue));
        this.method = method;
    }

    @Override
    public void defineSpec(ConfigSpec spec)
    {
        Preconditions.checkState(this.data != null, "Config property is not initialized yet");
        Collection<T> values = Arrays.asList(this.defaultValue.getDeclaringClass().getEnumConstants());
        spec.defineRestrictedEnum(this.data.getPath(), this.defaultValue, values, this.method);
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
