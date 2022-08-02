package com.mrcrayfish.configured.api.config;

import com.electronwill.nightconfig.core.ConfigSpec;

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
    public void defineSpec(ConfigSpec spec, String path)
    {
        spec.defineInRange(path, this.defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }
}
