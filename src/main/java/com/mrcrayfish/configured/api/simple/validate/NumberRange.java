package com.mrcrayfish.configured.api.simple.validate;

import com.google.common.base.Preconditions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

/**
 * Author: MrCrayfish
 */
public class NumberRange<T extends Number & Comparable<T>> implements Validator<T>
{
    private final T minValue;
    private final T maxValue;

    public NumberRange(T minValue, T maxValue)
    {
        Preconditions.checkArgument(minValue.compareTo(maxValue) <= 0, "Min value must be less than or equal to the max value");
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    @Override
    public boolean test(T value)
    {
        return value.compareTo(this.minValue) >= 0 && value.compareTo(this.maxValue) <= 0;
    }

    @Override
    public Component getHint()
    {
        return new TranslatableComponent("configured.validator.range_hint", this.minValue, this.maxValue);
    }
}
