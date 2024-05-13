package com.mrcrayfish.configured.impl.neoforge;

import com.mrcrayfish.configured.client.screen.list.IListConfigValue;
import com.mrcrayfish.configured.client.screen.list.IListType;
import net.neoforged.neoforge.common.ModConfigSpec;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public class NeoForgeListValue<T> extends NeoForgeValue<List<T>> implements IListConfigValue<T>
{
    protected final @Nullable Function<List<T>, List<T>> converter;

    public NeoForgeListValue(ModConfigSpec.ConfigValue<List<T>> configValue, ModConfigSpec.ValueSpec valueSpec)
    {
        super(configValue, valueSpec);
        this.converter = this.createConverter(configValue);
    }

    @Nullable
    private Function<List<T>, List<T>> createConverter(ModConfigSpec.ConfigValue<List<T>> configValue)
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