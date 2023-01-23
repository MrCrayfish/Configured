package com.mrcrayfish.configured.impl.forge;

import com.mrcrayfish.configured.client.screen.list.IListConfigValue;
import com.mrcrayfish.configured.client.screen.list.IListType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import net.minecraftforge.common.ForgeConfigSpec.ValueSpec;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public class ForgeListValue<T> extends ForgeValue<List<T>> implements IListConfigValue<T>
{
    protected final @Nullable Function<List<T>, List<T>> converter;

    public ForgeListValue(ConfigValue<List<T>> configValue, ValueSpec valueSpec)
    {
        super(configValue, valueSpec);
        this.converter = this.createConverter(configValue);
    }

    @Nullable
    private Function<List<T>, List<T>> createConverter(ForgeConfigSpec.ConfigValue<List<T>> configValue)
    {
        List<T> original = configValue.get();
        if(original instanceof ArrayList)
        {
            return ArrayList::new;
        }
        else if(original instanceof LinkedList)
        {
            return LinkedList::new;
        }
        return null;
    }

    @Override
    public void set(List<T> value)
    {
        this.valueSpec.correct(value);
        super.set(new ArrayList<>(value));
    }

    @Nullable
    public List<T> getConverted()
    {
        if(this.converter != null)
        {
            return this.converter.apply(get());
        }
        return null;
    }

    @Override
    public IListType<T> getListType()
    {
        return null;
    }
}