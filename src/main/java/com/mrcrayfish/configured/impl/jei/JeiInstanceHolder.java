package com.mrcrayfish.configured.impl.jei;

import mezz.jei.common.config.file.ConfigSchema;

import javax.annotation.Nullable;

/**
 * Author: MrCrayfish
 */
public final class JeiInstanceHolder
{
    private static ConfigSchema clientSchema;

    @Nullable
    public static ConfigSchema getClientSchema()
    {
        return clientSchema;
    }

    public static void setClientSchema(ConfigSchema clientSchema)
    {
        JeiInstanceHolder.clientSchema = clientSchema;
    }
}
