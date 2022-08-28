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

    public static IntProperty create(int defaultValue)
    {
        return create(defaultValue, null);
    }

    public static IntProperty create(int defaultValue, int minValue, int maxValue)
    {
        return create(defaultValue, new NumberRange<>(minValue, maxValue));
    }

    public static IntProperty create(int defaultValue, Validator<Integer> validator)
    {
        return new IntProperty(defaultValue, validator);
    }
}
