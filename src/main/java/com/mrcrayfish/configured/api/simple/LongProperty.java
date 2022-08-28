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

    public static LongProperty create(long defaultValue)
    {
        return create(defaultValue, null);
    }

    public static LongProperty create(long defaultValue, long minValue, long maxValue)
    {
        return create(defaultValue, new NumberRange<>(minValue, maxValue));
    }

    public static LongProperty create(long defaultValue, Validator<Long> validator)
    {
        return new LongProperty(defaultValue, validator);
    }
}
