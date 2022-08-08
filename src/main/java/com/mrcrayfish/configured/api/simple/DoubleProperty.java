package com.mrcrayfish.configured.api.simple;

import com.electronwill.nightconfig.core.ConfigSpec;

/**
 * Author: MrCrayfish
 */
public final class DoubleProperty extends ConfigProperty<Double>
{
    private final double minValue;
    private final double maxValue;

    DoubleProperty(double defaultValue, double minValue, double maxValue)
    {
        super(defaultValue);
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public double getMinValue()
    {
        return this.minValue;
    }

    public double getMaxValue()
    {
        return this.maxValue;
    }

    public void defineSpec(ConfigSpec spec, String path)
    {
        spec.defineInRange(path, this.defaultValue, this.minValue, this.maxValue);
    }

    public static DoubleProperty create(double defaultValue, double minValue, double maxValue)
    {
        return new DoubleProperty(defaultValue, minValue, maxValue);
    }
}
