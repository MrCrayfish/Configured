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
    /**
     * @return The id of the mod creating this config
     */
    String id();

    /**
     * Gets the name of this config. The name can only contain the lowercase letters from
     * a to z and underscores. The pattern for the name is ^[a-z_]+$ and an exception
     * will be thrown if name does not match.
     *
     * @return The name of this config.
     */
    String name();

    /**
     * A read only config is loaded once and cannot be edited while the game is running. This
     * means that the config can only be edited using the raw file and changes will only take effect
     * when the game is restarted.
     *
     * @return True if this config is read only
     */
    boolean readOnly() default false;

    /**
     * The type of this config. The config type determines how the config is handled, this affects
     * when it's loaded, when and where it's accessible, the directory it's generated in, and more.
     * See {@link ConfigType} for descriptions of each type.
     *
     * The following types are the equivalent of Forge config types:
     * - ConfigType.CLIENT = ModConfig.Type.CLIENT
     * - ConfigType.UNIVERSAL = ModConfig.Type.COMMON
     * - ConfigType.WORLD_SYNC = ModConfig.Type.SERVER
     *
     * @return the config type of this config
     */
    ConfigType type() default ConfigType.UNIVERSAL;

    //TODO add hidden
}
