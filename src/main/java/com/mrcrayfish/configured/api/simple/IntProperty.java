package com.mrcrayfish.configured.api.simple;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.google.common.base.Preconditions;

/**
 * Author: MrCrayfish
 */
public final class IntProperty extends ConfigProperty<Integer>
{
    private final int minValue;
    private final int maxValue;

    IntProperty(int defaultValue, int minValue, int maxValue)
    {
        super(defaultValue, (config, path) -> config.getIntOrElse(path, defaultValue));
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    @Override
    public void defineSpec(ConfigSpec spec)
    {
        Preconditions.checkState(this.data != null, "Config property is not initialized yet");
        spec.defineInRange(this.data.getPath(), this.defaultValue, this.minValue, this.maxValue);
    }

    @Override
    public boolean isValid(Integer value)
    {
        return value != null && value.compareTo(this.minValue) >= 0 && value.compareTo(this.maxValue) <= 0;
    }

    public static IntProperty create(int defaultValue)
    {
        return create(defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public static IntProperty create(int defaultValue, int minValue, int maxValue)
    {
        return new IntProperty(defaultValue, minValue, maxValue);
    }
}
