package com.mrcrayfish.configured.network.play;

import com.mrcrayfish.configured.client.SessionData;
import com.mrcrayfish.configured.network.message.play.S2CMessageSessionData;

/**
 * Author: MrCrayfish
 */
public class ClientPlayHandler
{
    public static void handleJoinMessage(S2CMessageSessionData data)
    {
        SessionData.setDeveloper(data.isDeveloper());
        SessionData.setLan(data.isLan());
    }
}
