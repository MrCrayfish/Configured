package com.mrcrayfish.configured.util.fabric;

import com.mrcrayfish.configured.util.CustomValueUtil;
import dev.architectury.platform.Mod;
import net.fabricmc.loader.api.FabricLoader;

public class CustomValueUtilImpl {
    public static CustomValueUtil.CustomValue getCustomValue(Mod mod, String string) {
        var q = FabricLoader.getInstance().getModContainer(mod.getModId()).get().getMetadata().getCustomValue(string);
        return new CustomValueUtil.CustomValue(q);
    }
}
