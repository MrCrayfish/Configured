package com.mrcrayfish.configured.api.simple;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.google.common.base.Preconditions;
import com.mrcrayfish.configured.api.simple.validate.NumberRange;
import com.mrcrayfish.configured.api.simple.validate.Validator;

/**
 * Author: MrCrayfish
 */
public final class IntProperty extends ConfigProperty<Integer>
{
    IntProperty(int defaultValue, Validator<Integer> validator)
    {
        super(defaultValue, (config, path) -> config.getIntOrElse(path, defaultValue), validator);
    }

    @Override
    public void defineSpec(ConfigSpec spec)
    {
        Preconditions.checkState(this.data != null, "Config property is not initialized yet");
        spec.define(this.data.getPath(), this.defaultValue, e -> e instanceof Integer && this.isValid((Integer) e));
    }

    @Override
    public boolean isValid(Integer value)
    {
        return value != null && (this.validator == null || this.validator.test(value));
    }

    /**
     * Creates a IntProperty with the given default value. This property will accept any parsable
     * integer as a valid value. To restrict the value to bounds, use {@link #create(int, int, int)}
     * or {@link #create(int, Validator)} for custom validation.
     *
     * @param defaultValue the default value of this property
     * @return a new IntProperty instance
     */
    public static IntProperty create(int defaultValue)
    {
        return create(defaultValue, null);
    }

    /**
     * Creates a IntProperty with the given default value and sets the valid bounds of this
     * property. The min and max value are inclusive of the validation check. If the min value is
     * greater than the max value, an exception will be thrown.
     *
     * @param defaultValue the default value of this property
     * @param minValue     the minimum bound of the integer (inclusive)
     * @param maxValue     the maximum bound of the integer (inclusive)
     * @return a new IntProperty instance
     */
    public static IntProperty create(int defaultValue, int minValue, int maxValue)
    {
        return create(defaultValue, new NumberRange<>(minValue, maxValue));
    }

    /**
     * Creates a IntProperty with the given default value and validator
     *
     * @param defaultValue the default value of this property
     * @param validator    a validator to determine valid values for this property
     * @return a new IntProperty instance
     */
    public static IntProperty create(int defaultValue, Validator<Integer> validator)
    {
        return new IntProperty(defaultValue, validator);
    }
}
