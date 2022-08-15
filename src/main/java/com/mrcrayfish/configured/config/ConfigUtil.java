package com.mrcrayfish.configured.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigFormat;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.file.FileWatcher;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.mrcrayfish.configured.api.simple.ConfigProperty;
import com.mrcrayfish.configured.api.simple.SimpleConfig;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Type;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 */
public class ConfigUtil
{
    private static final Type SIMPLE_CONFIG = Type.getType(SimpleConfig.class);

    public static CommentedConfig createSimpleConfig(@Nullable Path folder, String id, String name, Supplier<CommentedConfig> fallback)
    {
        if(folder != null)
        {
            String fileName = String.format("%s.%s.toml", id, name);
            File file = new File(folder.toFile(), fileName);
            return CommentedFileConfig.builder(file).autosave().sync().onFileNotFound((file1, configFormat) -> initConfig(file1, configFormat, fileName)).build();
        }
        return fallback.get();
    }

    public static CommentedConfig createTempServerConfig(Path folder, String id, String name)
    {
        String fileName = String.format("%s.%s.toml", id, name);
        File file = new File(folder.toFile(), fileName);
        return CommentedFileConfig.builder(file).autosave().sync().onFileNotFound((file1, configFormat) -> initConfig(file1, configFormat, fileName)).build();
    }

    public static CommentedFileConfig createTempConfig(Path folder, String id, String name)
    {
        String fileName = String.format("%s.%s.toml", id, name);
        File file = new File(folder.toFile(), fileName);
        return CommentedFileConfig.builder(file).sync().onFileNotFound((file1, configFormat) -> initConfig(file1, configFormat, fileName)).build();
    }

    private static boolean initConfig(final Path file, final ConfigFormat<?> format, final String fileName) throws IOException
    {
        Files.createDirectories(file.getParent());
        Path defaultConfigPath = FMLPaths.GAMEDIR.get().resolve(FMLConfig.defaultConfigPath());
        Path defaultConfigFile = defaultConfigPath.resolve(fileName);
        if(Files.exists(defaultConfigFile))
        {
            Files.copy(defaultConfigFile, file);
            return true;
        }
        Files.createFile(file);
        format.initEmptyFile(file);
        return false;
    }

    public static void watchFileConfig(Config config, Runnable callback)
    {
        if(config instanceof FileConfig fileConfig)
        {
            try
            {
                FileWatcher.defaultInstance().addWatch(fileConfig.getFile(), callback);
            }
            catch(IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public static void closeFileConfig(Config config)
    {
        if(config instanceof FileConfig fileConfig)
        {
            FileWatcher.defaultInstance().removeWatch(fileConfig.getFile());
            fileConfig.close();
        }
    }

    public static void loadFileConfig(Config config)
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

    public static void saveFileConfig(Config config)
    {
        if(config instanceof FileConfig fileConfig)
        {
            fileConfig.save();
        }
    }

    public static byte[] readBytes(Path path)
    {
        try
        {
            return Files.readAllBytes(path);
        }
        catch(IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static List<Pair<SimpleConfig, Object>> getAllSimpleConfigs()
    {
        List<ModFileScanData.AnnotationData> annotations = ModList.get().getAllScanData().stream().map(ModFileScanData::getAnnotations).flatMap(Collection::stream).filter(a -> SIMPLE_CONFIG.equals(a.annotationType())).toList();
        List<Pair<SimpleConfig, Object>> configs = new ArrayList<>();
        annotations.forEach(data ->
        {
            try
            {
                Class<?> configClass = Class.forName(data.clazz().getClassName());
                Field field = configClass.getDeclaredField(data.memberName());
                field.setAccessible(true);
                Object object = field.get(null);
                Optional.ofNullable(field.getDeclaredAnnotation(SimpleConfig.class)).ifPresent(simpleConfig -> {
                    configs.add(Pair.of(simpleConfig, object));
                });
            }
            catch(NoSuchFieldException | ClassNotFoundException | IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
        });
        return configs;
    }

    public static ConfigSpec createSpec(Set<ConfigProperty<?>> properties)
    {
        ConfigSpec spec = new ConfigSpec();
        properties.forEach(p -> p.defineSpec(spec));
        return spec;
    }

    public static CommentedConfig createComments(ConfigSpec spec, Map<List<String>, String> comments)
    {
        CommentedConfig config = CommentedConfig.inMemory();
        spec.correct(config);
        comments.forEach(config::setComment);
        return config;
    }

    public static String createTranslationKey(SimpleConfig config, List<String> path)
    {
        return String.format("simpleconfig.%s.%s.%s", config.id(), config.name(), StringUtils.join(path, '.'));
    }
}
