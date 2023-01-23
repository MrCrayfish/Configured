package com.mrcrayfish.configured.impl.jei;

import com.mrcrayfish.configured.Reference;
import com.mrcrayfish.configured.client.ClientHandler;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.ModIds;
import mezz.jei.api.runtime.IJeiRuntime;
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
    private static @Nullable IJeiConfigManager jeiConfigManager;

    public static Optional<IJeiConfigManager> getJeiConfigManager()
    {
        return Optional.ofNullable(jeiConfigManager);
    }

    @Override
    @Nonnull
    public ResourceLocation getPluginUid()
    {
        return new ResourceLocation(Reference.MOD_ID, "jei_plugin");
    }

    @Override
    public void onRuntimeAvailable(@Nonnull IJeiRuntime jeiRuntime)
    {
        jeiConfigManager = jeiRuntime.getConfigManager();
        ClientHandler.generateConfigFactory(ModIds.JEI_ID);
    }

    @Override
    public void onRuntimeUnavailable()
    {
        jeiConfigManager = null;
    }
}
