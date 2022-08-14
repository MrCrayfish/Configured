package com.mrcrayfish.configured.api.simple;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.EnumGetMethod;
import com.google.common.base.Preconditions;

/**
 * Author: MrCrayfish
 */
public final class EnumProperty<T extends Enum<T>> extends ConfigProperty<T>
{
    EnumProperty(T defaultValue)
    {
        super(defaultValue, (config, path) -> config.getEnumOrElse(path, defaultValue));
    }

    @Override
    public void defineSpec(ConfigSpec spec)
    {
        Preconditions.checkState(this.data != null, "Config property is not initialized yet");
        spec.defineEnum(this.data.getPath(), this.defaultValue, EnumGetMethod.NAME_IGNORECASE);
    }

    public static <T extends Enum<T>> EnumProperty<T> create(T defaultValue)
    {
        return new EnumProperty<>(defaultValue);
    }
}
