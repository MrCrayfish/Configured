package com.mrcrayfish.configured.client.screen;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.client.SessionData;
import com.mrcrayfish.configured.client.screen.widget.IconButton;
import com.mrcrayfish.configured.client.util.ScreenUtil;
import com.mrcrayfish.configured.platform.Services;
import com.mrcrayfish.configured.util.ConfigHelper;
import com.mrcrayfish.framework.api.Environment;
import com.mrcrayfish.framework.api.util.EnvironmentHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.io.FilenameUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public class ModConfigSelectionScreen extends ListMenuScreen
{
    private final Map<ConfigType, Set<IModConfig>> configMap;

    public ModConfigSelectionScreen(Screen parent, Component title, ResourceLocation background, Map<ConfigType, Set<IModConfig>> configMap)
    {
        super(parent, title, background, 30);
        this.configMap = configMap;
    }

    @Override
    protected void constructEntries(List<Item> entries)
    {
        Set<IModConfig> localConfigs = this.getLocalConfigs();
        if(!localConfigs.isEmpty())
        {
            entries.add(new TitleItem(Component.translatable("configured.gui.title.client_configuration").getString()));
            List<Item> localEntries = new ArrayList<>();
            localConfigs.forEach(config -> localEntries.add(new FileItem(config)));
            Collections.sort(localEntries);
            entries.addAll(localEntries);
        }

        Set<IModConfig> remoteConfigs = this.getRemoteConfigs();
        if(!remoteConfigs.isEmpty() && (!ConfigHelper.isPlayingGame() || ConfigHelper.isConfiguredInstalledOnServer()))
        {
            Player player = Minecraft.getInstance().player;
            if(ConfigHelper.isPlayingGame() && ConfigHelper.isPlayingRemotely())
            {
                if(SessionData.isLan())
                {
                    entries.add(new TitleItem(Component.translatable("configured.gui.title.server_configuration").getString()));
                    entries.add(new TitleItem(Component.translatable("configured.gui.lan_server")));
                    return;
                }

                if(!ConfigHelper.isOperator(player))
                    return;

                if(!SessionData.isDeveloper(player))
                {
                    entries.add(new TitleItem(Component.translatable("configured.gui.title.server_configuration").getString()));
                    entries.add(new MultiTextItem(
                            Component.translatable("configured.gui.no_developer_status"),
                            Component.translatable("configured.gui.developer_details", Component.literal("configured.developer.toml").withStyle(ChatFormatting.GOLD).withStyle(Style.EMPTY.withUnderlined(true))).withStyle(ChatFormatting.GRAY).withStyle(style -> {
                                return style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("configured.gui.developer_file")));
                            })));
                    return;
                }
            }

            entries.add(new TitleItem(Component.translatable("configured.gui.title.server_configuration").getString()));
            List<Item> remoteEntries = new ArrayList<>();
            remoteConfigs.forEach(config -> remoteEntries.add(new FileItem(config)));
            Collections.sort(remoteEntries);
            entries.addAll(remoteEntries);
        }
    }

    @Override
    protected void init()
    {
        super.init();
        this.addRenderableWidget(ScreenUtil.button(this.width / 2 - 75, this.height - 29, 150, 20, CommonComponents.GUI_BACK, button -> this.minecraft.setScreen(this.parent)));
    }

    private Set<IModConfig> getLocalConfigs()
    {
        return this.configMap.entrySet().stream().filter(entry -> {
            return !entry.getKey().isServer();
        }).flatMap(entry -> entry.getValue().stream()).collect(Collectors.toSet());
    }

    private Set<IModConfig> getRemoteConfigs()
    {
        return this.configMap.entrySet().stream().filter(entry -> {
            ConfigType type = entry.getKey();
            return type.isServer() && type.getEnv().orElse(null) != Environment.DEDICATED_SERVER;
        }).flatMap(entry -> entry.getValue().stream()).collect(Collectors.toSet());
    }

    public class FileItem extends Item
    {
        protected final IModConfig config;
        protected final Component title;
        protected final Component fileName;
        protected final Button modifyButton;
        @Nullable
        protected final Button restoreButton;

        public FileItem(IModConfig config)
        {
            super(createLabelFromModConfig(config));
            this.config = config;
            this.title = this.createTrimmedFileName(createLabelFromModConfig(config));
            this.fileName = this.createTrimmedFileName(config.getFileName()).withStyle(ChatFormatting.DARK_GRAY);
            this.modifyButton = this.createModifyButton(config);
            this.modifyButton.active = canEditConfig(Minecraft.getInstance().player, config);
            this.restoreButton = this.createRestoreButton(config);
            this.updateRestoreDefaultButton();
        }

        private void showRestoreScreen()
        {
            ConfirmationScreen confirmScreen = new ConfirmationScreen(ModConfigSelectionScreen.this, Component.translatable("configured.gui.restore_message"), ConfirmationScreen.Icon.WARNING, result -> {
                if(!result)
                    return true;
                this.config.restoreDefaults();
                this.updateRestoreDefaultButton();
                return true;
            });
            confirmScreen.setBackground(ModConfigSelectionScreen.this.background);
            confirmScreen.setPositiveText(Component.translatable("configured.gui.restore").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            confirmScreen.setNegativeText(CommonComponents.GUI_CANCEL);
            Minecraft.getInstance().setScreen(confirmScreen);
        }

        private MutableComponent createTrimmedFileName(String fileName)
        {
            MutableComponent trimmedFileName = Component.literal(fileName);
            if(Minecraft.getInstance().font.width(fileName) > 150)
            {
                trimmedFileName = Component.literal(Minecraft.getInstance().font.plainSubstrByWidth(fileName, 140) + "...");
            }
            return trimmedFileName;
        }

        /**
         * Creates and returns a new modify button instance. Since server configurations are handled
         * different, the label and click handler of this button is different if the given ModConfig
         * instance is of the server type.
         *
         * @param config
         * @return
         */
        private Button createModifyButton(IModConfig config)
        {
            int width = canRestoreConfig(Minecraft.getInstance().player, config) ? 60 : 82;
            return new IconButton(0, 0, this.getModifyIconU(config), this.getModifyIconV(config), width, this.getModifyLabel(config), button ->
            {
                if(!button.isActive() || !button.visible)
                    return;

                if(!ConfigHelper.isPlayingGame())
                {
                    if(ConfigHelper.isWorldConfig(config))
                    {
                        Minecraft.getInstance().setScreen(new WorldSelectionScreen(ModConfigSelectionScreen.this, ModConfigSelectionScreen.this.background, config, this.title));
                    }
                    else if(config.getType() != ConfigType.DEDICATED_SERVER)
                    {
                        Component newTitle = ModConfigSelectionScreen.this.title.copy().append(Component.literal(" > ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)).append(this.title);
                        Minecraft.getInstance().setScreen(new ConfigScreen(ModConfigSelectionScreen.this, newTitle, config, ModConfigSelectionScreen.this.background));
                    }
                }
                else if(ConfigHelper.isPlayingRemotely() && config.getType().isServer() && !config.getType().isSync())
                {
                    if(Services.PLATFORM.isModLoaded(config.getModId()))
                    {
                        Component newTitle = ModConfigSelectionScreen.this.title.copy().append(Component.literal(" > ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)).append(this.title);
                        Minecraft.getInstance().setScreen(new RequestScreen(ModConfigSelectionScreen.this, newTitle, ModConfigSelectionScreen.this.background, config));
                    }
                }
                else
                {
                    if(Services.PLATFORM.isModLoaded(config.getModId()))
                    {
                        Component newTitle = ModConfigSelectionScreen.this.title.copy().append(Component.literal(" > ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)).append(this.title);
                        Minecraft.getInstance().setScreen(new ConfigScreen(ModConfigSelectionScreen.this, newTitle, config, ModConfigSelectionScreen.this.background));
                    }
                }
            });
        }

        private int getModifyIconU(IModConfig config)
        {
            if(ConfigHelper.isPlayingGame())
            {
                if(ConfigHelper.isPlayingRemotely() && config.getType().isServer() && !config.getType().isSync())
                {
                    return 22;
                }
            }
            else
            {
                if(ConfigHelper.isWorldConfig(config))
                {
                    return 11;
                }
            }
            return 0;
        }

        private int getModifyIconV(IModConfig config)
        {
            if(ConfigHelper.isPlayingGame())
            {
                if(ConfigHelper.isPlayingRemotely() && config.getType().isServer() && !config.getType().isSync())
                {
                    return 22;
                }
                if(config.isReadOnly())
                {
                    return 33;
                }
            }
            else
            {
                if(config.isReadOnly() && !ConfigHelper.isWorldConfig(config))
                {
                    return 33;
                }
            }
            return 22;
        }

        private Component getModifyLabel(IModConfig config)
        {
            if(!ConfigHelper.isPlayingGame() && ConfigHelper.isWorldConfig(config))
            {
                return Component.translatable("configured.gui.select_world");
            }
            if(ConfigHelper.isPlayingGame() && ConfigHelper.isPlayingRemotely() && config.getType().isServer() && !config.getType().isSync() && config.getType() != ConfigType.DEDICATED_SERVER)
            {
                return Component.translatable("configured.gui.request");
            }
            if(config.isReadOnly())
            {
                return Component.translatable("configured.gui.view");
            }
            return Component.translatable("configured.gui.modify");
        }

        private Button createRestoreButton(IModConfig config)
        {
            if(canRestoreConfig(Minecraft.getInstance().player, config))
            {
                IconButton restoreButton = new IconButton(0, 0, 0, 0, onPress -> this.showRestoreScreen());
                restoreButton.active = !config.isReadOnly() && ConfigHelper.hasPermissionToEdit(Minecraft.getInstance().player, config);
                return restoreButton;
            }
            return null;
        }

        @Override
        public void render(GuiGraphics poseStack, int x, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean p_230432_9_, float partialTicks)
        {
            poseStack.drawString(Minecraft.getInstance().font, this.title, left + 28, top + 2, 0xFFFFFF);
            poseStack.drawString(Minecraft.getInstance().font, this.fileName, left + 28, top + 12, 0xFFFFFF);
            RenderSystem.setShaderTexture(0, IconButton.ICONS);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.blit(IconButton.ICONS, left + 4, top, 18, 22, this.getIconU(), this.getIconV(), 9, 11, 64, 64);

            if(this.config.isReadOnly())
            {
                poseStack.blit(IconButton.ICONS, left + 1, top + 15, 11, 11, 0, 33, 11, 11, 64, 64);
            }

            this.modifyButton.setX(left + width - 83);
            this.modifyButton.setY(top);
            this.modifyButton.render(poseStack, mouseX, mouseY, partialTicks);

            if(this.restoreButton != null)
            {
                this.restoreButton.setX(left + width - 21);
                this.restoreButton.setY(top);
                this.restoreButton.render(poseStack, mouseX, mouseY, partialTicks);
            }

            if(this.config.isReadOnly() && ScreenUtil.isMouseWithin(left - 1, top + 15, 11, 11, mouseX, mouseY))
            {
                ModConfigSelectionScreen.this.setActiveTooltip(Component.translatable("configured.gui.read_only_config"), 0xFF1E6566);
            }
        }

        private int getIconU()
        {
            return (this.config.getType().ordinal() % 3) * 9 + 33;
        }

        private int getIconV()
        {
            return (this.config.getType().ordinal() / 3) * 11;
        }

        @Override
        public List<? extends GuiEventListener> children()
        {
            if(this.restoreButton != null)
            {
                return ImmutableList.of(this.modifyButton, this.restoreButton);
            }
            return ImmutableList.of(this.modifyButton);
        }

        /**
         * Updates the active state of the restore default button. It will only be active if values are
         * different from their default.
         */
        private void updateRestoreDefaultButton()
        {
            if(this.config != null && this.restoreButton != null && canRestoreConfig(Minecraft.getInstance().player, this.config))
            {
                this.restoreButton.active = !this.config.isReadOnly() && this.config.isChanged();
            }
        }
    }

    /**
     * Tries to create a readable label from the file name of the given mod config.
     *
     * @param config
     * @return
     */
    public static String createLabelFromModConfig(IModConfig config)
    {
        if(config.getTranslationKey() != null) {
            return I18n.get(config.getTranslationKey());
        }
        String fileName = config.getFileName();
        fileName = fileName.replace(config.getModId() + "-", "");
        if(fileName.endsWith(".toml")) {
            fileName = fileName.substring(0, fileName.length() - ".toml".length());
        }
        fileName = FilenameUtils.getName(fileName);
        fileName = ConfigScreen.createLabel(fileName);
        return fileName;
    }

    public static boolean canEditConfig(@Nullable Player player, IModConfig config)
    {
        return switch(config.getType())
        {
            case CLIENT -> EnvironmentHelper.getEnvironment() == Environment.CLIENT;
            case UNIVERSAL, MEMORY -> true;
            case SERVER, WORLD, SERVER_SYNC, WORLD_SYNC -> !ConfigHelper.isPlayingGame() || ConfigHelper.isRunningLocalServer() || ConfigHelper.isOperator(player) && SessionData.isDeveloper(player);
            case DEDICATED_SERVER -> false;
        };
    }

    public static boolean canRestoreConfig(Player player, IModConfig config)
    {
        return switch(config.getType())
        {
            case CLIENT, UNIVERSAL, MEMORY -> true;
            case SERVER, SERVER_SYNC -> !ConfigHelper.isPlayingGame() || ConfigHelper.isRunningLocalServer();
            case WORLD, WORLD_SYNC -> ConfigHelper.isRunningLocalServer();
            case DEDICATED_SERVER -> false;
        };
    }
}
