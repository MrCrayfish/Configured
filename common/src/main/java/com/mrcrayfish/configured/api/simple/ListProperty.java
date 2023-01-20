package com.mrcrayfish.configured.api.simple;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.google.common.base.Preconditions;
import com.mrcrayfish.configured.api.simple.validate.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 */
public final class ListProperty<T> extends ConfigProperty<List<T>>
{
    public static final Type<Boolean> BOOL = new Type<>(Boolean.class);
    public static final Type<Double> DOUBLE = new Type<>(Double.class);
    public static final Type<Integer> INT = new Type<>(Integer.class);
    public static final Type<Long> LONG = new Type<>(Long.class, Integer.class);
    public static final Type<String> STRING = new Type<>(String.class);

    private final Supplier<List<T>> defaultList;
    private final Type<T> type;
    private final Validator<T> elementValidator;

    ListProperty(Supplier<List<T>> defaultList, Type<T> type, Validator<T> elementValidator)
    {
        super(null, (config, path) -> config.getOrElse(path, defaultList.get()));
        this.defaultList = defaultList;
        this.type = type;
        this.elementValidator = elementValidator;
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
    public boolean isDefault()
    {
        return compareLists(this.get(), this.getDefaultValue(), this.getType());
    }

    @Override
    public void defineSpec(ConfigSpec spec)
    {
        Preconditions.checkState(this.data != null, "Config property is not initialized yet");
        spec.defineList(this.data.getPath(), this.defaultList::get, e -> {
            if(this.type == LONG && this.type.test(e)) // Special case for longs
                e = ((Number) e).longValue();
            return e != null && this.type.test(e) && (this.elementValidator == null || this.elementValidator.test((T) e));
        });
    }

    @Override
    public boolean isValid(List<T> value)
    {
        return value != null && value.stream().allMatch(e -> e != null && this.type.test(e) && (this.elementValidator == null || this.elementValidator.test(e)));
    }

    /**
     * Creates a ListProperty with the given type. The default value will simply be an empty list.
     * This list property will allow any value (as long as it matches the type) to be added into the
     * list. If only specific values should be allowed, use {@link #create(Type, Validator)} to provide
     * a custom element validator.
     *
     * @param type the type of the list
     * @return a new ListProperty instance
     */
    public static <T> ListProperty<T> create(Type<T> type)
    {
        return create(type, ArrayList::new);
    }

    /**
     * Creates a ListProperty with the given type and default list. This list property will allow any
     * value (as long as it matches the type) to be added into the list. If only specific values
     * should be allowed, while also providing a default list, used {@link #create(Type, Validator, Supplier)}
     * to provide a custom element validator.
     *
     * @param type        the type of the list
     * @param defaultList the default list of this property
     * @return a new ListProperty instance
     */
    public static <T> ListProperty<T> create(Type<T> type, Supplier<List<T>> defaultList)
    {
        return create(type, null, defaultList);
    }

    /**
     * Creates a ListProperty with the given type and element validator. The element validator
     * is used to validate a value before allowing it to be added into the list. The default value
     * of this property will simply be an empty list.
     *
     * @param type             the type of the list
     * @param elementValidator the element validator to determine which values can be in the list
     * @return a new ListProperty instance
     */
    public static <T> ListProperty<T> create(Type<T> type, Validator<T> elementValidator)
    {
        return create(type, elementValidator, ArrayList::new);
    }

    /**
     * Creates a ListProperty with the given type, element validator, and default list.
     *
     * @param type             the type of the list
     * @param elementValidator the element validator to determine which values can be in the list
     * @param defaultList      the default list of this property
     * @return a new ListProperty instance
     */
    public static <T> ListProperty<T> create(Type<T> type, Validator<T> elementValidator, Supplier<List<T>> defaultList)
    {
        return new ListProperty<>(defaultList, type, elementValidator);
    }

    public static class Type<T>
    {
        private final Class<T> classType;
        private final Class<?>[] additionalTypes;

        private Type(Class<T> classType, Class<?> ... additionalTypes)
        {
            this.classType = classType;
            this.additionalTypes = additionalTypes;
        }

        private boolean test(Object o)
        {
            Preconditions.checkNotNull(o);
            for(Class<?> validType : this.additionalTypes)
            {
                if(validType.isAssignableFrom(o.getClass()))
                {
                    return true;
                }
            }
            return this.classType.isAssignableFrom(o.getClass());
        }
    }

    /**
     * Compares two lists to test for equality, with a special case to handle when comparing
     * long lists. Nightconfig will parse longs as integers, however Long#equals will return
     * false even if the numbers are theoretically the same (e.g. 1L == 1). The method will
     * simply compare the size and the value at an index of a list against the same index of
     * another list; the List implementation does not affect the results.
     *
     * @param a    the first list
     * @param b    the second list
     * @param type the type matching the lists
     * @param <T>  the type of the list
     * @return true if the lists are equal
     */
    public static <T> boolean compareLists(List<T> a, List<T> b, Type<T> type)
    {
        if(a.size() != b.size())
            return false;

        for(int i = 0; i < a.size(); i++)
        {
            // Special handling of long lists since Long#equals fails against integers
            if(type == LONG)
            {
                long v1 = ((Number) a.get(i)).longValue();
                long v2 = ((Number) b.get(i)).longValue();
                if(!Objects.equals(v1, v2))
                {
                    return false;
                }
            }
            else if(!Objects.equals(a.get(i), b.get(i)))
            {
                return false;
            }
        }
        return true;
    }
}
