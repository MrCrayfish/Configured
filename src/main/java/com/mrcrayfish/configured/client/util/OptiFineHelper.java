package com.mrcrayfish.configured.client.util;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.lang.reflect.Field;

/**
 * Author: MrCrayfish
 */
@OnlyIn(Dist.CLIENT)
public class OptiFineHelper
{
    private static Boolean loaded = null;
    private static Field programIdField;

    public static boolean isLoaded()
    {
        if(loaded == null)
        {
            try
            {
                Class.forName("optifine.Installer");
                loaded = true;
            }
            catch(ClassNotFoundException e)
            {
                loaded = false;
            }
        }
        return loaded;
    }
}
