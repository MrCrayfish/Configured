package com.mrcrayfish.configured.impl.jei;

import com.mrcrayfish.configured.api.simple.validate.NumberRange;
import mezz.jei.common.config.JEIClientConfigs;
import mezz.jei.common.config.file.ConfigCategory;
import mezz.jei.common.config.file.ConfigSchema;
import mezz.jei.common.config.file.ConfigValue;
import mezz.jei.common.config.file.serializers.IConfigValueSerializer;
import mezz.jei.common.config.file.serializers.IntegerSerializer;
import mezz.jei.common.config.file.serializers.ListSerializer;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;

/**
 * Author: MrCrayfish
 */
public final class JeiReflection
{
    public static NumberRange<Integer> getRange(IntegerSerializer serializer)
    {
        try
        {
            Field minField = ObfuscationReflectionHelper.findField(IntegerSerializer.class, "min");
            minField.setAccessible(true);
            int min = minField.getInt(serializer);

            Field maxField = ObfuscationReflectionHelper.findField(IntegerSerializer.class, "max");
            maxField.setAccessible(true);
            int max = maxField.getInt(serializer);

            return new NumberRange<>(min, max);
        }
        catch(IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, ConfigCategory> getConfigCategories(ConfigSchema schema)
    {
        try
        {
            Field field = ObfuscationReflectionHelper.findField(ConfigSchema.class, "categories");
            field.setAccessible(true);
            return (Map<String, ConfigCategory>) field.get(schema);
        }
        catch(IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    static Path getConfigPath(ConfigSchema schema)
    {
        try
        {
            Field field = ObfuscationReflectionHelper.findField(ConfigSchema.class, "path");
            field.setAccessible(true);
            return (Path) field.get(schema);
        }
        catch(IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    static <T> void setJeiValue(ConfigValue<?> configValue, T value)
    {
        try
        {
            Field field = ObfuscationReflectionHelper.findField(ConfigValue.class, "currentValue");
            field.setAccessible(true);
            field.set(configValue, value);
        }
        catch(IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void readSchema(JEIClientConfigs configs)
    {
        try
        {
            Field field = ObfuscationReflectionHelper.findField(JEIClientConfigs.class, "schema");
            field.setAccessible(true);
            JeiInstanceHolder.setClientSchema((ConfigSchema) field.get(configs));
        }
        catch(IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> IConfigValueSerializer<T> getListValueSerializer(ListSerializer<T> serializer)
    {
        try
        {
            Field field = ObfuscationReflectionHelper.findField(ListSerializer.class, "valueSerializer");
            field.setAccessible(true);
            return (IConfigValueSerializer<T>) field.get(serializer);
        }
        catch(IllegalAccessException e)
        {
            throw new RuntimeException(e);
        }
    }
}
