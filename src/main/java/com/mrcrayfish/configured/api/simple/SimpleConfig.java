package com.mrcrayfish.configured.api.simple;

import com.mrcrayfish.configured.api.ConfigType;

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

    boolean editable() default true;

    ConfigType type() default ConfigType.UNIVERSAL;
}
