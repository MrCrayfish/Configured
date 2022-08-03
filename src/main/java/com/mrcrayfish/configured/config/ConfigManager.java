package com.mrcrayfish.configured.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigFormat;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.file.FileWatcher;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.api.config.ConfigProperty;
import com.mrcrayfish.configured.api.config.SimpleConfig;
import com.mrcrayfish.configured.api.config.SimpleProperty;
import com.mrcrayfish.configured.api.config.StorageType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.FileUtils;
import net.minecraftforge.fml.loading.moddiscovery.ModAnnotation;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.Type;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Stream;

/**
 * Author: MrCrayfish
 */
public class ConfigManager
{
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

    private final List<ConfigEntry> configs;

    private ConfigManager()
    {
        this.configs = this.getAllSimpleConfigs();
    }

    private List<ConfigEntry> getAllSimpleConfigs()
    {
        List<ModFileScanData.AnnotationData> annotations = ModList.get().getAllScanData().stream().map(ModFileScanData::getAnnotations).flatMap(Collection::stream).filter(a -> SIMPLE_CONFIG.equals(a.annotationType())).toList();
        List<ConfigEntry> configs = new ArrayList<>();
        annotations.forEach(data ->
        {
            try
            {
                Class<?> configClass = Class.forName(data.clazz().getClassName());
                Field field = configClass.getDeclaredField(data.memberName());
                field.setAccessible(true);
                Object object = field.get(null);
                if(!object.getClass().isPrimitive())
                {
                    configs.add(new ConfigEntry(data.annotationData(), object));
                }
            }
            catch(NoSuchFieldException | ClassNotFoundException | IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
        });
        return ImmutableList.copyOf(configs);
    }

    private static Map<String, ConfigProperty<?>> gatherConfigProperties(Object object)
    {
        Map<String, ConfigProperty<?>> map = new HashMap<>();
        readFields(map, new Stack<>(), object);
        return ImmutableMap.copyOf(map);
    }

    private static ConfigSpec createSpec(Map<String, ConfigProperty<?>> map)
    {
        ConfigSpec spec = new ConfigSpec();
        map.forEach((path, property) -> property.defineSpec(spec, path));
        return spec;
    }

    private static void readFields(Map<String, ConfigProperty<?>> map, Stack<String> path, Object instance)
    {
        Field[] fields = instance.getClass().getDeclaredFields();
        Stream.of(fields).forEach(field -> Optional.ofNullable(field.getDeclaredAnnotation(SimpleProperty.class)).ifPresent(sp ->
        {
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

    private static boolean initConfigFile(final Path file, final ConfigFormat<?> format, final String fileName) throws IOException
    {
        Files.createDirectories(file.getParent());
        Path defaultConfigPath = FMLPaths.GAMEDIR.get().resolve(FMLConfig.defaultConfigPath());
        Path defaultConfigFile = defaultConfigPath.resolve(fileName);
        if(Files.exists(defaultConfigFile))
        {
            Files.copy(defaultConfigFile, file);
        }
        else
        {
            Files.createFile(file);
            format.initEmptyFile(file);
        }
        return true;
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
        this.configs.stream().filter(entry -> entry.storage == StorageType.WORLD).forEach(ConfigEntry::unload);
    }

    private static final class ConfigEntry
    {
        private final String id;
        private final String name;
        private final boolean sync;
        private final StorageType storage;
        private final Object instance;
        private final Map<String, ConfigProperty<?>> properties;
        private final ConfigSpec spec;
        private final ClassLoader classLoader;
        @Nullable
        private Config config;

        private ConfigEntry(Map<String, Object> data, Object instance)
        {
            Preconditions.checkArgument(data.get("id") instanceof String, "The 'id' of the config is not a String");
            Preconditions.checkArgument(!((String) data.get("id")).trim().isEmpty(), "The 'id' of the config cannot be empty");
            Preconditions.checkArgument(ModList.get().isLoaded(((String) data.get("id"))), "The 'id' of the config must match a mod id");
            Preconditions.checkArgument(data.get("name") instanceof String, "The 'name' of the config is not a String");
            Preconditions.checkArgument(!((String) data.get("name")).trim().isEmpty(), "The 'name' of the config cannot be empty");
            Preconditions.checkArgument(((String) data.get("name")).length() <= 64, "The 'name' of the config must be 64 characters or less");

            this.id = (String) data.get("id");
            this.name = (String) data.get("name");
            this.sync = (Boolean) data.getOrDefault("sync", false);
            this.storage = Optional.ofNullable((ModAnnotation.EnumHolder) data.get("storage")).map(holder -> StorageType.valueOf(holder.getValue())).orElse(StorageType.GLOBAL);
            this.instance = instance;
            this.properties = gatherConfigProperties(instance);
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

        private void load(@Nullable Path folder)
        {
            Config config = folder != null ? createConfigFromFile(folder, this) : CommentedConfig.inMemory();
            this.correct(config);
            this.properties.forEach((path, property) -> property.updateProxy(new ValueProxy(config, path)));
            this.config = config;
            this.startWatching();
        }

        private void unload()
        {
            if(this.config != null)
            {
                this.properties.forEach((path, property) -> property.updateProxy(ValueProxy.EMPTY));
                this.closeConfig();
                this.config = null;
            }
        }

        private void changeCallback()
        {
            Thread.currentThread().setContextClassLoader(this.classLoader);
            if(this.config != null)
            {
                loadConfig(this.config);
                this.correct(this.config);
                this.properties.values().forEach(ConfigProperty::invalidateCache);
            }
        }

        private void correct(Config config)
        {
            if(!this.spec.isCorrect(config))
            {
                this.spec.correct(config);
                saveConfig(config);
            }
        }

        private void startWatching()
        {
            if(this.config instanceof FileConfig fileConfig)
            {
                try
                {
                    FileWatcher.defaultInstance().addWatch(fileConfig.getFile(), this::changeCallback);
                }
                catch(IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }

        private void closeConfig()
        {
            if(this.config instanceof FileConfig fileConfig)
            {
                FileWatcher.defaultInstance().removeWatch(fileConfig.getFile());
                fileConfig.close();
            }
        }

        public String id()
        {
            return this.id;
        }

        public String name()
        {
            return this.name;
        }

        public boolean sync()
        {
            return this.sync;
        }

        public StorageType storage()
        {
            return this.storage;
        }

        @Override
        public boolean equals(Object obj)
        {
            if(obj == this) return true;
            if(obj == null || obj.getClass() != this.getClass()) return false;
            var that = (ConfigEntry) obj;
            return Objects.equals(this.id, that.id) && Objects.equals(this.name, that.name);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(this.id, this.name);
        }

        @Override
        public String toString()
        {
            return "ConfigInfo[" + "id=" + this.id + ", " + "name=" + this.name + ", " + "sync=" + this.sync + ", " + "storage=" + this.storage + ']';
        }

        private static FileConfig createConfigFromFile(Path folder, ConfigEntry entry)
        {
            String fileName = String.format("%s.%s.toml", entry.id, entry.name);
            File file = new File(folder.toFile(), fileName);
            FileConfig config = CommentedFileConfig.builder(file).autosave().sync().onFileNotFound((file1, configFormat) -> initConfigFile(file1, configFormat, fileName)).build();
            loadConfig(config);
            return config;
        }

        private static void loadConfig(Config config)
        {
            if(config instanceof FileConfig fileConfig)
            {
                try
                {
                    fileConfig.load();
                }
                catch(Exception ignored)
                {
                    //TODO error handling
                }
            }
        }

        private static void saveConfig(Config config)
        {
            if(config instanceof FileConfig fileConfig)
            {
                fileConfig.save();
            }
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
}
