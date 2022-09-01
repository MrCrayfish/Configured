package com.mrcrayfish.configured.api.simple;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.google.common.base.Preconditions;
import com.mrcrayfish.configured.api.simple.validate.NumberRange;
import com.mrcrayfish.configured.api.simple.validate.Validator;

/**
 * Author: MrCrayfish
 */
public final class LongProperty extends ConfigProperty<Long>
{
    LongProperty(long defaultValue, Validator<Long> validator)
    {
        super(defaultValue, (config, path) -> config.getLongOrElse(path, defaultValue), validator);
    }

    @Override
    public void defineSpec(ConfigSpec spec)
    {
        Preconditions.checkState(this.data != null, "Config property is not initialized yet");
        // Custom handling since nightconfig will parse number as an integer when possible
        spec.define(this.data.getPath(), this.defaultValue, o -> {
            if(o instanceof Long || o instanceof Integer) {
                Long value = ((Number) o).longValue();
                return this.isValid(value);
            }
            return false;
        });
    }

    @Override
    public boolean isValid(Long value)
    {
        return value != null && (this.validator == null || this.validator.test(value));
    }

    /**
     * Creates a LongProperty with the given default value. This property will accept any parsable
     * long as a valid value. To restrict the value to bounds, use {@link #create(long, long, long)}
     * or {@link #create(long, Validator)} for custom validation.
     *
     * @param defaultValue the default value of this property
     * @return a new LongProperty instance
     */
    public static LongProperty create(long defaultValue)
    {
        return create(defaultValue, null);
    }

    /**
     * Creates a LongProperty with the given default value and sets the valid bounds of this
     * property. The min and max value are inclusive of the validation check. If the min value is
     * greater than the max value, an exception will be thrown.
     *
     * @param defaultValue the default value of this property
     * @param minValue     the minimum bound of the long (inclusive)
     * @param maxValue     the maximum bound of the long (inclusive)
     * @return a new LongProperty instance
     */
    public static LongProperty create(long defaultValue, long minValue, long maxValue)
    {
        return create(defaultValue, new NumberRange<>(minValue, maxValue));
    }

    /**
     * Creates a LongProperty with the given default value and validator
     *
     * @param defaultValue the default value of this property
     * @param validator    a validator to determine valid values for this property
     * @return a new LongProperty instance
     */
    public static LongProperty create(long defaultValue, Validator<Long> validator)
    {
        return new LongProperty(defaultValue, validator);
    }
}
