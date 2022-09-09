package com.mrcrayfish.configured.network;

/**
 * Author: MrCrayfish
 */
class HandshakeHandler
{
    /*static void handleAcknowledge(HandshakeMessages.C2SAcknowledge message, Supplier<NetworkEvent.Context> supplier)
    {
        Configured.LOGGER.debug("Received acknowledgement from client");
        supplier.get().setPacketHandled(true);
    }

    static void handleConfigData(HandshakeMessages.S2CConfigData message, Supplier<NetworkEvent.Context> supplier)
    {
        Configured.LOGGER.debug("Received config data from server");
        CountDownLatch block = new CountDownLatch(1);
        supplier.get().enqueueWork(() -> {
            if(!SimpleConfigManager.getInstance().processConfigData(message)) {
                supplier.get().getNetworkManager().disconnect(Component.literal("Received invalid config data from server"));
            }
            block.countDown();
        });
        try
        {
            block.await();
        }
        catch(InterruptedException e)
        {
            e.printStackTrace();
        }
        supplier.get().setPacketHandled(true);
        PacketHandler.getHandshakeChannel().reply(new HandshakeMessages.C2SAcknowledge(), supplier.get());
    }*/
}
