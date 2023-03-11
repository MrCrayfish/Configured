package com.mrcrayfish.configured.client.screen.list;

import net.minecraft.network.chat.Component;

import java.util.function.Function;

public interface IListType<T>
{
    Function<T, String> getStringParser();

    Function<String, T> getValueParser();

    Component getHint();
}
