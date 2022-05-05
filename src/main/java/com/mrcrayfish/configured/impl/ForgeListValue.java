package com.mrcrayfish.configured.impl;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.ValueSpec;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public class ForgeListValue extends ForgeValue<List<?>>
{
    private final Function<List<?>, List<?>> converter;

    public ForgeListValue(ConfigValue<List<?>> configValue, ValueSpec valueSpec)
    {
        super(configValue, valueSpec);
        converter = createConverter(configValue);
    }

    @Nullable
    private Function<List<?>, List<?>> createConverter(ForgeConfigSpec.ConfigValue<List<?>> configValue)
    {
        List<?> original = configValue.get();
        if(original instanceof ArrayList)
        {
            return T -> new ArrayList<>(T);
        }
        else if(original instanceof LinkedList)
        {
            return T -> new LinkedList<>(T);
        }
        // TODO allow developers to hook custom list
        return null;
    }

    @Override
    public void set(List<?> value)
    {
        valueSpec.correct(value);
        super.set(new ArrayList<>(value));
    }

    public Function<List<?>, List<?>> getConverter()
    {
        return converter;
    }
}