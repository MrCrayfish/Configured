package com.mrcrayfish.configured.platform;

import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.api.IConfigProviderT;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.api.IModConfigProvider;
import com.mrcrayfish.configured.api.ModContext;
import com.mrcrayfish.configured.platform.services.IConfigHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 */
public class FabricConfigHelper implements IConfigHelper
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
        this.readProviders(obj ->
        {
            if(obj instanceof IModConfigProvider provider)
            {
                providers.add(provider);
            }
        });
        return providers;
    }

    @Override
    public List<Function<ModContext, Supplier<Set<IModConfig>>>> getLegacyProviders()
    {
        List<Function<ModContext, Supplier<Set<IModConfig>>>> providers = new ArrayList<>();
        this.readProviders(obj ->
        {
            if(obj instanceof IConfigProviderT provider)
            {
                providers.add(context ->
                {
                    ModContainer c = FabricLoader.getInstance().getModContainer(context.modId()).orElse(null);
                    if(c != null)
                    {
                        return () -> provider.getConfigurationsForMod(c);
                    }
                    return Collections::emptySet;
                });
            }
        });
        return providers;
    }

    private void readProviders(Consumer<Object> function)
    {
        FabricLoader.getInstance().getAllMods().forEach(container ->
        {
            CustomValue value = container.getMetadata().getCustomValue("configured");
            if(value != null && value.getType() == CustomValue.CvType.OBJECT)
            {
                CustomValue.CvObject configuredObj = value.getAsObject();
                CustomValue providersValue = configuredObj.get("providers");
                if(providersValue != null)
                {
                    if(providersValue.getType() == CustomValue.CvType.ARRAY)
                    {
                        CustomValue.CvArray array = providersValue.getAsArray();
                        array.forEach(providerValue -> this.readProvider(providerValue, container, function));
                    }
                    else
                    {
                        this.readProvider(providersValue, container, function);
                    }
                }
            }
        });
    }

    private void readProvider(CustomValue providerValue, ModContainer container, Consumer<Object> function)
    {
        if(providerValue.getType() == CustomValue.CvType.STRING)
        {
            String providerClass = providerValue.getAsString();
            Object obj = this.createProviderInstance(container, providerClass);
            function.accept(obj);
            Constants.LOG.info("Successfully loaded config provider: {}", providerClass);
        }
        else
        {
            throw new RuntimeException("Config provider definition must be a String");
        }
    }

    private Object createProviderInstance(ModContainer container, String classPath)
    {
        try
        {
            Class<?> providerClass = Class.forName(classPath);
            Object obj = providerClass.getDeclaredConstructor().newInstance();
            if(!(obj instanceof IModConfigProvider) && !(obj instanceof IConfigProviderT))
            {
                throw new RuntimeException("Config providers must implement IModConfigProvider");
            }
            return obj;
        }
        catch(Exception e)
        {
            Constants.LOG.error("Failed to load config provider from mod: {}", container.getMetadata().getId());
            throw new RuntimeException("Failed to load config provider", e);
        }
    }

    @Override
    public ResourceLocation getBackgroundTexture(String modId)
    {
        ModContainer container = FabricLoader.getInstance().getModContainer(modId).orElse(null);
        if(container != null)
        {
            CustomValue value = container.getMetadata().getCustomValue("configured");
            if(value != null && value.getType() == CustomValue.CvType.OBJECT)
            {
                CustomValue.CvObject configuredObj = value.getAsObject();
                CustomValue backgroundValue = configuredObj.get("background");
                if(backgroundValue != null && backgroundValue.getType() == CustomValue.CvType.STRING)
                {
                    try
                    {
                        return new ResourceLocation(backgroundValue.getAsString());
                    }
                    catch(ResourceLocationException e)
                    {
                        return BACKGROUND_LOCATION;
                    }
                }
            }
        }
        return BACKGROUND_LOCATION;
    }
}
