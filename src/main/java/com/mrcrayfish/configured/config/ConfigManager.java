package com.mrcrayfish.configured.config;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileWatcher;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mrcrayfish.configured.api.config.ConfigProperty;
import com.mrcrayfish.configured.api.config.SimpleConfig;
import com.mrcrayfish.configured.api.config.SimpleProperty;
import com.mrcrayfish.configured.api.config.StorageType;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Author: MrCrayfish
 */
public class ConfigManager
{
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

    private static final class ConfigEntry
    {
        private final String id;
        private final String name;
        private final boolean sync;
        private final StorageType storage;
        private final Object instance;
        private final Map<String, ConfigProperty<?>> properties;
        private final ConfigSpec spec;
        private final CommentedFileConfig config;
        private final ClassLoader classLoader;

        private ConfigEntry(Map<String, Object> data, Object instance)
        {
            this.id = (String) data.get("id");
            this.name = (String) data.get("name");
            this.sync = (Boolean) data.getOrDefault("sync", false);
            this.storage = null;//(StorageType) data.computeIfPresent("storage", StorageType.GLOBAL);
            this.instance = instance;
            this.properties = gatherConfigProperties(instance);
            this.spec = createSpec(this.properties);
            this.config = this.createConfig();
            this.classLoader = Thread.currentThread().getContextClassLoader();
        }

        private CommentedFileConfig createConfig()
        {
            String fileName = String.format("%s.%s.toml", this.id, this.name);
            File file = new File(FMLPaths.CONFIGDIR.get().toFile(), fileName);
            CommentedFileConfig config = CommentedFileConfig.builder(file).autosave().sync().build();
            config.load();
            if(!this.spec.isCorrect(config))
            {
                this.spec.correct(config);
                config.save();
            }
            this.properties.forEach((path, property) -> property.initialize(new ConfigSupplier<>(() -> config.get(path), v -> config.set(path, v))));
            try
            {
                FileWatcher.defaultInstance().addWatch(file, this::changeCallback);
            }
            catch(IOException e)
            {
                throw new RuntimeException(e);
            }
            return config;
        }

        private void changeCallback()
        {
            Thread.currentThread().setContextClassLoader(this.classLoader);
            if(!this.spec.isCorrect(this.config))
            {
                this.spec.correct(this.config);
                this.config.save();
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

        public Object instance()
        {
            return this.instance;
        }

        @Override
        public boolean equals(Object obj)
        {
            if(obj == this) return true;
            if(obj == null || obj.getClass() != this.getClass()) return false;
            var that = (ConfigEntry) obj;
            return Objects.equals(this.id, that.id) && Objects.equals(this.name, that.name) && this.sync == that.sync && Objects.equals(this.instance, that.instance);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(this.id, this.name, this.sync, this.instance);
        }

        @Override
        public String toString()
        {
            return "ConfigInfo[" + "id=" + this.id + ", " + "name=" + this.name + ", " + "sync=" + this.sync + ", " + "instance=" + this.instance + ']';
        }
    }

    public static class ConfigSupplier<T>
    {
        private final Supplier<T> getter;
        private final Consumer<T> setter;

        private ConfigSupplier(Supplier<T> getter, Consumer<T> setter)
        {
            this.getter = getter;
            this.setter = setter;
        }

        public Supplier<T> getGetter()
        {
            return this.getter;
        }

        public Consumer<T> getSetter()
        {
            return this.setter;
        }
    }
}
