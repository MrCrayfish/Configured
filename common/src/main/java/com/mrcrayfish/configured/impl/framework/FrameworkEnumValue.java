package com.mrcrayfish.configured.impl.framework;

import com.mrcrayfish.configured.api.IAllowedEnums;
import com.mrcrayfish.framework.api.config.EnumProperty;

import java.util.Set;

/**
 * Author: MrCrayfish
 */
public class FrameworkEnumValue<T extends Enum<T>> extends FrameworkValue<T> implements IAllowedEnums<T>
{
    public FrameworkEnumValue(EnumProperty<T> enumProperty)
    {
        super(enumProperty);
    }

    @Override
    public Set<T> getAllowedValues()
    {
        return ((EnumProperty<T>) this.property).getAllowedValues();
    }
}
