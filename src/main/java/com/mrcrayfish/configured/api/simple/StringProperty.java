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

    /**
     * Creates a StringProperty with the given default value. This method will not create any
     * validation for the property and any string is considered valid, even an empty one.
     * See {@link #create(String, Validator)} to specify validation.
     *
     * @param defaultValue the default value of this property
     * @return a new StringProperty instance
     */
    public static StringProperty create(String defaultValue)
    {
        return create(defaultValue, null);
    }

    /**
     * Creates a StringProperty with the given default value and validator
     *
     * @param defaultValue the default value of this property
     * @param validator    a validator to determine valid values for this property
     * @param <V>          the validator must be an implementation that uses the String type
     * @return a new StringProperty instance
     */
    public static <V extends Validator<String>> StringProperty create(String defaultValue, V validator)
    {
        return new StringProperty(defaultValue, validator);
    }
}
