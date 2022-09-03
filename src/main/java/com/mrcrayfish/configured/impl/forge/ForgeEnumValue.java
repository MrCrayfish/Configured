package com.mrcrayfish.configured.impl.forge;

import com.mrcrayfish.configured.api.IAllowedValues;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.HashSet;
import java.util.Set;

/**
 * Author: MrCrayfish
 */
public class ForgeEnumValue<T extends Enum<T>> extends ForgeValue<T> implements IAllowedValues<T>
{
    public ForgeEnumValue(ForgeConfigSpec.EnumValue<T> configValue, ForgeConfigSpec.ValueSpec valueSpec)
    {
        super(configValue, valueSpec);
    }

    @Override
    public Set<T> getAllowedValues()
    {
        Set<T> allowedValues = new HashSet<>();
        T[] enums = this.initialValue.getDeclaringClass().getEnumConstants();
        for(T e : enums)
        {
            if(this.valueSpec.test(e))
            {
                allowedValues.add(e);
            }
        }
        return allowedValues;
    }
}
