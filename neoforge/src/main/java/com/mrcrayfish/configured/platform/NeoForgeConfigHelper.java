package com.mrcrayfish.configured.platform;

import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.api.IModConfigProvider;
import com.mrcrayfish.configured.platform.services.IConfigHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.moddiscovery.ModInfo;

import org.jetbrains.annotations.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Author: MrCrayfish
 */
public class NeoForgeConfigHelper implements IConfigHelper
{
    private static final LevelResource SERVER_CONFIG_RESOURCE = new LevelResource("serverconfig");

    @Override
    public LevelResource getServerConfigResource()
    {
        return SERVER_CONFIG_RESOURCE;
    }

    @Override
    public Set<IModConfigProvider> getProviders()
    {
        Set<IModConfigProvider> providers = new HashSet<>();
        ModList.get().forEachModContainer((id, container) ->
        {
            Object raw = container.getModInfo().getModProperties().get("configuredProviders");
            if(raw instanceof String)
            {
                Object provider = createProviderInstance(container, raw.toString());
                if(provider instanceof IModConfigProvider)
                {
                    providers.add((IModConfigProvider) provider);
                    Constants.LOG.info("Successfully loaded config provider: {}", raw.toString());
                }
            }
            else if(raw instanceof List<?> providerList)
            {
                for(Object obj : providerList)
                {
                    Object provider = createProviderInstance(container, obj.toString());
                    if(provider instanceof IModConfigProvider)
                    {
                        providers.add((IModConfigProvider) provider);
                        Constants.LOG.info("Successfully loaded config provider: {}", obj.toString());
                    }
                }
            }
            else if(raw != null)
            {
                throw new RuntimeException("Config provider definition must be either a String or Array of Strings");
            }
        });
        return providers;
    }

    @Nullable
    private static Object createProviderInstance(ModContainer container, String classPath)
    {
        try
        {
            Class<?> providerClass = Class.forName(classPath);
            Object obj = providerClass.getDeclaredConstructor().newInstance();
            if(!(obj instanceof IModConfigProvider))
            {
                throw new RuntimeException("Config providers must implement IModConfigProvider");
            }
            return obj;
        }
        catch(Exception e)
        {
            Constants.LOG.error("Failed to load config provider from mod: {}", container.getModId());
            return null;
        }
    }
}
