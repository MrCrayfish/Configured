package com.mrcrayfish.configured.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.api.StorageType;
import com.mrcrayfish.configured.api.simple.ConfigProperty;
import com.mrcrayfish.configured.api.simple.SimpleConfig;
import com.mrcrayfish.configured.api.simple.SimpleProperty;
import com.mrcrayfish.configured.client.screen.IEditing;
import com.mrcrayfish.configured.client.screen.ListMenuScreen;
import com.mrcrayfish.configured.impl.simple.SimpleFolderEntry;
import com.mrcrayfish.configured.impl.simple.SimpleValue;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.client.event.ScreenOpenEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.FileUtils;
import net.minecraftforge.fml.loading.moddiscovery.ModAnnotation;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Author: MrCrayfish
 */
public class ConfigManager
{
    private static final Predicate<String> NAME_PATTERN = Pattern.compile("^[a-z_]+$").asMatchPredicate();
    private static final LevelResource SERVER_CONFIG = new LevelResource("serverconfig");
    private static final org.objectweb.asm.Type SIMPLE_CONFIG = org.objectweb.asm.Type.getType(SimpleConfig.class);

    private static ConfigManager instance;

    public static ConfigManager getInstance()
    {
        if(instance == null)
        {
            instance = new ConfigManager();
        }
        return instance;
    }

    private final List<SimpleConfigEntry> configs;
    private IModConfig editingConfig;

    private ConfigManager()
    {
        this.configs = this.getAllSimpleConfigs();
    }

    public List<SimpleConfigEntry> getConfigs()
    {
        return ImmutableList.copyOf(this.configs);
    }

