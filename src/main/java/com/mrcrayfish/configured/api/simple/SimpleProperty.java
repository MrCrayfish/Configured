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
public @interface SimpleProperty
{
    /**
     * @return The name or key for this property. It should only contain [a-z_]+
     */
    String name();

    /**
     * A comment to associate with this property. This will be shown in the raw config file, and
     * it'll be used as a fallback if no translated comment was found. It is recommended to add
     * translations, so it can be supported in multiple languages.
     *
     * The translation key structure is:
     * simpleconfig.[mod_id].[config_name].[property_name]
     *
     * If the property is in a sub-level, the structure is simply:
     * simpleconfig.[mod_id].[config_name].[sub_level_name].[property_name]
     *
     * You can also add a translated tooltip by cloning and adding ".tooltip" to the end of the key.
     *
     * @return the comment for this property
     */
    String comment() default "";

    /**
     * After modifying this property, it may require the player to reload their world to take effect.
     * In this case, world restart should be set to true. This will prompt the player to reload
     * their world for the changes to apply.
     *
     * @return True if this property requires the player to reload their world to take effect
     */
    boolean worldRestart() default false;
}
