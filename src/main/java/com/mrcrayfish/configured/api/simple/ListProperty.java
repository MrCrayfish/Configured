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
    public static final Type<String> STRING = new Type<>(String.class);

    private final Type<T> type;

    ListProperty(List<T> defaultValue, Type<T> type)
    {
        super(defaultValue);
        this.type = type;
    }

    @Override
    public void defineSpec(ConfigSpec spec)
    {
        Preconditions.checkState(this.data != null, "Config property is not initialized yet");
        spec.defineList(this.data.getPath(), this.defaultValue, e -> {
            return e != null && this.type.getClassType().isAssignableFrom(e.getClass());
        });
    }

    public static <T> ListProperty<T> create(Type<T> type, Supplier<List<T>> defaultList)
    {
        return new ListProperty<>(defaultList.get(), type);
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
