package com.mrcrayfish.configured.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;

/**
 * Author: MrCrayfish
 */
public interface IBackgroundTexture
{
    ResourceLocation getBackgroundTexture();

    static void loadTexture(Object object)
    {
        if(object instanceof IBackgroundTexture)
        {
            RenderSystem.setShaderTexture(0, ((IBackgroundTexture) object).getBackgroundTexture());
        }
    }
}
