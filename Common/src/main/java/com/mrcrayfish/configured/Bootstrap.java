package com.mrcrayfish.configured;

import com.mrcrayfish.configured.network.Network;

/**
 * Author: MrCrayfish
 */
public class Bootstrap
{
    public static void init()
    {
        Network.init();
        Events.init();
    }
}
