package com.mrcrayfish.configured.impl.simple;

import com.mrcrayfish.configured.api.simple.ListProperty;
import com.mrcrayfish.configured.client.screen.EditListScreen;
import net.minecraft.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: MrCrayfish
 */
public class SimpleListValue<T> extends SimpleValue<List<T>>
{
    private static final Map<ListProperty.Type<?>, EditListScreen.ListType> LIST_TYPE_RESOLVER = Util.make(new HashMap<>(), map -> {
        map.put(ListProperty.INT, EditListScreen.ListType.INTEGER);
        map.put(ListProperty.LONG, EditListScreen.ListType.LONG);
        map.put(ListProperty.DOUBLE, EditListScreen.ListType.DOUBLE);
        map.put(ListProperty.BOOL, EditListScreen.ListType.BOOLEAN);
        map.put(ListProperty.STRING, EditListScreen.ListType.STRING);
    });

    private final ListProperty<T> property;

    public SimpleListValue(ListProperty<T> property)
    {
        super(property);
        this.property = property;
    }

    public EditListScreen.ListType getListType()
    {
        return LIST_TYPE_RESOLVER.getOrDefault(this.property.getType(), EditListScreen.ListType.UNKNOWN);
    }

    @Override
    public boolean isDefault()
    {
        return ListProperty.compareLists(this.get(), this.defaultValue, this.property.getType());
    }
}
