package com.mrcrayfish.configured.api.simple;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.google.common.base.Preconditions;

/**
 * Author: MrCrayfish
 */
public final class IntProperty extends ConfigProperty<Integer>
{
    public IntProperty(int defaultValue)
    {
        super(defaultValue);
    }

    public static IntProperty create(int defaultValue)
    {
        return new IntProperty(defaultValue);
    }

    @Override
    public void defineSpec(ConfigSpec spec)
    {
        Preconditions.checkState(this.data != null, "");
        spec.defineInRange(this.data.getPath(), this.defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
}
