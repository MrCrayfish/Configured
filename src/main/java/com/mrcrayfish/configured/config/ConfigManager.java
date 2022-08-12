package com.mrcrayfish.configured.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.simple.ConfigProperty;
import com.mrcrayfish.configured.api.simple.SimpleConfig;
import com.mrcrayfish.configured.api.simple.SimpleProperty;
import com.mrcrayfish.configured.client.screen.IEditing;
import com.mrcrayfish.configured.client.screen.ListMenuScreen;
import com.mrcrayfish.configured.impl.simple.SimpleFolderEntry;
import com.mrcrayfish.configured.impl.simple.SimpleValue;
import com.mrcrayfish.configured.network.HandshakeMessages;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenOpenEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Author: MrCrayfish
 */
public class ConfigManager
{
    private static final Predicate<String> NAME_PATTERN = Pattern.compile("^[a-z_]+$").asMatchPredicate();
    private static final LevelResource WORLD_CONFIG = new LevelResource("serverconfig");

    private static ConfigManager instance;

    public static ConfigManager getInstance()
    {
        if(instance == null)
        {
            instance = new ConfigManager();
        }
        return instance;
    }

    private final Map<ResourceLocation, SimpleConfigEntry> configs;
    private IModConfig editingConfig;

    private ConfigManager()
    {
        Map<ResourceLocation, SimpleConfigEntry> configs = new HashMap<>();
        ConfigUtil.getAllSimpleConfigs().forEach(pair ->
        {
            ConfigScanData data = ConfigScanData.scan(pair.getLeft(), pair.getRight());
            SimpleConfigEntry entry = new SimpleConfigEntry(data);
            configs.put(entry.getName(), entry);
        });
        this.configs = ImmutableMap.copyOf(configs);
    }

    public List<SimpleConfigEntry> getConfigs()
    {
        return ImmutableList.copyOf(this.configs.values());
    }

    public List<Pair<String, HandshakeMessages.S2CConfigData>> getMessagesForLogin(boolean local)
    {
        if(local) return Collections.emptyList();
        return this.configs.values().stream()
            .filter(entry -> entry.getType().isSync() && entry.getFilePath() != null)
            .map(entry -> {
                ResourceLocation key = entry.getName();
                byte[] data = ConfigUtil.readBytes(entry.getFilePath());
                return Pair.of("SimpleConfig " + key, new HandshakeMessages.S2CConfigData(key, data));
            }).collect(Collectors.toList());
    }

    public void processConfigData(HandshakeMessages.S2CConfigData message)
    {
        Configured.LOGGER.info("Loading synced config from server: " + message.getKey());
        this.configs.get(message.getKey()).loadFromData(message.getData());
    }

