package com.mrcrayfish.configured.util.forge;

import com.mrcrayfish.configured.util.CustomValueUtil;
import dev.architectury.platform.Mod;
import net.minecraftforge.fml.ModList;

public class CustomValueUtilImpl {
    public static CustomValueUtil.CustomValue getCustomValue(Mod mod, String string) {
        return new CustomValueUtil.CustomValue(ModList.get().getModContainerById(mod.getModId()).get().getModInfo().getModProperties().get(string));
    }
}
