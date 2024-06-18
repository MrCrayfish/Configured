package com.mrcrayfish.configured;

import com.mrcrayfish.configured.api.Environment;
import com.mrcrayfish.configured.platform.Services;

/**
 * Author: MrCrayfish
 */
public class Bootstrap
{
    public static void init()
    {
        Config.load(Services.PLATFORM.getConfigPath());
    }
}
