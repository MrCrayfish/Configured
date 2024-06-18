package com.mrcrayfish.configured;

import com.google.common.collect.ImmutableList;
import com.mrcrayfish.configured.api.Environment;
import com.mrcrayfish.configured.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Author: MrCrayfish
 */
public class Config
{
    private static final Pattern UUID_PATTERN = Pattern.compile("^[\\da-fA-F]{8}\\b-[\\da-fA-F]{4}\\b-[\\da-fA-F]{4}\\b-[\\da-fA-F]{4}\\b-[\\da-fA-F]{12}$");
    private static final String VALID_CHAT_FORMATTING = Util.make(() -> {
        StringJoiner joiner = new StringJoiner(", ");
        List.of(ChatFormatting.values()).forEach(formatting -> joiner.add(formatting.getName()));
        return joiner.toString();
    });

    private static final String DEFAULT_CLIENT_CONFIG = """
        # -------- CONFIGURED CLIENT CONFIG --------
        # If properties are missing, delete this file
        # and load the game to regenerate the config.
        
        # [Force Configured Menu]
        # If enabled, will attempt to override config screens provided by
        # other mods in favour for Configured's auto generated screen. May
        # not works for all mods.
        # Possible values: true, false
        forceConfiguredMenu=false
        
        # [Include Folders in Search]
        # When using the search bar in the config screen, the results will
        # also include folders and not just config properties.
        # Possible values: true, false
        includeFoldersInSearch=false
        
        # [Changed Formatting]
        # The chat formatting of a config property name when it has changed
        # during editing.
        # Possible values: %s
        changedFormatting=ITALIC
        """.formatted(VALID_CHAT_FORMATTING);

    private static final String DEFAULT_DEVELOPER_CONFIG = """
        # ------- CONFIGURED DEVELOPER CONFIG -------
        # If properties are missing, delete this file
        # and load the game to regenerate the config.
        
        # [Developer Mode]
        # Enables the ability to update remote configs. You should only enabled
        # developer mode when you are developing your server. This mode should not
        # be enabled if you are running in production, a public server, or a server
        # with players you don't trust. Even if this mode is enabled, you will still
        # need to authorise players who have access to editing remote configs by
        # listing them in the developers property below and they must also have
        # operator privileges.
        # Possible values: true, false
        developerMode=false
        
        # [Broadcast Logs]
        # When a remote config is updated by a developer, broadcast those changes,
        # successful or not, into the chat of other developers. The log is also included
        # in the normal log of the game.
        # Possible values: true, false
        broadcastLogs=true
        
        # [Developers]
        # A list of comma separated UUIDS of players who are authorised to edit remote
        # configs. The players must also have operator privileges.
        # You can find the UUID of a player in the log file when they join your server.
        developers=
        """;

    // Client config
    private static boolean forceConfiguredMenu = false;
    private static boolean includeFoldersInSearch = false;
    private static ChatFormatting changedFormatting = ChatFormatting.ITALIC;

    // Developer config
    private static boolean developerMode = false;
    private static boolean broadcastLogs = false;
    private static ImmutableList<UUID> developers = ImmutableList.of();

    public static void load(Path path)
    {
        Environment environment = Services.PLATFORM.getEnvironment();
        if(environment == Environment.CLIENT)
        {
            loadConfigFile(path, "configured-client.properties", DEFAULT_CLIENT_CONFIG).ifPresent(file -> {
                try {
                    Properties properties = new Properties();
                    properties.load(new FileInputStream(file));
                    forceConfiguredMenu = Boolean.parseBoolean(properties.getProperty("forceConfiguredMenu", "false"));
                    includeFoldersInSearch = Boolean.parseBoolean(properties.getProperty("includeFoldersInSearch", "false"));
                    changedFormatting = ChatFormatting.getByName(properties.getProperty("changedFormatting", "italic"));
                } catch(Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        else if(environment == Environment.DEDICATED_SERVER)
        {
            loadConfigFile(path, "configured-developer.properties", DEFAULT_DEVELOPER_CONFIG).ifPresent(file -> {
                try {
                    Properties properties = new Properties();
                    properties.load(new FileInputStream(file));
                    developerMode = Boolean.parseBoolean(properties.getProperty("developerMode", "false"));
                    broadcastLogs = Boolean.parseBoolean(properties.getProperty("broadcastLogs", "true"));
                    developers = readDeveloperList(properties.getProperty("developers", ""));
                } catch(Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private static ImmutableList<UUID> readDeveloperList(String value)
    {
        if(value.isBlank())
            return ImmutableList.of();

        List<UUID> list = new ArrayList<>();
        for(String rawUuid : value.split(","))
        {
            rawUuid = rawUuid.trim();
            if(!UUID_PATTERN.matcher(rawUuid).matches()) {
                Constants.LOG.error("Invalid UUID when loading developer config: {}", rawUuid);
                continue;
            }
            list.add(UUID.fromString(rawUuid));
        }
        return ImmutableList.copyOf(list);
    }

    private static Optional<File> loadConfigFile(Path path, String name, String defaultConfig)
    {
        Path file = path.resolve(name);
        if(!Files.exists(file))
        {
            try
            {
                Files.writeString(file, defaultConfig, StandardOpenOption.CREATE);
            }
            catch(IOException e)
            {
                return Optional.empty();
            }
        }
        return Optional.of(file.toFile());
    }

    public static boolean isForceConfiguredMenu()
    {
        return forceConfiguredMenu;
    }

    public static boolean isIncludeFoldersInSearch()
    {
        return includeFoldersInSearch;
    }

    public static ChatFormatting getChangedFormatting()
    {
        return changedFormatting;
    }

    public static boolean isDeveloperEnabled()
    {
        return developerMode;
    }

    public static ImmutableList<UUID> getDevelopers()
    {
        return developers;
    }

    public static boolean shouldBroadcastLogs()
    {
        return broadcastLogs;
    }
}
