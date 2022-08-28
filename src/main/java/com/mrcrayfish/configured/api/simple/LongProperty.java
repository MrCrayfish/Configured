package com.mrcrayfish.configured.api.simple;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.google.common.base.Preconditions;

/**
 * Author: MrCrayfish
 */
public final class LongProperty extends ConfigProperty<Long>
{
    private final long minValue;
    private final long maxValue;

    LongProperty(long defaultValue, long minValue, long maxValue)
    {
        super(defaultValue, (config, path) -> config.getLongOrElse(path, defaultValue));
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    @Override
    public void defineSpec(ConfigSpec spec)
    {
        Preconditions.checkState(this.data != null, "Config property is not initialized yet");
        // Custom handling since nightconfig will parse number as an integer when possible
        spec.define(this.data.getPath(), this.defaultValue, o -> {
            if(o instanceof Long || o instanceof Integer) {
                Long value = ((Number) o).longValue();
                return value.compareTo(this.minValue) >= 0 && value.compareTo(this.maxValue) <= 0;
            }
            return false;
        });
    }

    @Override
    public boolean isValid(Long value)
    {
        return value != null && value.compareTo(this.minValue) >= 0 && value.compareTo(this.maxValue) <= 0;
    }

    public static LongProperty create(long defaultValue)
    {
        return new LongProperty(defaultValue, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    public static LongProperty create(long defaultValue, long minValue, long maxValue)
    {
        return new LongProperty(defaultValue, minValue, maxValue);
    }
}
