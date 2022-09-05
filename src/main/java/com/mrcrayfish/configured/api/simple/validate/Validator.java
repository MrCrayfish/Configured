package com.mrcrayfish.configured.api.simple.validate;

import net.minecraft.network.chat.Component;

/**
 * Author: MrCrayfish
 */
public interface Validator<T>
{
    /**
     * Tests if the given value is valid as specified by the implementation
     *
     * @param value the object to test
     * @return True if valid
     */
    boolean test(T value);

    /**
     * @return A hint to the user that explains a valid value
     */
    Component getHint();
}
