package com.mrcrayfish.configured.client.screen;

import net.minecraft.resources.ResourceLocation;

/**
 * Author: MrCrayfish
 */
public interface IBackgroundTexture
{
    ResourceLocation getBackgroundTexture();

    static ResourceLocation loadTexture(Object object, ResourceLocation original)
    {
        if(object instanceof IBackgroundTexture)
        {
            return ((IBackgroundTexture) object).getBackgroundTexture();
        }
        return original;
    }
}
