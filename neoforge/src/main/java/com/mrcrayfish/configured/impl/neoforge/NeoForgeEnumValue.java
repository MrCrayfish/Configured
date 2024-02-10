package com.mrcrayfish.configured.impl.neoforge;

import com.mrcrayfish.configured.api.IAllowedEnums;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.HashSet;
import java.util.Set;

/**
 * Author: MrCrayfish
 */
public class NeoForgeEnumValue<T extends Enum<T>> extends NeoForgeValue<T> implements IAllowedEnums<T>
{
    public NeoForgeEnumValue(ModConfigSpec.EnumValue<T> configValue, ModConfigSpec.ValueSpec valueSpec)
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
