package com.mrcrayfish.configured.platform;

import com.mrcrayfish.configured.platform.services.IPlatformHelper;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class FabricPlatformHelper implements IPlatformHelper
{
    @Override
    public String getPlatformName()
    {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId)
    {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment()
    {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public Path getGamePath()
    {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public Path getConfigPath()
    {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public String getDefaultConfigPath()
    {
        return "defaultconfigs";
    }
}
