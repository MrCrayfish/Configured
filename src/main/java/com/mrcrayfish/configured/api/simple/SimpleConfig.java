package com.mrcrayfish.configured.api.simple;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Author: MrCrayfish
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SimpleConfig
{
    String id();

    String name();

    boolean sync() default false;

    boolean editable() default true;

    StorageType storage() default StorageType.GLOBAL;
}
