package com.mrcrayfish.configured.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.api.simple.ConfigProperty;
import com.mrcrayfish.configured.api.simple.SimpleConfig;
import com.mrcrayfish.configured.api.simple.SimpleProperty;
import com.mrcrayfish.configured.api.simple.StorageType;
import com.mrcrayfish.configured.impl.simple.SimpleFolderEntry;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.FileUtils;
import net.minecraftforge.fml.loading.moddiscovery.ModAnnotation;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Type;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
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
    private static final Type SIMPLE_CONFIG = Type.getType(SimpleConfig.class);

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

    private static ConfigSpec createSpec(Map<String, ConfigProperty<?>> map)
    {
        ConfigSpec spec = new ConfigSpec();
        map.forEach((path, property) -> property.defineSpec(spec, path));
        return spec;
    }

    private static Map<String, ConfigProperty<?>> gatherConfigProperties(Object object)
    {
        Map<String, ConfigProperty<?>> map = new HashMap<>();
        readFields(map, new Stack<>(), object);
        return ImmutableMap.copyOf(map);
    }

    private static void readFields(Map<String, ConfigProperty<?>> map, Stack<String> path, Object instance)
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
                    map.put(StringUtils.join(path, "."), property);
                }
                else
                {
                    readFields(map, path, obj);
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
        this.configs.stream().filter(entry -> entry.storage == StorageType.WORLD).forEach(entry -> {
            entry.load(serverConfig);
        });
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event)
    {
        Configured.LOGGER.info("Unloading world configs...");
        this.configs.stream().filter(entry -> entry.storage == StorageType.WORLD).forEach(SimpleConfigEntry::unload);
    }

    public static final class SimpleConfigEntry implements IModConfig
    {
        private final String id;
        private final String name;
        private final boolean sync;
        private final StorageType storage;
        private final Object instance;
        private final Map<String, ConfigProperty<?>> properties;
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
            this.storage = Optional.ofNullable((ModAnnotation.EnumHolder) data.get("storage")).map(holder -> StorageType.valueOf(holder.getValue())).orElse(StorageType.GLOBAL);
            this.instance = instance;
            this.properties = gatherConfigProperties(instance);
            this.propertyMap = new PropertyMap(this.properties);
            this.spec = createSpec(this.properties);
            this.classLoader = Thread.currentThread().getContextClassLoader();

            if(this.storage == StorageType.GLOBAL) // Load global configs immediately
            {
                this.load(FMLPaths.CONFIGDIR.get());
            }
            else if(this.storage == StorageType.MEMORY)
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
            this.properties.forEach((path, property) -> property.updateProxy(new ValueProxy(config, path)));
            this.config = config;
            ConfigUtil.watchFileConfig(config, this::changeCallback);
        }

        private void unload()
        {
            if(this.config != null)
            {
                this.properties.forEach((path, property) -> property.updateProxy(ValueProxy.EMPTY));
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
                this.properties.values().forEach(ConfigProperty::invalidateCache);
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
        public void saveConfig(IConfigEntry entry)
        {

        }

        @Override
        public IConfigEntry getRoot()
        {
            return new SimpleFolderEntry("Root", this.propertyMap, true);
        }

        @Override
        public ModConfig.Type getConfigType()
        {
            return this.storage == StorageType.WORLD ? ModConfig.Type.SERVER : ModConfig.Type.COMMON;
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

        @Override
        public void loadServerConfig(Path path, Consumer<IModConfig> result) throws IOException
        {

        }
    }

    public static class PropertyMap implements IMapEntry
    {
        private final Map<String, IMapEntry> map = new HashMap<>();

        private PropertyMap(String name) {}

        private PropertyMap(Map<String, ConfigProperty<?>> properties)
        {
            properties.forEach((rawPath, property) ->
            {
                PropertyMap current = this;
                List<String> path = com.electronwill.nightconfig.core.utils.StringUtils.split(rawPath, '.');
                for(int i = 0; i < path.size() - 1; i++)
                {
                    current = (PropertyMap) current.map.computeIfAbsent(path.get(i), PropertyMap::new);
                }
                current.map.put(path.get(path.size() - 1), property);
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

    public interface IMapEntry
    {
    }
}
