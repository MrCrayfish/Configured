package com.mrcrayfish.configured;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.EnumGetMethod;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.common.collect.ImmutableSet;
import com.mrcrayfish.configured.api.Environment;
import com.mrcrayfish.configured.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Author: MrCrayfish
 */
public class Config
{
    private static final Pattern UUID_PATTERN = Pattern.compile("^[\\da-fA-F]{8}\\b-[\\da-fA-F]{4}\\b-[\\da-fA-F]{4}\\b-[\\da-fA-F]{4}\\b-[\\da-fA-F]{12}$");

    private static CommentedFileConfig clientConfig;
    private static final ConfigSpec CLIENT_SPEC = Util.make(() -> {
        ConfigSpec spec = new ConfigSpec();
        spec.define("forceConfiguredMenu", false);
        spec.define("includeFoldersInSearch", false);
        spec.defineRestrictedEnum("changedFormatting", ChatFormatting.ITALIC, ImmutableSet.copyOf(ChatFormatting.values()), EnumGetMethod.NAME_IGNORECASE);
        return spec;
    });

    private static CommentedFileConfig developerConfig;
    private static final ConfigSpec DEVELOPER_SPEC = Util.make(() -> {
        ConfigSpec spec = new ConfigSpec();
        spec.define("enabled", false);
        spec.defineList("developers", Collections.emptyList(), e -> {
            return e instanceof String s && UUID_PATTERN.matcher(s).matches();
        });
        spec.define("broadcastLogs", true);
        return spec;
    });

    public static void load()
    {
        Path configPath = Services.PLATFORM.getConfigPath();
        clientConfig = createConfig(new File(configPath.toFile(), "configured-client.toml"));
        clientConfig.load();
        CLIENT_SPEC.correct(clientConfig);
        clientConfig.save();

        if(Services.PLATFORM.getEnvironment() == Environment.DEDICATED_SERVER)
        {
            developerConfig = createConfig(new File(configPath.toFile(), "configured-developer.toml"));
            developerConfig.load();
            DEVELOPER_SPEC.correct(developerConfig);
            developerConfig.save();
        }
    }

    private static CommentedFileConfig createConfig(File configFile)
    {
        return CommentedFileConfig.builder(configFile).autosave().sync().onFileNotFound((file, format) -> {
            Files.createDirectories(file.getParent());
            Files.createFile(file);
            format.initEmptyFile(file);
            return false;
        }).build();
    }

    public static boolean isForceConfiguredMenu()
    {
        return clientConfig.get("forceConfiguredMenu");
    }

    public static boolean isIncludeFoldersInSearch()
    {
        return clientConfig.get("includeFoldersInSearch");
    }

    public static ChatFormatting getChangedFormatting()
    {
        return clientConfig.getEnumOrElse("changedFormatting", ChatFormatting.ITALIC, EnumGetMethod.NAME_IGNORECASE);
    }

    public static boolean isDeveloperEnabled()
    {
        return developerConfig != null && developerConfig.<Boolean>get("enabled");
    }

    public static List<String> getDevelopers()
    {
        if(developerConfig != null)
        {
            return developerConfig.getOrElse("developers", Collections.emptyList());
        }
        return Collections.emptyList();
    }

    public static boolean shouldBroadcastLogs()
    {
        return developerConfig != null && developerConfig.<Boolean>get("broadcastLogs");
    }
}
