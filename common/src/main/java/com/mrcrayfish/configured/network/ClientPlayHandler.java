package com.mrcrayfish.configured.network;

import com.mrcrayfish.configured.client.SessionData;
import com.mrcrayfish.configured.network.message.MessageSessionData;

/**
 * Author: MrCrayfish
 */
public class ClientPlayHandler
{
    public static void handleSessionData(MessageSessionData message)
    {
        SessionData.setDeveloper(message.developer());
        SessionData.setLan(message.lan());
    }
}
