package com.mrcrayfish.configured.client;

public class ClientSessionData
{
    private static boolean developer;
    private static boolean lan;

    public static void setDeveloper(boolean enabled)
    {
        ClientSessionData.developer = enabled;
    }

    public static boolean isDeveloper()
    {
        return developer;
    }

    public static void setLan(boolean lan)
    {
        ClientSessionData.lan = lan;
    }

    public static boolean isLan()
    {
        return lan;
    }
}
