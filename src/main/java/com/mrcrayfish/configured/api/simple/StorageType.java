package com.mrcrayfish.configured.api.simple;

/**
 * Author: MrCrayfish
 */
public enum StorageType
{
    /** Stores the config in the config folder */
    GLOBAL,

    /** Stores the config in the world/server folder. Config properties can only be accessed when a world is loaded. */
    WORLD,

    /** Stores the config in the memory. This will not load or save anything. */
    MEMORY
}
