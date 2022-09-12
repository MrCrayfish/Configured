package com.mrcrayfish.configured.impl.simple;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigFormat;
import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.api.simple.ConfigProperty;
import com.mrcrayfish.configured.api.simple.SimpleConfig;
import com.mrcrayfish.configured.api.simple.SimpleProperty;
import com.mrcrayfish.configured.api.simple.event.SimpleConfigEvent;
import com.mrcrayfish.configured.network.HandshakeMessages;
import com.mrcrayfish.configured.util.ConfigHelper;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;
import net.minecraftforge.fml.loading.FMLConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.FileUtils;
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
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Author: MrCrayfish
 */
public class SimpleConfigManager
{
    private static final Predicate<String> NAME_PATTERN = Pattern.compile("^[a-z_]+$").asMatchPredicate();
    private static final LevelResource WORLD_CONFIG = new LevelResource("serverconfig");
    private static final Type SIMPLE_CONFIG = Type.getType(SimpleConfig.class);

    private static SimpleConfigManager instance;

    public static SimpleConfigManager getInstance()
    {
        if(instance == null)
        {
            instance = new SimpleConfigManager();
        }
        return instance;
    }

    private final Map<ResourceLocation, SimpleConfigImpl> configs;

    private SimpleConfigManager()
    {
        Map<ResourceLocation, SimpleConfigImpl> configs = new HashMap<>();
        getAllSimpleConfigs().forEach(pair ->
        {
            ConfigScanData data = ConfigScanData.analyze(pair.getLeft(), pair.getRight());
            SimpleConfigImpl entry = new SimpleConfigImpl(data);
            configs.put(entry.getName(), entry);
        });
        this.configs = ImmutableMap.copyOf(configs);
    }

