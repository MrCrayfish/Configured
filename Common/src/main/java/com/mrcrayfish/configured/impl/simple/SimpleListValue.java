package com.mrcrayfish.configured.impl.simple;

import com.mrcrayfish.configured.api.simple.ListProperty;
import com.mrcrayfish.configured.client.screen.list.IListConfigValue;
import com.mrcrayfish.configured.client.screen.list.IListType;
import com.mrcrayfish.configured.client.screen.list.ListTypes;
import net.minecraft.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: MrCrayfish
 */
public class SimpleListValue<T> extends SimpleValue<List<T>> implements IListConfigValue<T>
{
    private static final Map<ListProperty.Type<?>, IListType<?>> LIST_TYPE_RESOLVER = Util.make(new HashMap<>(), map -> {
        map.put(ListProperty.INT, ListTypes.INTEGER);
        map.put(ListProperty.LONG, ListTypes.LONG);
        map.put(ListProperty.DOUBLE, ListTypes.DOUBLE);
        map.put(ListProperty.BOOL, ListTypes.BOOLEAN);
        map.put(ListProperty.STRING, ListTypes.STRING);
    });

    private final ListProperty<T> property;

    public SimpleListValue(ListProperty<T> property)
    {
        super(property);
        this.property = property;
    }

    @SuppressWarnings("unchecked")
    @Override
    public IListType<T> getListType()
    {
        return (IListType<T>) LIST_TYPE_RESOLVER.getOrDefault(this.property.getType(), ListTypes.getUnknown());
    }

    @Override
    public boolean isDefault()
    {
        return ListProperty.compareLists(this.get(), this.defaultValue, this.property.getType());
    }
}
