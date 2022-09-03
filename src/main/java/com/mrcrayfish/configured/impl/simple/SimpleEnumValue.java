package com.mrcrayfish.configured.impl.simple;

import com.mrcrayfish.configured.api.IAllowedValues;
import com.mrcrayfish.configured.api.simple.EnumProperty;

import java.util.Set;

/**
 * Author: MrCrayfish
 */
public class SimpleEnumValue<T extends Enum<T>> extends SimpleValue<T> implements IAllowedValues<T>
{
    public SimpleEnumValue(EnumProperty<T> enumProperty)
    {
        super(enumProperty);
    }

    @Override
    public Set<T> getAllowedValues()
    {
        return ((EnumProperty<T>) this.property).getAllowedValues();
    }
}
