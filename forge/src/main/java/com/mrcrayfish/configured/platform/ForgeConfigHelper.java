package com.mrcrayfish.configured.platform;

import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.api.IConfigProvider;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.api.IModConfigProvider;
import com.mrcrayfish.configured.api.ModContext;
import com.mrcrayfish.configured.platform.services.IConfigHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 */
public class ForgeConfigHelper implements IConfigHelper
{
    private static final ResourceLocation BACKGROUND_LOCATION = new ResourceLocation("textures/gui/options_background.png");
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

    @Override
    public List<Function<ModContext, Supplier<Set<IModConfig>>>> getLegacyProviders()
    {
        List<Function<ModContext, Supplier<Set<IModConfig>>>> providers = new ArrayList<>();
        ModList.get().forEachModContainer((id, container) ->
        {
            Object raw = container.getModInfo().getModProperties().get("configuredProviders");
            if(raw instanceof String)
            {
                Object provider = createProviderInstance(container, raw.toString());
                if(provider instanceof IConfigProvider legacyProvider)
                {
                    providers.add(context ->
                    {
                        ModContainer c = ModList.get().getModContainerById(context.modId()).orElse(null);
                        if(c != null)
                        {
                            return () -> legacyProvider.getConfigurationsForMod(c);
                        }
                        return Collections::emptySet;
                    });
                    Constants.LOG.info("Successfully loaded config provider: {}", raw.toString());
                }
            }
            else if(raw instanceof List<?> providerList)
            {
                for(Object obj : providerList)
                {
                    Object provider = createProviderInstance(container, obj.toString());
                    if(provider instanceof IConfigProvider legacyProvider)
                    {
                        providers.add(context ->
                        {
                            ModContainer c = ModList.get().getModContainerById(context.modId()).orElse(null);
                            if(c != null)
                            {
                                return () -> legacyProvider.getConfigurationsForMod(c);
                            }
                            return Collections::emptySet;
                        });
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
            if(!(obj instanceof IModConfigProvider) && !(obj instanceof IConfigProvider))
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

    @Override
    public ResourceLocation getBackgroundTexture(String modId)
    {
        ModContainer container = ModList.get().getModContainerById(modId).orElse(null);
        if(container != null)
        {
            String configBackground = (String) container.getModInfo().getModProperties().get("configuredBackground");
            if(configBackground != null)
            {
                return new ResourceLocation(configBackground);
            }
            if(container.getModInfo() instanceof ModInfo modInfo)
            {
                // Fallback to old method to getting config background (since mods might not have updated)
                Optional<String> optional = modInfo.getConfigElement("configBackground");
                if(optional.isPresent())
                {
                    return new ResourceLocation(optional.get());
                }
            }
        }
        return BACKGROUND_LOCATION;
    }
}
