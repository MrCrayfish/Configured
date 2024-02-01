package com.mrcrayfish.configured.mixin;

import com.mrcrayfish.configured.Events;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Author: MrCrayfish
 */
@Mixin(PlayerList.class)
public class PlayerListMixin
{
    @Inject(method = "placeNewPlayer", at = @At(value = "TAIL"))
    private void frameworkOnPlayerJoin(Connection connection, ServerPlayer player, CallbackInfo ci)
    {
        Events.onPlayerLoggedIn(player);
    }
}
