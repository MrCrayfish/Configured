package com.mrcrayfish.configured.impl.jei;

import com.mrcrayfish.configured.Reference;
import com.mrcrayfish.configured.client.ClientHandler;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.ModIds;
import mezz.jei.api.runtime.config.IJeiConfigManager;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * @author mezz
 */
@SuppressWarnings("unused")
@JeiPlugin
public class ConfiguredJeiPlugin implements IModPlugin
{
    @Nullable
    private static IJeiConfigManager jeiConfigManager;

    public static Optional<IJeiConfigManager> getJeiConfigManager()
    {
        return Optional.ofNullable(jeiConfigManager);
    }

    @Override
    public ResourceLocation getPluginUid()
    {
        return new ResourceLocation(Reference.MOD_ID, "jei_plugin");
    }

    @Override
    public void onConfigManagerAvailable(IJeiConfigManager configManager)
    {
        jeiConfigManager = configManager;
        ClientHandler.generateConfigFactory(ModIds.JEI_ID);
    }
}
