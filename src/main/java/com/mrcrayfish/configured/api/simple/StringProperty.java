package com.mrcrayfish.configured.api.simple;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.google.common.base.Preconditions;
import com.mrcrayfish.configured.api.simple.validate.Validator;

/**
 * Author: MrCrayfish
 */
public final class StringProperty extends ConfigProperty<String>
{
    StringProperty(String defaultValue, Validator<String> validator)
    {
        super(defaultValue, validator);
    }

    @Override
    public void defineSpec(ConfigSpec spec)
    {
        Preconditions.checkState(this.data != null, "Config property is not initialized yet");
        spec.define(this.data.getPath(), this.defaultValue, e -> e instanceof String s && this.isValid(s));
    }

    @Override
    public boolean isValid(String value)
    {
        return value != null && (this.validator == null || this.validator.test(value));
    }

    public static StringProperty create(String defaultValue)
    {
        return create(defaultValue, null);
    }

    public static <V extends Validator<String>> StringProperty create(String defaultValue, V validator)
    {
        return new StringProperty(defaultValue, validator);
    }
}
