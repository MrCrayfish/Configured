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
        spec.defineInRange(this.data.getPath(), this.defaultValue, this.minValue, this.maxValue);
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
