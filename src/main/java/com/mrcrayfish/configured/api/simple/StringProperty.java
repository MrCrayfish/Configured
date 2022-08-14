package com.mrcrayfish.configured.api.simple;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.google.common.base.Preconditions;

/**
 * Author: MrCrayfish
 */
public final class StringProperty extends ConfigProperty<String>
{
    StringProperty(String defaultValue)
    {
        super(defaultValue);
    }

    @Override
    public void defineSpec(ConfigSpec spec)
    {
        Preconditions.checkState(this.data != null, "Config property is not initialized yet");
        spec.define(this.data.getPath(), this.defaultValue);
    }

    public static StringProperty create(String defaultValue)
    {
        return new StringProperty(defaultValue);
    }
}
