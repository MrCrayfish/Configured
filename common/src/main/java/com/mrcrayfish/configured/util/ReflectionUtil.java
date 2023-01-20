package com.mrcrayfish.configured.util;

import java.lang.reflect.Field;

/**
 * Author: Jab125
 */
public class ReflectionUtil {
    public static Class<?> classForName(String name) {
        return unsafeOperation(() -> Class.forName(name));
    }

    public static Object fieldGet(Field field, Object value) {
        return unsafeOperation(() -> field.get(value));
    }

    public static <T> T unsafeOperation(UnsafeAction<T> action) {
        try {
            return action.run();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static interface UnsafeAction<T> {
        T run() throws Throwable;
    }
}
