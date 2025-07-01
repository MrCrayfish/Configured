package com.mrcrayfish.configured.impl.neoforge;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.concurrent.SynchronizedConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import com.google.common.base.Suppliers;
import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.ExecutionContext;
import com.mrcrayfish.configured.api.ActionResult;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.client.ClientSessionData;
import com.mrcrayfish.configured.network.payload.SyncNeoForgeConfigPayload;
import com.mrcrayfish.configured.util.ConfigHelper;
import com.mrcrayfish.configured.util.NeoForgeConfigHelper;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public class NeoForgeConfig implements IModConfig
{
    protected static final EnumMap<ModConfig.Type, ConfigType> TYPE_RESOLVER = Util.make(new EnumMap<>(ModConfig.Type.class), (map) -> {
        map.put(ModConfig.Type.CLIENT, ConfigType.CLIENT);
        map.put(ModConfig.Type.COMMON, ConfigType.UNIVERSAL);
        map.put(ModConfig.Type.SERVER, ConfigType.WORLD_SYNC);
        map.put(ModConfig.Type.STARTUP, ConfigType.UNIVERSAL);
    });

    protected final ModConfig config;
    protected final Supplier<List<ForgeValueEntry>> allConfigValues;

    public NeoForgeConfig(ModConfig config)
    {
        this.config = config;
        this.allConfigValues = Suppliers.memoize(() -> this.getAllConfigValues(config));
    }

    @Override
    public ActionResult update(IConfigEntry entry)
    {
        CommentedConfig origConfig = NeoForgeConfigHelper.getConfigData(this.config);
        if(origConfig == null)
        {
            Constants.LOG.error("Unable to update config '{}' as it is not loaded", this.config.getFileName());
            return ActionResult.fail(Component.translatable("configured.gui.update_error.unloaded"));
        }

        Set<IConfigValue<?>> changedValues = ConfigHelper.getChangedValues(entry);
        if(!changedValues.isEmpty())
        {
            SynchronizedConfig newConfig = new SynchronizedConfig(TomlFormat.instance(), LinkedHashMap::new);
            changedValues.forEach(value -> {
                if(value instanceof NeoForgeValue<?> forge) {
                    if(forge instanceof NeoForgeListValue<?> forgeList) {
                        List<?> converted = forgeList.getConverted();
                        if(converted != null) {
                            newConfig.set(forge.configValue.getPath(), converted);
                            return;
                        }
                    }
                    newConfig.set(forge.configValue.getPath(), value.get());
                }
            });
            origConfig.putAll(newConfig);
            NeoForgeConfigHelper.correctConfig(this.config, origConfig);
        }

        if(this.getType() == ConfigType.WORLD_SYNC && !ConfigHelper.isSingleplayer())
        {
            if(!ConfigHelper.isPlayingGame())
            {
                // Unload server configs since still in main menu
                NeoForgeConfigHelper.saveConfig(this.config);
                NeoForgeConfigHelper.closeConfig(this.config);
            }
            else
            {
                this.syncToServer();
            }
        }
        else if(!changedValues.isEmpty())
        {
            Constants.LOG.info("Saving config and sending reloading event for {}", this.config.getFileName());
            NeoForgeConfigHelper.resetConfigCache(this.config);
            NeoForgeConfigHelper.saveConfig(this.config); // NeoForge saving method also sends reload event
        }
        return ActionResult.success();
    }

    @Override
    public IConfigEntry createRootEntry()
    {
        return new NeoForgeFolderEntry(((ModConfigSpec) this.config.getSpec()).getValues(), (ModConfigSpec) this.config.getSpec());
    }

    @Override
    public ConfigType getType()
    {
        return TYPE_RESOLVER.get(this.config.getType());
    }

    @Override
    public String getFileName()
    {
        return this.config.getFileName();
    }

    @Override
    public String getModId()
    {
        return this.config.getModId();
    }

    @Override
    public ActionResult loadWorldConfig(Path path)
    {
        if(this.config.getLoadedConfig() == null)
        {
            try
            {
                NeoForgeConfigHelper.openConfig(this.config, path.toAbsolutePath());
                if(this.config.getLoadedConfig() != null)
                {
                    return ActionResult.success();
                }
                return ActionResult.fail(); // TODO add message
            }
            catch(Exception e)
            {
                return ActionResult.fail(Component.literal(e.getMessage()));
            }
        }
        return ActionResult.success();
    }

    @Override
    public void stopEditing(boolean updated)
    {
        // Attempts to unload the server config if player simply just went back
        if(this.config != null && this.getType() == ConfigType.WORLD_SYNC)
        {
            if(!ConfigHelper.isPlayingGame())
            {
                // Unload server configs since still in main menu
                NeoForgeConfigHelper.closeConfig(this.config);
            }
        }
    }

    @Override
    public boolean isChanged()
    {
        // TODO test

        // Check if config is loaded
        CommentedConfig data = NeoForgeConfigHelper.getConfigData(this.config);
        if(data == null)
            return false;

        // Check if any config value doesn't equal it's default
        return this.allConfigValues.get().stream().anyMatch(entry -> {
            return !Objects.equals(entry.value.get(), entry.spec.getDefault());
        });
    }

    @Override
    public Optional<Runnable> restoreDefaultsTask()
    {
        if(this.config.getType() == ModConfig.Type.SERVER && ConfigHelper.isPlayingOnRemoteServer())
            return Optional.empty();

        return Optional.ofNullable(NeoForgeConfigHelper.getConfigData(this.config)).map(data -> () -> {
            // Creates a copy of the config data then pushes all at once to avoid multiple IO ops
            CommentedConfig newConfig = CommentedConfig.copy(data);
            this.allConfigValues.get().forEach(entry -> newConfig.set(entry.value.getPath(), entry.spec.getDefault()));
            data.putAll(newConfig);
            // Finally clear cache of all config values
            this.allConfigValues.get().forEach(pair -> pair.value.clearCache());
        });
    }

    @Override
    public ActionResult canPlayerEdit(@Nullable Player player)
    {
        ExecutionContext context = new ExecutionContext(player);
        if(context.isClient())
        {
            return switch(this.config.getType()) {
                case CLIENT, COMMON, STARTUP -> {
                    if(context.isMainMenu() || context.isLocalPlayer()) {
                        yield ActionResult.success();
                    }
                    yield ActionResult.fail();
                }
                case SERVER -> {
                    if(context.isMainMenu() || context.isSingleplayer()) {
                        yield ActionResult.success();
                    }
                    if(context.isPlayingOnLan()) {
                        if(context.isIntegratedServerOwnedByPlayer()) {
                            yield ActionResult.fail(Component.translatable("configured.gui.no_editing_published_lan_server"));
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
                    yield ActionResult.fail();
                }
            };
        }
        else if(context.isDedicatedServer())
        {
            return switch(this.config.getType()) {
                case CLIENT, COMMON, STARTUP -> ActionResult.fail();
                case SERVER -> {
                    if(context.isPlayerAnOperator() && context.isDeveloperPlayer()) {
                        yield ActionResult.success();
                    }
                    yield ActionResult.fail();
                }
            };
        }
        return ActionResult.fail();
    }

    @Override
    public ActionResult showSaveConfirmation(Player player)
    {
        ExecutionContext context = new ExecutionContext(player);
        if(context.isClient() && context.isPlayingOnRemoteServer())
        {
            if(this.config.getType() == ModConfig.Type.SERVER)
            {
                return ActionResult.success(Component.translatable("configured.gui.neoforge.players_kicked"));
            }
        }
        return ActionResult.fail();
    }

    private void syncToServer()
    {
        if(this.config == null)
            return;

        CommentedConfig data = NeoForgeConfigHelper.getConfigData(this.config);
        if(data == null)
            return;

        // Don't need to sync since singleplayer
        if(ConfigHelper.isSingleplayer() || ConfigHelper.isPlayingLan())
            return;

        if(!ConfigHelper.isPlayingGame())
            return;

        if(!ConfigHelper.isConfiguredInstalledOnServer())
            return;

        if(this.getType() != ConfigType.WORLD_SYNC) // Forge only supports this type
            return;

        // This is checked on server too
        Player player = ConfigHelper.getClientPlayer();
        if(!ConfigHelper.isOperator(player) || !ClientSessionData.isDeveloper())
            return;

        try
        {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            TomlFormat.instance().createWriter().write(data, stream);
            ClientPacketDistributor.sendToServer(new SyncNeoForgeConfigPayload(this.config.getFileName(), stream.toByteArray()));
            stream.close();
        }
        catch(IOException e)
        {
            Constants.LOG.error("Failed to close byte stream when sending config to server");
        }
    }

    protected List<ForgeValueEntry> getAllConfigValues(ModConfig config)
    {
        return NeoForgeConfigHelper.gatherAllConfigValues(config).stream().map(pair -> new ForgeValueEntry(pair.getLeft(), pair.getRight())).toList();
    }

    protected record ForgeValueEntry(ModConfigSpec.ConfigValue<?> value, ModConfigSpec.ValueSpec spec) {}
}
