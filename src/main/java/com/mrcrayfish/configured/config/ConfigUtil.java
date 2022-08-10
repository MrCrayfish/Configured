package com.mrcrayfish.configured.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigFormat;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.file.FileWatcher;
import net.minecraftforge.fml.loading.FMLConfig;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 */
public class ConfigUtil
{
    public static Config createSimpleConfig(@Nullable Path folder, String id, String name, Supplier<Config> fallback)
    {
        if(folder != null)
        {
            String fileName = String.format("%s.%s.toml", id, name);
            File file = new File(folder.toFile(), fileName);
            return CommentedFileConfig.builder(file).autosave().sync().onFileNotFound((file1, configFormat) -> initConfig(file1, configFormat, fileName)).build();
        }
        return fallback.get();
    }

    public static Config createTempServerConfig(Path folder, String id, String name)
    {
        String fileName = String.format("%s.%s.toml", id, name);
        File file = new File(folder.toFile(), fileName);
        return CommentedFileConfig.builder(file).autosave().sync().onFileNotFound((file1, configFormat) -> initConfig(file1, configFormat, fileName)).build();
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
}
