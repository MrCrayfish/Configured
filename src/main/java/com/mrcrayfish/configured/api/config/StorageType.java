package com.mrcrayfish.configured.api.config;

/**
 * Author: MrCrayfish
 */
public enum StorageType
{
    /** Stores the config in the config folder */
    GLOBAL,

    /** Stores the config in the world folder */
    WORLD,

    /** Stores the config in the memory. This will not save! */
    MEMORY
}
