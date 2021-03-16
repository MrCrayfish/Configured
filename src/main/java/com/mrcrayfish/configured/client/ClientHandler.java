package com.mrcrayfish.configured.client;

import com.mrcrayfish.configured.mixin.ModContainerMixin;
import net.minecraftforge.fml.ModList;

import java.util.Map;

/**
 * Author: MrCrayfish
 */
public class ClientHandler
{
    public static void setup()
    {
        ModList.get().forEachModContainer((modId, container) -> {
            ((ModContainerMixin) container).getConfigs().forEach((type, config) -> {
                Map<String, Object> valueMap = config.getSpec().valueMap();
                System.out.println(valueMap);
            });
        });
    }
}
