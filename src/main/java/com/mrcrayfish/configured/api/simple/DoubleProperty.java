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

    /**
     * Creates a DoubleProperty with the given default value. This property will accept any parsable
     * double as a valid value. To restrict the value to bounds, use {@link #create(double, double, double)}
     * or {@link #create(double, Validator)} for custom validation.
     *
     * @param defaultValue the default value of this property
     * @return a new DoubleProperty instance
     */
    public static DoubleProperty create(double defaultValue)
    {
        return create(defaultValue, -Double.MAX_VALUE, Double.MAX_VALUE);
    }

    /**
     * Creates a DoubleProperty with the given default value and sets the valid bounds of this
     * property. The min and max value are inclusive of the validation check. If the min value is
     * greater than the max value, an exception will be thrown.
     *
     * @param defaultValue the default value of this property
     * @param minValue     the minimum bound of the double (inclusive)
     * @param maxValue     the maximum bound of the double (inclusive)
     * @return a new DoubleProperty instance
     */
    public static DoubleProperty create(double defaultValue, double minValue, double maxValue)
    {
        return create(defaultValue, new NumberRange<>(minValue, maxValue));
    }

    /**
     * Creates a DoubleProperty with the given default value and validator
     *
     * @param defaultValue the default value of this property
     * @param validator    a validator to determine valid values for this property
     * @return a new DoubleProperty instance
     */
    public static DoubleProperty create(double defaultValue, Validator<Double> validator)
    {
        return new DoubleProperty(defaultValue, validator);
    }
}
