package com.mrcrayfish.configured.impl.framework;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.api.*;
import com.mrcrayfish.configured.client.ClientSessionData;
import com.mrcrayfish.configured.impl.framework.message.MessageFramework;
import com.mrcrayfish.configured.platform.Services;
import com.mrcrayfish.configured.util.ConfigHelper;
import com.mrcrayfish.framework.api.config.AbstractProperty;
import com.mrcrayfish.framework.api.config.event.FrameworkConfigEvents;
import com.mrcrayfish.framework.config.FrameworkConfigManager;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.*;

/**
 * Author: MrCrayfish
 */
public class FrameworkModConfig implements IModConfig
{
    private static final Map<com.mrcrayfish.framework.api.config.ConfigType, ConfigType> TYPE_MAPPER = Util.make(() -> {
        ImmutableMap.Builder<com.mrcrayfish.framework.api.config.ConfigType, ConfigType> builder = ImmutableMap.builder();
        builder.put(com.mrcrayfish.framework.api.config.ConfigType.CLIENT, ConfigType.CLIENT);
        builder.put(com.mrcrayfish.framework.api.config.ConfigType.UNIVERSAL, ConfigType.UNIVERSAL);
        builder.put(com.mrcrayfish.framework.api.config.ConfigType.SERVER, ConfigType.SERVER);
        builder.put(com.mrcrayfish.framework.api.config.ConfigType.SERVER_SYNC, ConfigType.SERVER_SYNC);
        builder.put(com.mrcrayfish.framework.api.config.ConfigType.DEDICATED_SERVER, ConfigType.DEDICATED_SERVER);
        builder.put(com.mrcrayfish.framework.api.config.ConfigType.WORLD, ConfigType.WORLD);
        builder.put(com.mrcrayfish.framework.api.config.ConfigType.WORLD_SYNC, ConfigType.WORLD_SYNC);
        builder.put(com.mrcrayfish.framework.api.config.ConfigType.MEMORY, ConfigType.MEMORY);
        return builder.build();
    });

    private final FrameworkConfigManager.FrameworkConfigImpl config;
    private final Supplier<PropertyMap> map;

    public FrameworkModConfig(FrameworkConfigManager.FrameworkConfigImpl config)
    {
        this.config = config;
        this.map = Suppliers.memoize(() -> new PropertyMap(config));
    }

    @Override
    public ActionResult update(IConfigEntry entry)
    {
        Preconditions.checkState(this.config.getConfig() != null, "Tried to update a config that is not loaded");

        // Prevent updating if read only or not a modifiable config
        if(this.config.isReadOnly() || !(this.config.getConfig() instanceof Config configData))
            return ActionResult.fail(Component.translatable("configured.gui.update_error.read_only"));

        // Find changed values and return if nothing changed
        Set<IConfigValue<?>> changedValues = ConfigHelper.getChangedValues(entry);
        if(changedValues.isEmpty())
            return ActionResult.success();

        // Update the config with new changes
        CommentedConfig newConfigData = CommentedConfig.inMemory();
        changedValues.forEach(value -> {
            if(value instanceof FrameworkValue<?> frameworkValue) {
                newConfigData.set(frameworkValue.getPath(), frameworkValue.get());
            }
        });
        configData.putAll(newConfigData);
        this.config.correct(configData);
        this.config.getAllProperties().forEach(AbstractProperty::invalidateCache);

        // Post handling
        if(this.getType().isServer())
        {
            if(!ConfigHelper.isPlayingGame())
            {
                // Unload world configs since still in main menu
                this.config.unload(false);
                return ActionResult.success();
            }

            this.syncToServer();

            if(!ConfigHelper.isIntegratedServer() && !this.getType().isSync())
            {
                this.config.unload(false);
                return ActionResult.success();
            }
        }

        Constants.LOG.info("Sending config reloading event for {}", this.getFileName());
        FrameworkConfigEvents.RELOAD.post().handle(this.config.getSource());
        return ActionResult.success();
    }

    @Override
    public IConfigEntry createRootEntry()
    {
        return new FrameworkFolderEntry(this.map.get());
    }

    @Override
    public ConfigType getType()
    {
        return TYPE_MAPPER.get(this.config.getType());
    }

    @Override
    public String getFileName()
    {
        return this.config.getFileName();
    }

    @Override
    public String getModId()
    {
        return this.config.getName().getNamespace();
    }

    @Override
    public ActionResult loadWorldConfig(Path path)
    {
        if(!this.config.isLoaded())
        {
            this.config.load(path, false);
            if(this.config.isLoaded())
            {
                return ActionResult.success();
            }
            return ActionResult.fail(Component.translatable("configured.gui.load_world_config_failed"));
        }
        return ActionResult.success();
    }

    @Override
    public boolean isReadOnly()
    {
        return this.config.isReadOnly();
    }

    @Override
    public boolean isChanged()
    {
        return this.config.isChanged();
    }

    @Override
    public Optional<Runnable> restoreDefaultsTask()
    {
        if(this.config.isReadOnly())
            return Optional.empty();

        if(FrameworkConfigHelper.isWorldType(this.config) && !this.config.isLoaded())
            return Optional.empty();

        if(this.config.getType().isServer() && ConfigHelper.isPlayingOnRemoteServer())
            return Optional.empty();

        return Optional.of(this.config::restoreDefaults);
    }

    @Override
    public void startEditing()
    {
        if(!ConfigHelper.isPlayingGame() && ConfigHelper.isServerConfig(this))
        {
            this.config.load(com.mrcrayfish.framework.platform.Services.CONFIG.getConfigPath(), false);
        }
    }