    private List<SimpleConfigEntry> getAllSimpleConfigs()
    {
        List<ModFileScanData.AnnotationData> annotations = ModList.get().getAllScanData().stream().map(ModFileScanData::getAnnotations).flatMap(Collection::stream).filter(a -> SIMPLE_CONFIG.equals(a.annotationType())).toList();
        List<SimpleConfigEntry> configs = new ArrayList<>();
        annotations.forEach(data -> {
            try
            {
                Class<?> configClass = Class.forName(data.clazz().getClassName());
                Field field = configClass.getDeclaredField(data.memberName());
                field.setAccessible(true);
                Object object = field.get(null);
                if(!object.getClass().isPrimitive())
                {
                    configs.add(new SimpleConfigEntry(data.annotationData(), object));
                }
            }
            catch(NoSuchFieldException | ClassNotFoundException | IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
        });
        return ImmutableList.copyOf(configs);
    }

    private static ConfigSpec createSpec(Set<ConfigProperty<?>> properties)
    {
        ConfigSpec spec = new ConfigSpec();
        properties.forEach(p -> p.defineSpec(spec));
        return spec;
    }

    private static Set<ConfigProperty<?>> gatherConfigProperties(Object object)
    {
        Set<ConfigProperty<?>> properties = new HashSet<>();
        readFields(properties, new Stack<>(), object);
        return ImmutableSet.copyOf(properties);
    }

    private static void readFields(Set<ConfigProperty<?>> properties, Stack<String> path, Object instance)
    {
        Field[] fields = instance.getClass().getDeclaredFields();
        Stream.of(fields).forEach(field -> Optional.ofNullable(field.getDeclaredAnnotation(SimpleProperty.class)).ifPresent(sp -> {
            path.push(sp.value());
            try
            {
                field.setAccessible(true);
                Object obj = field.get(instance);
                if(obj instanceof ConfigProperty<?> property)
                {
                    property.initPath(new ValuePath(StringUtils.join(path, ".")));
                    properties.add(property);
                }
                else
                {
                    readFields(properties, path, obj);
                }
            }
            catch(IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
            path.pop();
        }));
    }

    @SubscribeEvent
    public void onServerStarting(ServerAboutToStartEvent event)
    {
        Configured.LOGGER.info("Loading world configs...");
        Path serverConfig = event.getServer().getWorldPath(SERVER_CONFIG);
        FileUtils.getOrCreateDirectory(serverConfig, "serverconfig");
        this.configs.stream().filter(entry -> entry.storageType == StorageType.WORLD).forEach(entry -> {
            entry.load(serverConfig);
        });
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event)
    {
        Configured.LOGGER.info("Unloading world configs...");
        this.configs.stream().filter(entry -> entry.storageType == StorageType.WORLD).forEach(SimpleConfigEntry::unload);
    }

    @SubscribeEvent
    public void onScreenOpen(ScreenOpenEvent event)
    {
        // Keeps track of the config currently being editing and runs events accordingly
        if(event.getScreen() instanceof IEditing editing)
        {
            if(this.editingConfig == null)
            {
                this.editingConfig = editing.getActiveConfig();
                this.editingConfig.startEditing();
                Configured.LOGGER.info("Started editing '" + this.editingConfig.getFileName() + "'");
            }
            else if(this.editingConfig != editing.getActiveConfig())
            {
                throw new IllegalStateException("Trying to edit a config while one is already loaded. This should not happen!");
            }
        }
        else if(this.editingConfig != null)
        {
            Configured.LOGGER.info("Stopped editing '" + this.editingConfig.getFileName() + "'");
            this.editingConfig.stopEditing();
            this.editingConfig = null;
        }
    }

    public static final class SimpleConfigEntry implements IModConfig
    {
        private final String id;
        private final String name;
        private final boolean sync;
        private final StorageType storageType;
        private final Object instance;
        private final Set<ConfigProperty<?>> allProperties;
        private final PropertyMap propertyMap;
        private final ConfigSpec spec;
        private final ClassLoader classLoader;
        @Nullable
        private Config config;

        private SimpleConfigEntry(Map<String, Object> data, Object instance)
        {
            Preconditions.checkArgument(data.get("id") instanceof String, "The 'id' of the config is not a String");
            Preconditions.checkArgument(!((String) data.get("id")).trim().isEmpty(), "The 'id' of the config cannot be empty");
            Preconditions.checkArgument(ModList.get().isLoaded(((String) data.get("id"))), "The 'id' of the config must match a mod id");
            Preconditions.checkArgument(data.get("name") instanceof String, "The 'name' of the config is not a String");
            Preconditions.checkArgument(!((String) data.get("name")).trim().isEmpty(), "The 'name' of the config cannot be empty");
            Preconditions.checkArgument(((String) data.get("name")).length() <= 64, "The 'name' of the config must be 64 characters or less");
            Preconditions.checkArgument(NAME_PATTERN.test((String) data.get("name")), "The 'name' of the config is invalid. It can only contain 'a-z' and '_'");

            this.id = (String) data.get("id");
            this.name = (String) data.get("name");
            this.sync = (Boolean) data.getOrDefault("sync", false);
            this.storageType = Optional.ofNullable((ModAnnotation.EnumHolder) data.get("storage")).map(holder -> StorageType.valueOf(holder.getValue())).orElse(StorageType.GLOBAL);
            this.instance = instance;
            this.allProperties = gatherConfigProperties(instance);
            this.propertyMap = new PropertyMap(this.allProperties);
            this.spec = createSpec(this.allProperties);
            this.classLoader = Thread.currentThread().getContextClassLoader();

            if(this.storageType == StorageType.GLOBAL) // Load global configs immediately
            {
                this.load(FMLPaths.CONFIGDIR.get());
            }
            else if(this.storageType == StorageType.MEMORY)
            {
                this.load(null);
            }
        }

        /**
         * Loads the config from the given path. If the path is null then a memory config will be
         * loaded instead.
         *
         * @param configDir the path of the configuration directory
         */
        private void load(@Nullable Path configDir)
        {
            Preconditions.checkState(this.config == null, "Config is already loaded. Unload before loading again.");
            Config config = ConfigUtil.createSimpleConfig(configDir, this.id, this.name, CommentedConfig::inMemory);
            ConfigUtil.loadFileConfig(config);
            this.correct(config);
            this.allProperties.forEach(p -> p.updateProxy(new ValueProxy(config, p.getPath())));
            this.config = config;
            ConfigUtil.watchFileConfig(config, this::changeCallback);
        }

        private void unload()
        {
            if(this.config != null)
            {
                this.allProperties.forEach(p -> p.updateProxy(ValueProxy.EMPTY));
                ConfigUtil.closeFileConfig(this.config);
                this.config = null;
            }
        }

        private void changeCallback()
        {
            Thread.currentThread().setContextClassLoader(this.classLoader);
            if(this.config != null)
            {
                ConfigUtil.loadFileConfig(this.config);
                this.correct(this.config);
                this.allProperties.forEach(ConfigProperty::invalidateCache);
            }
        }

        private void correct(Config config)
        {
            if(!this.spec.isCorrect(config))
            {
                this.spec.correct(config);
                ConfigUtil.saveFileConfig(config);
            }
        }

        @Override
        public void update(IConfigEntry entry)
        {
            Preconditions.checkState(this.config != null, "Tried to update a config that is not loaded");

            // Find changed values and update config if necessary
            Set<IConfigValue<?>> changedValues = entry.getChangedValues();
            if(!changedValues.isEmpty())
            {
                CommentedConfig newConfig = CommentedConfig.copy(this.config);
                changedValues.forEach(value ->
                {
                    if(value instanceof SimpleValue<?> simpleValue)
                    {
                        newConfig.set(simpleValue.getPath(), simpleValue.get());
                    }
                });
                this.correct(newConfig);
                this.config.putAll(newConfig);
                this.allProperties.forEach(ConfigProperty::invalidateCache);
            }

            // Post handling
            if(this.getStorage() == StorageType.WORLD)
            {
                if(!ListMenuScreen.isPlayingGame())
                {
                    // Unload world configs since still in main menu
                    this.unloadServerConfig();
                }
                else if(this.sync)
                {
                    //TODO send to server
                    //ConfigHelper.sendModConfigDataToServer(this.config);
                }
            }
            else
            {
                //TODO events for simple configs
                /*Configured.LOGGER.info("Sending config reloading event for {}", this.config.getFileName());
                this.config.getSpec().afterReload();
                ConfigHelper.fireEvent(this.config, new ModConfigEvent.Reloading(this.config));*/
            }
        }

        @Override
        public IConfigEntry getRoot()
        {
            return new SimpleFolderEntry("Root", this.propertyMap, true);
        }

        @Override
        public ConfigType getConfigType()
        {
            return this.storageType == StorageType.WORLD ? ConfigType.SERVER : ConfigType.COMMON;
        }

        @Override
        public StorageType getStorage()
        {
            return this.storageType;
        }

        @Override
        public String getFileName()
        {
            return String.format("%s.%s.toml", this.id, this.name);
        }

        @Override
        public String getTranslationKey()
        {
            return String.format("simpleconfig.%s.%s", this.id, this.name);
        }

        @Override
        public String getModId()
        {
            return this.id;
        }

        //TODO unload
        @Override
        public void loadServerConfig(Path configDir, Consumer<IModConfig> result) throws IOException
        {
            // Same as normal loading just without file watching
            Config config = ConfigUtil.createTempServerConfig(configDir, this.id, this.name);
            ConfigUtil.loadFileConfig(config);
            this.correct(config);
            this.allProperties.forEach(p -> p.updateProxy(new ValueProxy(config, p.getPath())));
            this.config = config;
            result.accept(this);
        }

        @Override
        public void stopEditing()
        {
            // Attempts to unload the server config if player simply just went back
            if(this.config != null && this.getStorage() == StorageType.WORLD)
            {
                if(!ListMenuScreen.isPlayingGame())
                {
                    this.unloadServerConfig();
                }
            }
        }

        private void unloadServerConfig()
        {
            if(this.config != null)
            {
                this.allProperties.forEach(p -> p.updateProxy(ValueProxy.EMPTY));
                if(this.config instanceof FileConfig fileConfig) fileConfig.close();
                this.config = null;
            }
        }
    }

    public static class PropertyMap implements IMapEntry
    {
        private final Map<String, IMapEntry> map = new HashMap<>();

        private final String path;

        private PropertyMap(String path)
        {
            this.path = path;
        }

        private PropertyMap(Set<ConfigProperty<?>> properties)
        {
            this.path = null;
            properties.forEach(p ->
            {
                PropertyMap current = this;
                List<String> path = com.electronwill.nightconfig.core.utils.StringUtils.split(p.getPath(), '.');
                for(int i = 0; i < path.size() - 1; i++)
                {
                    int finalI = i;
                    current = (PropertyMap) current.map.computeIfAbsent(path.get(i), s -> {
                        String subPath = StringUtils.join(path.subList(0, finalI), '.');
                        return new PropertyMap(subPath);
                    });
                }
                current.map.put(path.get(path.size() - 1), p);
            });
        }

        public List<Pair<String, PropertyMap>> getConfigMaps()
        {
            return this.map.entrySet().stream()
                    .filter(entry -> entry.getValue() instanceof PropertyMap)
                    .map(entry -> Pair.of(entry.getKey(), (PropertyMap) entry.getValue()))
                    .toList();
        }

        public List<Pair<String, ConfigProperty<?>>> getConfigProperties()
        {
            List<Pair<String, ConfigProperty<?>>> properties = new ArrayList<>();
            this.map.forEach((name, entry) ->
            {
                if(entry instanceof ConfigProperty<?> property)
                {
                    properties.add(Pair.of(name, property));
                }
            });
            return properties;
        }

        @Override
        public String getPath()
        {
            return this.path;
        }
    }

    /**
     * Creates a tunnel from a ConfigProperty to a value in Config. This allows for a ConfigProperty
     * to be linked to any config and easily swappable.
     */
    public static class ValueProxy
    {
        private static final ValueProxy EMPTY = new ValueProxy();

        private final Config config;
        private final String path;

        private ValueProxy()
        {
            this.config = null;
            this.path = null;
        }

        private ValueProxy(Config config, String path)
        {
            this.config = config;
            this.path = path;
        }

        public boolean isLinked()
        {
            return this != EMPTY;
        }

        @Nullable
        public <T> T get()
        {
            if(this.isLinked() && this.config != null)
            {
                return this.config.get(this.path);
            }
            return null;
        }

        public <T> void set(T value)
        {
            if(this.isLinked() && this.config != null)
            {
                this.config.set(this.path, value);
            }
        }
    }

    public static class ValuePath
    {
        //TODO use list version
        private final String path;

        private ValuePath(String path)
        {
            this.path = path;
        }

        public String getPath()
        {
            return this.path;
        }
    }

    public interface IMapEntry
    {
        String getPath();
    }
}
