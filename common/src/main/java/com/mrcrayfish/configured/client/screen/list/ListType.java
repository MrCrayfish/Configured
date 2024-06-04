package com.mrcrayfish.configured.client.screen.list;

import com.mrcrayfish.configured.Constants;
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
        this.valueParser = new ErrorSuppressingParserWrapper<>(valueParser);
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

    private record ErrorSuppressingParserWrapper<T>(Function<String, T> wrapped) implements Function<String, T>
    {
        @Override
        public T apply(String s)
        {
            try
            {
                return this.wrapped.apply(s);
            }
            catch(RuntimeException e)
            {
                Constants.LOG.debug(String.format("Suppressing error parsing value: %s", s), e);
                return null;
            }
        }
    }
}