    @Override
    public void stopEditing(boolean changed)
    {
        if(this.config.getConfig() == null)
            return;

        if(!this.getType().isServer())
            return;

        if(ConfigHelper.isPlayingGame() && (ConfigHelper.isIntegratedServer() || this.getType().isSync()))
            return;

        this.config.unload(false);
    }

    @Override
    public Optional<Runnable> requestFromServerTask()
    {
        if(this.config.getType().isServer() && !this.config.getType().isSync() && this.config.getType().getEnv().stream().noneMatch(com.mrcrayfish.framework.api.Environment::isDedicatedServer))
        {
            return Optional.of(() -> Services.PLATFORM.sendFrameworkConfigRequest(this.config.getName()));
        }
        return Optional.empty();
    }

    @Override
    public ActionResult canPlayerEdit(@Nullable Player player)
    {
        ExecutionContext context = new ExecutionContext(player);
        if(context.isClient())
        {
            return switch(this.getType()) {
                case DEDICATED_SERVER -> ActionResult.fail();
                case CLIENT, UNIVERSAL, MEMORY -> {
                    if(context.isMainMenu() || context.isLocalPlayer()) {
                        yield ActionResult.success();
                    }
                    yield ActionResult.fail();
                }
                case SERVER, WORLD, SERVER_SYNC, WORLD_SYNC -> {
                    if(context.isMainMenu() || context.isSingleplayer()) {
                        yield ActionResult.success();
                    }
                    if(context.isPlayingOnLan()) {
                        if(context.isIntegratedServerOwnedByPlayer()) {
                            yield ActionResult.success();
                        } else {
                            yield ActionResult.fail(Component.translatable("configured.gui.lan_server"));
                        }
                    }
                    if(context.isPlayingOnRemoteServer()) {
                        if(context.isPlayerAnOperator() && context.isDeveloperPlayer()) {
                            yield ActionResult.success();
                        } else {
                            yield ActionResult.fail(Component.translatable("configured.gui.no_developer_status"));
                        }
                    }
                    yield ActionResult.fail(Component.translatable("configured.gui.no_permission"));
                }
            };
        }
        else if(context.isDedicatedServer())
        {
            return switch(this.getType()) {
                case CLIENT, UNIVERSAL, MEMORY, DEDICATED_SERVER -> ActionResult.fail();
                case SERVER, WORLD, SERVER_SYNC, WORLD_SYNC -> {
                    if(context.isPlayerAnOperator() && context.isDeveloperPlayer()) {
                        yield ActionResult.success();
                    }
                    yield ActionResult.fail();
                }
            };
        }
        return ActionResult.fail();
    }

    private void syncToServer()
    {
        if(Services.PLATFORM.getEnvironment() != Environment.CLIENT)
            return;

        if(this.config.getConfig() == null)
            return;

        if(!ConfigHelper.isPlayingGame())
            return;

        if(!ConfigHelper.isConfiguredInstalledOnServer())
            return;

        if(!this.getType().isServer() || this.getType() == ConfigType.DEDICATED_SERVER)
            return;

        if(this.isReadOnly())
            return;

        Player player = ConfigHelper.getClientPlayer();
        if(!ConfigHelper.isOperator(player) || !ClientSessionData.isDeveloper())
            return;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        TomlFormat.instance().createWriter().write(this.config.getConfig(), stream);
        Services.PLATFORM.sendFrameworkConfigToServer(this.config.getName(), stream.toByteArray());
    }

    public boolean loadDataFromResponse(MessageFramework.Response message)
    {
        return this.config.loadFromData(message.data());
    }

    public static class PropertyMap implements FrameworkConfigManager.IMapEntry
    {
        private final Map<String, FrameworkConfigManager.IMapEntry> map = new HashMap<>();

        private final FrameworkConfigManager.FrameworkConfigImpl config;
        private final List<String> path;

        private PropertyMap(FrameworkConfigManager.FrameworkConfigImpl config, List<String> path)
        {
            this.config = config;
            this.path = path;
        }

        private PropertyMap(FrameworkConfigManager.FrameworkConfigImpl config)
        {
            this.config = config;
            this.path = new ArrayList<>();
            config.getAllProperties().forEach(p ->
            {
                PropertyMap current = this;
                List<String> path = p.getPath();
                for(int i = 0; i < path.size() - 1; i++)
                {
                    int finalI = i + 1;
                    current = (PropertyMap) current.map.computeIfAbsent(path.get(i), s -> {
                        return new PropertyMap(config, path.subList(0, finalI));
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

        public List<AbstractProperty<?>> getConfigProperties()
        {
            List<AbstractProperty<?>> properties = new ArrayList<>();
            this.map.forEach((name, entry) ->
            {
                if(entry instanceof AbstractProperty<?> property)
                {
                    properties.add(property);
                }
            });
            return properties;
        }

        @Nullable
        public String getComment()
        {
            if(this.path != null && !this.path.isEmpty())
            {
                return this.config.getComments().getComment(this.path);
            }
            return null;
        }

        public List<String> getPath()
        {
            return this.path;
        }

        public String getTranslationKey()
        {
            if(this.path == null || this.path.isEmpty())
            {
                return String.format("config.%s.%s", this.config.getName().getNamespace(), this.config.getName().getPath());
            }
            return String.format("config.%s.%s.%s", this.config.getName().getNamespace(), this.config.getName().getPath(), StringUtils.join(this.path, '.'));
        }
    }
}
