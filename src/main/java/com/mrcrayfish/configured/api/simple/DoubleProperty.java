package com.mrcrayfish.configured.api.simple;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.google.common.base.Preconditions;
import com.mrcrayfish.configured.api.simple.validate.NumberRange;
import com.mrcrayfish.configured.api.simple.validate.Validator;

/**
 * Author: MrCrayfish
 */
public final class DoubleProperty extends ConfigProperty<Double>
{
    DoubleProperty(double defaultValue, Validator<Double> validator)
    {
        super(defaultValue, validator);
    }

    @Override
    public void defineSpec(ConfigSpec spec)
    {
        Preconditions.checkState(this.data != null, "Config property is not initialized yet");
        spec.define(this.data.getPath(), this.defaultValue, e -> e instanceof Double && this.isValid((Double) e));
    }

    @Override
    public boolean isValid(Double value)
    {
        return value != null && (this.validator == null || this.validator.test(value));
    }

    public static DoubleProperty create(double defaultValue)
    {
        return create(defaultValue, Double.MIN_VALUE, Double.MAX_VALUE);
    }

    public static DoubleProperty create(double defaultValue, double minValue, double maxValue)
    {
        return create(defaultValue, new NumberRange<>(minValue, maxValue));
    }

    public static DoubleProperty create(double defaultValue, Validator<Double> validator)
    {
        return new DoubleProperty(defaultValue, validator);
    }
}
