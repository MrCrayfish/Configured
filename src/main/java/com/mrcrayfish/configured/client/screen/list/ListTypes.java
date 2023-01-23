package com.mrcrayfish.configured.client.screen.list;

import com.google.common.collect.Streams;
import com.mrcrayfish.configured.api.IConfigValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

public class ListTypes
{
    private static final Map<IConfigValue<?>, IListType<?>> TYPE_CACHE = new HashMap<>();
    private static final IListType<?> UNKNOWN = new ListType<>(Object::toString, o -> o, "configured.parser.not_a_value");

    public static final IListType<Boolean> BOOLEAN = new ListType<>(Object::toString, Boolean::valueOf, "configured.parser.not_a_boolean");
    public static final IListType<Integer> INTEGER = new ListType<>(Object::toString, Integer::parseInt, "configured.parser.not_a_number");
    public static final IListType<Long> LONG = new ListType<>(Object::toString, Long::parseLong, "configured.parser.not_a_number");
    public static final IListType<Double> DOUBLE = new ListType<>(Object::toString, Double::parseDouble, "configured.parser.not_a_number");
    public static final IListType<String> STRING = new ListType<>(Function.identity(), Function.identity(), "configured.parser.not_a_value");

    @SuppressWarnings("unchecked")
    public static <T> IListType<T> getUnknown()
    {
        return (IListType<T>) UNKNOWN;
    }

    @SuppressWarnings("unchecked")
    public static <T> IListType<T> getType(IConfigValue<List<T>> holder)
    {
        if(holder instanceof IListConfigValue<T> provider)
        {
            IListType<T> type = provider.getListType();
            if(type != null)
            {
                return type;
            }
        }
        return (IListType<T>) TYPE_CACHE.computeIfAbsent(holder, value -> fromHolder(holder));
    }

    private static <T> IListType<T> fromHolder(IConfigValue<List<T>> holder)
    {
        return getListValues(holder)
                .map(ListTypes::fromObject)
                .filter(t -> t != UNKNOWN)
                .findAny()
                .orElseGet(() -> fromElementValidator(holder));
    }

    private static <T> Stream<T> getListValues(IConfigValue<List<T>> holder)
    {
        return Streams.concat(
                        holder.get().stream(),
                        holder.getDefault().stream()
                )
                .filter(Objects::nonNull);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> IListType<T> fromObject(T o)
    {
        if(o instanceof Boolean)
        {
            return (IListType<T>) BOOLEAN;
        }
        else if(o instanceof Integer)
        {
            return (IListType<T>) INTEGER;
        }
        else if(o instanceof Long)
        {
            return (IListType<T>) LONG;
        }
        else if(o instanceof Double)
        {
            return (IListType<T>) DOUBLE;
        }
        else if(o instanceof String)
        {
            return (IListType<T>) STRING;
        }
        else if(o instanceof Enum<?> enumValue)
        {
            return new EnumListType<>(enumValue.getClass());
        }
        return (IListType<T>) UNKNOWN;
    }

    /**
     * Attempts to determine the type of list from the element validator. This currently
     * used as a last resort since validation may fail even though it's the correct type.
     * It may also return the incorrect type if the validator accepts everything.
     */
    @SuppressWarnings("unchecked")
    private static <T> IListType<T> fromElementValidator(IConfigValue<List<T>> spec)
    {
        if(safeTryIsValid(spec, Collections.singletonList("s")))
        {
            return (IListType<T>) STRING;
        }
        if(safeTryIsValid(spec, Collections.singletonList(true)))
        {
            return (IListType<T>) BOOLEAN;
        }
        if(safeTryIsValid(spec, Collections.singletonList(0.0D)))
        {
            return (IListType<T>) DOUBLE;
        }
        if(safeTryIsValid(spec, Collections.singletonList(0L)))
        {
            return (IListType<T>) LONG;
        }
        if(safeTryIsValid(spec, Collections.singletonList(0)))
        {
            return (IListType<T>) INTEGER;
        }
        return (IListType<T>) UNKNOWN;
    }

    @SuppressWarnings("unchecked")
    private static <T> boolean safeTryIsValid(IConfigValue<List<T>> spec, List<?> list)
    {
        try
        {
            return spec.isValid((List<T>) list);
        } catch (ClassCastException ignored)
        {
            return false;
        }
    }
}
