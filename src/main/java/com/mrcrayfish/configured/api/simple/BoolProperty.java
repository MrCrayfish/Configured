package com.mrcrayfish.configured.api.simple;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.google.common.base.Preconditions;

/**
 * Author: MrCrayfish
 */
public final class BoolProperty extends ConfigProperty<Boolean>
{
    BoolProperty(boolean defaultValue)
    {
        super(defaultValue);
    }

    @Override
    public void defineSpec(ConfigSpec spec)
    {
        Preconditions.checkState(this.data != null, "Config property is not initialized yet");
        spec.define(this.data.getPath(), this.defaultValue);
    }

    public static BoolProperty create(boolean defaultValue)
    {
        return new BoolProperty(defaultValue);
    }
}
