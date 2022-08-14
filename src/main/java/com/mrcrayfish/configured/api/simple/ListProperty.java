package com.mrcrayfish.configured.api.simple;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.google.common.base.Preconditions;

import java.util.List;
import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 */
public final class ListProperty<T> extends ConfigProperty<List<T>>
{
    public static final Type<Boolean> BOOL = new Type<>(Boolean.class);
    public static final Type<Double> DOUBLE = new Type<>(Double.class);
    public static final Type<Integer> INT = new Type<>(Integer.class);
    public static final Type<Long> LONG = new Type<>(Long.class);
    public static final Type<String> STRING = new Type<>(String.class);

    private final Supplier<List<T>> defaultList;
    private final Type<T> type;

    ListProperty(Supplier<List<T>> defaultList, Type<T> type)
    {
        super(null, (config, path) -> config.getOrElse(path, defaultList.get()));
        this.defaultList = defaultList;
        this.type = type;
    }

    public Type<T> getType()
    {
        return this.type;
    }

    @Override
    public List<T> getDefaultValue()
    {
        return this.defaultList.get();
    }

    @Override
    public void defineSpec(ConfigSpec spec)
    {
        Preconditions.checkState(this.data != null, "Config property is not initialized yet");
        spec.defineList(this.data.getPath(), this.defaultList::get, e -> {
            return e != null && this.type.getClassType().isAssignableFrom(e.getClass());
        });
    }

    public static <T> ListProperty<T> create(Type<T> type, Supplier<List<T>> defaultList)
    {
        return new ListProperty<>(defaultList, type);
    }

    public static class Type<T>
    {
        private final Class<T> classType;

        private Type(Class<T> classType)
        {
            this.classType = classType;
        }

        public Class<T> getClassType()
        {
            return this.classType;
        }
    }
}
