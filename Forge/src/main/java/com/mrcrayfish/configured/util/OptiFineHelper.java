package com.mrcrayfish.configured.util;

/**
 * Author: MrCrayfish
 */
public class OptiFineHelper
{
    private static Boolean loaded = null;

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
