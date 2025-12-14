package com.mrcrayfish.configured.impl.jei;

import com.mrcrayfish.configured.Constants;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.config.IJeiConfigManager;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * @author mezz
 */
@SuppressWarnings("unused")
//@JeiPlugin // TODO reimplement
public class ConfiguredJeiPlugin// implements IModPlugin
{
    /*@Nullable
    private static IJeiConfigManager jeiConfigManager;

    public static Optional<IJeiConfigManager> getJeiConfigManager()
    {
        return Optional.ofNullable(jeiConfigManager);
    }

    @Override
    public Identifier getPluginUid()
    {
        return Identifier.fromNamespaceAndPath(Constants.MOD_ID, "jei_plugin");
    }

    @Override
    public void onConfigManagerAvailable(IJeiConfigManager configManager)
    {
        jeiConfigManager = configManager;
    }*/
}
