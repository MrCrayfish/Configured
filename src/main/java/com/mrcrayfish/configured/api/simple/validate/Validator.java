package com.mrcrayfish.configured.api.simple.validate;

import net.minecraft.network.chat.Component;

/**
 * Author: MrCrayfish
 */
public interface Validator<T>
{
    boolean test(T value);

    Component getHint();
}
