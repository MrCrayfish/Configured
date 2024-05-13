package com.mrcrayfish.configured.client.screen.list;

import com.mrcrayfish.configured.api.IConfigValue;

import org.jetbrains.annotations.Nullable;
import java.util.List;

public interface IListConfigValue<T> extends IConfigValue<List<T>>
{
    @Nullable
    IListType<T> getListType();
}
