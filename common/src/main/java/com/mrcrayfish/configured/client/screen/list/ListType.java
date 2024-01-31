package com.mrcrayfish.configured.client.screen.list;

import net.minecraft.network.chat.Component;

import java.util.function.Function;

public class ListType<T> implements IListType<T>
{
    private final Function<T, String> stringParser;
    private final Function<String, T> valueParser;
    private final String hintKey;

    public ListType(Function<T, String> stringParser, Function<String, T> valueParser, String hintKey)
    {
        this.stringParser = stringParser;
        this.valueParser = valueParser;
        this.hintKey = hintKey;
    }

    @Override
    public Function<T, String> getStringParser()
    {
        return this.stringParser;
    }

    @Override
    public Function<String, T> getValueParser()
    {
        return this.valueParser;
    }

    @Override
    public Component getHint()
    {
        return Component.translatable(this.hintKey);
    }
}