    @SubscribeEvent
    public void onClientDisconnect(ClientPlayerNetworkEvent.LoggedOutEvent event)
    {
        Configured.LOGGER.info("Unloading synced configs from server");
        Connection connection = event.getConnection();
        if(connection != null && !connection.isMemoryConnection()) // Run only if disconnected from remote server
        {
            // Unloads all synced configs since they should no longer be accessible
            this.configs.values().stream().filter(entry -> entry.getType().isSync()).forEach(SimpleConfigEntry::unload);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerAboutToStartEvent event)
    {
        Configured.LOGGER.info("Loading server configs...");

        // Create the server config directory
        Path serverConfig = event.getServer().getWorldPath(WORLD_CONFIG);
        FileUtils.getOrCreateDirectory(serverConfig, "serverconfig");

        // Handle loading server configs based on type
        this.configs.values().forEach(entry ->
        {
            switch(entry.configType)
            {
                case WORLD, WORLD_SYNC -> entry.load(serverConfig);
                case SERVER, SERVER_SYNC -> entry.load(FMLPaths.CONFIGDIR.get());
                case DEDICATED_SERVER ->
                {
                    if(FMLEnvironment.dist.isDedicatedServer())
                    {
                        entry.load(FMLPaths.CONFIGDIR.get());
                    }
                }
            }
        });
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event)
    {
        Configured.LOGGER.info("Unloading server configs...");
        this.configs.values().stream().filter(entry -> entry.configType.isServer()).forEach(SimpleConfigEntry::unload);
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
        private final ConfigType configType;
        private final Set<ConfigProperty<?>> allProperties;
        private final PropertyMap propertyMap;
        private final ConfigSpec spec;
        private final ClassLoader classLoader;
        private final CommentedConfig comments; //TODO use comment node?
        @Nullable
        private Config config;

        private SimpleConfigEntry(ConfigScanData data)
        {
            Preconditions.checkArgument(!data.getConfig().id().trim().isEmpty(), "The 'id' of the config cannot be empty");
            Preconditions.checkArgument(ModList.get().isLoaded(data.getConfig().id()), "The 'id' of the config must match a mod id");
            Preconditions.checkArgument(!data.getConfig().name().trim().isEmpty(), "The 'name' of the config cannot be empty");
            Preconditions.checkArgument(data.getConfig().name().length() <= 64, "The 'name' of the config must be 64 characters or less");
            Preconditions.checkArgument(NAME_PATTERN.test(data.getConfig().name()), "The 'name' of the config is invalid. It can only contain 'a-z' and '_'");

            this.id = data.getConfig().id();
            this.name = data.getConfig().name();
            this.configType = data.getConfig().type();
            this.allProperties = ImmutableSet.copyOf(data.getProperties());
            this.propertyMap = new PropertyMap(this.allProperties);
            this.spec = ConfigUtil.createSpec(this.allProperties);
            this.comments = ConfigUtil.createComments(this.spec, data.getComments());
            this.classLoader = Thread.currentThread().getContextClassLoader();

            // Load non-server configs immediately
            if(!this.configType.isServer())
            {
                if(this.configType == ConfigType.MEMORY)
                {
                    this.load(null);
                }
                else
                {
                    this.load(FMLPaths.CONFIGDIR.get());
                }
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
            Optional<Dist> dist = this.getType().getDist();
            if(dist.isPresent() && !FMLEnvironment.dist.equals(dist.get()))
                return;
            Preconditions.checkState(this.config == null, "Config is already loaded. Unload before loading again.");
            CommentedConfig config = ConfigUtil.createSimpleConfig(configDir, this.id, this.name, CommentedConfig::inMemory);
            ConfigUtil.loadFileConfig(config);
            this.correct(config);
            this.allProperties.forEach(p -> p.updateProxy(new ValueProxy(config, p.getPath())));
            this.config = config;
            ConfigUtil.watchFileConfig(config, this::changeCallback);
        }

        private void loadFromData(byte[] data)
        {
            Preconditions.checkState(this.configType.isSync(), "Tried to load from data for a non-sync config");
            CommentedConfig config = TomlFormat.instance().createParser().parse(new ByteArrayInputStream(data));
            this.correct(config);
            this.allProperties.forEach(p -> p.updateProxy(new ValueProxy(config, p.getPath())));
            this.config = config;
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
                if(config instanceof CommentedConfig c)
                    c.putAllComments(this.comments);
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
            if(this.getType() == ConfigType.WORLD)
            {
                if(!ListMenuScreen.isPlayingGame())
                {
                    // Unload world configs since still in main menu
                    this.unloadServerConfig();
                }
                else if(this.configType.isSync())
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

        public ResourceLocation getName()
        {
            return new ResourceLocation(this.id, this.name);
        }

        @Nullable
        public Path getFilePath()
        {
            return this.config instanceof FileConfig ? ((FileConfig) this.config).getNioPath() : null;
        }

        @Override
        public IConfigEntry getRoot()
        {
            return new SimpleFolderEntry("Root", this.propertyMap, true);
        }

        @Override
        public ConfigType getType()
        {
            return this.configType;
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
            CommentedConfig config = ConfigUtil.createTempServerConfig(configDir, this.id, this.name);
            ConfigUtil.loadFileConfig(config);
            this.correct(config);
            config.putAllComments(this.comments);
            this.allProperties.forEach(p -> p.updateProxy(new ValueProxy(config, p.getPath())));
            this.config = config;
            result.accept(this);
        }

        @Override
        public void stopEditing()
        {
            // Attempts to unload the server config if player simply just went back
            if(this.config != null && this.getType() == ConfigType.WORLD)
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

    private static class ConfigScanData
    {
        private final SimpleConfig config;
        private final Set<ConfigProperty<?>> properties = new HashSet<>();
        private final Map<String, String> comments = new HashMap<>();

        private ConfigScanData(SimpleConfig config)
        {
            this.config = config;
        }

        public SimpleConfig getConfig()
        {
            return this.config;
        }

        public Set<ConfigProperty<?>> getProperties()
        {
            return this.properties;
        }

        public Map<String, String> getComments()
        {
            return this.comments;
        }

        private static ConfigScanData scan(SimpleConfig config, Object object)
        {
            Preconditions.checkArgument(!object.getClass().isPrimitive(), "SimpleConfig annotation can only be attached");
            ConfigScanData data = new ConfigScanData(config);
            data.scan(new Stack<>(), object);
            return data;
        }

        private void scan(Stack<String> stack, Object instance)
        {
            Field[] fields = instance.getClass().getDeclaredFields();
            Stream.of(fields).forEach(field -> Optional.ofNullable(field.getDeclaredAnnotation(SimpleProperty.class)).ifPresent(sp ->
            {
                stack.push(sp.name());
                try
                {
                    field.setAccessible(true);

                    // Read comment
                    String path = StringUtils.join(stack, ".");
                    if(!sp.comment().isEmpty())
                    {
                        this.comments.put(path, sp.comment());
                    }

                    // Read config property or object
                    Object obj = field.get(instance);
                    if(obj instanceof ConfigProperty<?> property)
                    {
                        property.initProperty(new ValuePath(path));
                        this.properties.add(property);
                    }
                    else
                    {
                        this.scan(stack, obj);
                    }
                }
                catch(IllegalAccessException e)
                {
                    throw new RuntimeException(e);
                }
                stack.pop();
            }));
        }
    }
}