    public List<SimpleConfigImpl> getConfigs()
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
                byte[] data = ConfigHelper.readBytes(entry.getFilePath());
                return Pair.of("SimpleConfig " + key, new HandshakeMessages.S2CConfigData(key, data));
            }).collect(Collectors.toList());
    }

    public boolean processConfigData(HandshakeMessages.S2CConfigData message)
    {
        Configured.LOGGER.info("Loading synced config from server: " + message.getKey());
        SimpleConfigImpl entry = this.configs.get(message.getKey());
        if(entry != null && entry.getType().isSync())
        {
            entry.loadFromData(message.getData());
            return true;
        }
        return false;
    }

    // Unloads all synced configs since they should no longer be accessible
    public void onClientDisconnect(@Nullable Connection connection)
    {
        if(connection != null && !connection.isMemoryConnection()) // Run only if disconnected from remote server
        {
            Configured.LOGGER.info("Unloading synced configs from server");
            this.configs.values().stream().filter(entry -> entry.getType().isSync()).forEach(entry -> entry.unload(true));
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
        this.configs.values().stream().filter(entry -> entry.configType.isServer()).forEach(entry -> entry.unload(true));
    }

    public static final class SimpleConfigImpl implements IModConfig
    {
        private final Object source;
        private final String id;
        private final String name;
        private final boolean readOnly;
        private final ConfigType configType;
        private final Set<ConfigProperty<?>> allProperties;
        private final PropertyMap propertyMap;
        private final ConfigSpec spec;
        private final ClassLoader classLoader;
        private final CommentedConfig comments;
        @Nullable
        private UnmodifiableConfig config;

        private SimpleConfigImpl(ConfigScanData data)
        {
            Preconditions.checkArgument(!data.getConfig().id().trim().isEmpty(), "The 'id' of the config cannot be empty");
            Preconditions.checkArgument(ModList.get().isLoaded(data.getConfig().id()), "The 'id' of the config must match a mod id");
            Preconditions.checkArgument(!data.getConfig().name().trim().isEmpty(), "The 'name' of the config cannot be empty");
            Preconditions.checkArgument(data.getConfig().name().length() <= 64, "The 'name' of the config must be 64 characters or less");
            Preconditions.checkArgument(NAME_PATTERN.test(data.getConfig().name()), "The 'name' of the config is invalid. It can only contain 'a-z' and '_'");

            this.source = data.getSource();
            this.id = data.getConfig().id();
            this.name = data.getConfig().name();
            this.readOnly = data.getConfig().readOnly();
            this.configType = data.getConfig().type();
            this.allProperties = ImmutableSet.copyOf(data.getProperties());
            this.propertyMap = new PropertyMap(this.allProperties);
            this.spec = createSpec(this.allProperties);
            this.comments = createComments(this.spec, data.getComments());
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
            UnmodifiableConfig config = this.createConfig(configDir);
            ConfigHelper.loadConfig(config);
            this.correct(config);
            this.allProperties.forEach(p -> p.updateProxy(new ValueProxy(config, p.getPath(), this.readOnly)));
            this.config = config;
            ConfigHelper.watchConfig(config, this::changeCallback);
            this.sendEvent(new SimpleConfigEvent.Load(this.source));
        }

        private void loadFromData(byte[] data)
        {
            this.unload(false);
            CommentedConfig config = TomlFormat.instance().createParser().parse(new ByteArrayInputStream(data));
            this.correct(config);
            this.allProperties.forEach(p -> p.updateProxy(new ValueProxy(config, p.getPath(), this.readOnly)));
            this.config = config;
            this.sendEvent(new SimpleConfigEvent.Load(this.source));
        }

        private UnmodifiableConfig createConfig(@Nullable Path configDir)
        {
            if(this.readOnly)
            {
                Preconditions.checkArgument(configDir != null, "Config dir must not be null for read only configs");
                return createReadOnlyConfig(configDir, this.id, this.name, this::correct);
            }
            return createSimpleConfig(configDir, this.id, this.name);
        }

        void unload(boolean sendEvent)
        {
            if(this.config != null)
            {
                this.allProperties.forEach(p -> p.updateProxy(ValueProxy.EMPTY));
                ConfigHelper.closeConfig(this.config);
                this.config = null;
                if(sendEvent)
                {
                    Configured.LOGGER.info("Sending config unload event for {}", this.getFileName());
                    this.sendEvent(new SimpleConfigEvent.Unload(this.source));
                }
            }
        }

        private void changeCallback()
        {
            Thread.currentThread().setContextClassLoader(this.classLoader);
            if(this.config != null && !this.isReadOnly())
            {
                ConfigHelper.loadConfig(this.config);
                this.correct(this.config);
                this.allProperties.forEach(ConfigProperty::invalidateCache);
                LogicalSidedProvider.WORKQUEUE.get(FMLEnvironment.dist.isClient() ? LogicalSide.CLIENT : LogicalSide.SERVER).submit(() -> {
                    this.sendEvent(new SimpleConfigEvent.Reload(this.source));
                });
            }
        }

        private void correct(UnmodifiableConfig config)
        {
            //TODO correct comments even if config is correct
            if(config instanceof Config && !this.spec.isCorrect((Config) config))
            {
                ConfigHelper.createBackup(config);
                this.spec.correct((Config) config);
                if(config instanceof CommentedConfig c)
                    c.putAllComments(this.comments);
                ConfigHelper.saveConfig(config);
            }
        }

        @Override
        public void update(IConfigEntry entry)
        {
            Preconditions.checkState(this.config != null, "Tried to update a config that is not loaded");

            // Prevent updating if read only or not a modifiable config
            if(this.readOnly || !(this.config instanceof Config))
                return;

            // Find changed values and return if nothing changed
            Set<IConfigValue<?>> changedValues = ConfigHelper.getChangedValues(entry);
            if(changedValues.isEmpty())
                return;

            // Update the config with new changes
            CommentedConfig newConfig = CommentedConfig.copy(this.config);
            changedValues.forEach(value ->
            {
                if(value instanceof SimpleValue<?> simpleValue)
                {
                    newConfig.set(simpleValue.getPath(), simpleValue.get());
                }
            });
            this.correct(newConfig);
            ((Config) this.config).putAll(newConfig);
            this.allProperties.forEach(ConfigProperty::invalidateCache);

            // Post handling
            if(this.getType().isServer())
            {
                if(!ConfigHelper.isPlayingGame())
                {
                    // Unload world configs since still in main menu
                    this.unload(false);
                    return;
                }
                else if(!ConfigHelper.isRunningLocalServer() && !this.getType().isSync())
                {
                    this.unload(false);
                    return;
                }
            }

            Configured.LOGGER.info("Sending config reloading event for {}", this.getFileName());
            this.sendEvent(new SimpleConfigEvent.Reload(this.source));
        }

        private ResourceLocation getName()
        {
            return new ResourceLocation(this.id, this.name);
        }

        @Nullable
        private Path getFilePath()
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

        @Override
        public boolean isReadOnly()
        {
            return this.readOnly;
        }

        @Override
        public void startEditing()
        {
            if(!ConfigHelper.isPlayingGame() && ConfigHelper.isServerConfig(this))
            {
                this.load(FMLPaths.CONFIGDIR.get());
            }
        }

        @Override
        public void stopEditing()
        {
            if(this.config != null)
            {
                if(this.getType().isServer() && (!ConfigHelper.isPlayingGame() || (!ConfigHelper.isRunningLocalServer() && !this.getType().isSync())))
                {
                    this.unload(false);
                }
            }
        }

        //TODO change how this works.
        @Override
        public void loadWorldConfig(Path configDir, Consumer<IModConfig> result)
        {
            if(!ConfigHelper.isWorldConfig(this))
                return;
            Preconditions.checkState(this.config == null, "Something went wrong and tried to load the server config again!");
            CommentedConfig config = createSimpleConfig(configDir, this.id, this.name);
            ConfigHelper.loadConfig(config);
            this.correct(config);
            config.putAllComments(this.comments);
            this.allProperties.forEach(p -> p.updateProxy(new ValueProxy(config, p.getPath(), this.readOnly)));
            this.config = config;
            result.accept(this);
        }

        @Override
        public boolean isChanged()
        {
            // Block unloaded world configs since the path is dynamic
            if(ConfigHelper.isWorldConfig(this) && this.config == null)
                return false;

            // An unloaded memory config is never going to be changed
            if(this.getType() == ConfigType.MEMORY && this.config == null)
                return false;

            // Test and return immediately if config already loaded
            if(this.config != null)
                return this.allProperties.stream().anyMatch(property -> !property.isDefault());

            // Temporarily load config to test for changes. Unloads immediately after test.
            CommentedFileConfig tempConfig = createTempConfig(FMLPaths.CONFIGDIR.get(), this.id, this.name);
            ConfigHelper.loadConfig(tempConfig);
            this.correct(tempConfig);
            tempConfig.putAllComments(this.comments);
            this.allProperties.forEach(p -> p.updateProxy(new ValueProxy(tempConfig, p.getPath(), this.readOnly)));
            boolean changed = this.allProperties.stream().anyMatch(property -> !property.isDefault());
            this.allProperties.forEach(p -> p.updateProxy(ValueProxy.EMPTY));
            tempConfig.close();
            return changed;
        }

        @Override
        public void restoreDefaults()
        {
            // Don't restore default if read only
            if(this.readOnly)
                return;

            // Block unloaded world configs since the path is dynamic
            if(ConfigHelper.isWorldConfig(this) && this.config == null)
                return;

            // Restore properties immediately if config already loaded
            if(this.config != null) {
                this.allProperties.forEach(ConfigProperty::restoreDefault);
                return;
            }

            // Temporarily loads the config, restores the defaults then saves and closes.
            CommentedFileConfig tempConfig = createTempConfig(FMLPaths.CONFIGDIR.get(), this.id, this.name);
            ConfigHelper.loadConfig(tempConfig);
            this.correct(tempConfig);
            tempConfig.putAllComments(this.comments);
            this.allProperties.forEach(property -> tempConfig.set(property.getPath(), property.getDefaultValue()));
            ConfigHelper.saveConfig(tempConfig);
            tempConfig.close();
        }

        private void sendEvent(SimpleConfigEvent event)
        {
            ModList.get().getModContainerById(this.id).ifPresent(container ->
            {
                if(container instanceof FMLModContainer modContainer)
                {
                    modContainer.getEventBus().post(event);
                }
            });
        }
    }

    public static class PropertyMap implements IMapEntry
    {
        private final Map<String, IMapEntry> map = new HashMap<>();

        private final List<String> path;

        private PropertyMap(List<String> path)
        {
            this.path = path;
        }

        private PropertyMap(Set<ConfigProperty<?>> properties)
        {
            this.path = null;
            properties.forEach(p ->
            {
                PropertyMap current = this;
                List<String> path = p.getPath();
                for(int i = 0; i < path.size() - 1; i++)
                {
                    int finalI = i;
                    current = (PropertyMap) current.map.computeIfAbsent(path.get(i), s -> {
                        return new PropertyMap(path.subList(0, finalI));
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

        public List<ConfigProperty<?>> getConfigProperties()
        {
            List<ConfigProperty<?>> properties = new ArrayList<>();
            this.map.forEach((name, entry) ->
            {
                if(entry instanceof ConfigProperty<?> property)
                {
                    properties.add(property);
                }
            });
            return properties;
        }

        public List<String> getPath()
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

        private final UnmodifiableConfig config;
        private final List<String> path;
        private final boolean readOnly;

        private ValueProxy()
        {
            this.config = null;
            this.path = null;
            this.readOnly = true;
        }

        private ValueProxy(UnmodifiableConfig config, List<String> path, boolean readOnly)
        {
            this.config = config;
            this.path = path;
            this.readOnly = readOnly;
        }

        public boolean isLinked()
        {
            return this != EMPTY;
        }

        public boolean isWritable()
        {
            return !this.readOnly;
        }

        @Nullable
        public <T> T get(BiFunction<UnmodifiableConfig, List<String>, T> function)
        {
            if(this.isLinked() && this.config != null)
            {
                return function.apply(this.config, this.path);
            }
            return null;
        }

        public <T> void set(T value)
        {
            if(this.isLinked() && this.isWritable() && this.config instanceof Config c)
            {
                c.set(this.path, value);
            }
        }
    }

    public static class PropertyData
    {
        private final String name;
        private final List<String> path;
        private final String translationKey;
        private final String comment;
        private final boolean worldRestart;
        private final boolean gameRestart;

        private PropertyData(String name, List<String> path, String translationKey, String comment, boolean worldRestart, boolean gameRestart)
        {
            this.name = name;
            this.path = ImmutableList.copyOf(path);
            this.translationKey = translationKey;
            this.comment = comment;
            this.worldRestart = worldRestart;
            this.gameRestart = gameRestart;
        }

        public String getName()
        {
            return this.name;
        }

        public List<String> getPath()
        {
            return this.path;
        }

        public String getTranslationKey()
        {
            return this.translationKey;
        }

        public String getComment()
        {
            return this.comment;
        }

        public boolean requiresWorldRestart()
        {
            return this.worldRestart;
        }

        public boolean requiresGameRestart()
        {
            return this.gameRestart;
        }
    }

    public interface IMapEntry {}

    private static class ConfigScanData
    {
        private final SimpleConfig config;
        private final Object source;
        private final Set<ConfigProperty<?>> properties = new HashSet<>();
        private final Map<List<String>, String> comments = new HashMap<>();

        private ConfigScanData(SimpleConfig config, Object source)
        {
            this.config = config;
            this.source = source;
        }

        public SimpleConfig getConfig()
        {
            return this.config;
        }

        public Object getSource()
        {
            return this.source;
        }

        public Set<ConfigProperty<?>> getProperties()
        {
            return this.properties;
        }

        public Map<List<String>, String> getComments()
        {
            return this.comments;
        }

        private static ConfigScanData analyze(SimpleConfig config, Object source)
        {
            Preconditions.checkArgument(!source.getClass().isPrimitive(), "SimpleConfig annotation can only be attached");
            ConfigScanData data = new ConfigScanData(config, source);
            data.scan(new Stack<>(), source);
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
                    if(!sp.comment().isEmpty())
                    {
                        this.comments.put(new ArrayList<>(stack), sp.comment());
                    }

                    // Read config property or object
                    Object obj = field.get(instance);
                    if(obj instanceof ConfigProperty<?> property)
                    {
                        List<String> path = new ArrayList<>(stack);
                        String key = String.format("simpleconfig.%s.%s.%s", this.config.id(), this.config.name(), StringUtils.join(path, '.'));
                        property.initProperty(new PropertyData(sp.name(), path, key, sp.comment(), sp.worldRestart(), sp.gameRestart()));
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

    public static CommentedConfig createSimpleConfig(@Nullable Path folder, String id, String name)
    {
        if(folder != null)
        {
            String fileName = String.format("%s.%s.toml", id, name);
            File file = new File(folder.toFile(), fileName);
            return CommentedFileConfig.builder(file).autosave().sync().onFileNotFound((file1, configFormat) -> initConfig(file1, configFormat, fileName)).build();
        }
        return CommentedConfig.inMemory();
    }

    public static UnmodifiableCommentedConfig createReadOnlyConfig(Path folder, String id, String name, Consumer<Config> corrector)
    {
        CommentedFileConfig temp = createTempConfig(folder, id, name);
        ConfigHelper.loadConfig(temp);
        corrector.accept(temp);
        CommentedConfig config = CommentedConfig.inMemory();
        config.putAll(temp);
        ConfigHelper.closeConfig(temp);
        return config.unmodifiable();
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
}
