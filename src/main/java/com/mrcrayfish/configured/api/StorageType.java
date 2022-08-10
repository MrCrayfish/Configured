package com.mrcrayfish.configured.api;

/**
 * Author: MrCrayfish
 */
public enum StorageType
{
    /**
     * Stores the config in the standard config directory, located from the working directory of the game
     */
    GLOBAL,

    /**
     * Stores the config in the world/server directory. Config properties can only be accessed when a world is loaded.
     */
    WORLD,

    /**
     * Stores the config in the memory. This will not load or save anything.
     */
    MEMORY
}
