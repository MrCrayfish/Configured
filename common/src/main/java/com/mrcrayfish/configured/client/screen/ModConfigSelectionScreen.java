package com.mrcrayfish.configured.client.screen;

import com.google.common.collect.ImmutableList;
import com.mrcrayfish.configured.api.*;
import com.mrcrayfish.configured.client.ClientConfigHelper;
import com.mrcrayfish.configured.client.screen.widget.IconButton;
import com.mrcrayfish.configured.client.util.ScreenUtil;
import com.mrcrayfish.configured.util.ConfigHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.*;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public class ModConfigSelectionScreen extends ListMenuScreen
{
    private final Map<ConfigType, Set<IModConfig>> configMap;

    public ModConfigSelectionScreen(Screen parent, Component title, Map<ConfigType, Set<IModConfig>> configMap)
    {
        super(parent, title, 30);
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
            localConfigs.forEach(config -> localEntries.add(new FileItem(this, config)));
            Collections.sort(localEntries);
            entries.addAll(localEntries);
        }

        Player player = Minecraft.getInstance().player;
        ExecutionContext context = new ExecutionContext(player);
        Set<IModConfig> remoteConfigs = this.getRemoteConfigs();
        if(!remoteConfigs.isEmpty() && (context.isMainMenu() || context.isConfiguredInstalledRemotely()))
        {
            if(context.isPlayingGame() && context.isPlayingOnRemoteServer())
            {
                if(context.isPlayingOnLan() && !context.isIntegratedServerOwnedByPlayer())
                {
                    entries.add(new TitleItem(Component.translatable("configured.gui.title.server_configuration").getString()));
                    entries.add(new TitleItem(Component.translatable("configured.gui.lan_server")));
                    return;
                }

                // Don't show developer hint if not at least an operator
                if(!context.isPlayerAnOperator())
                    return;

                if(!context.isDeveloperPlayer())
                {
                    entries.add(new TitleItem(Component.translatable("configured.gui.title.server_configuration").getString()));
                    entries.add(new MultiTextItem(
                            Component.translatable("configured.gui.no_developer_status"),
                            Component.translatable("configured.gui.developer_details", Component.literal("configured.developer.toml").withStyle(ChatFormatting.GOLD).withStyle(Style.EMPTY.withUnderlined(true))).withStyle(ChatFormatting.GRAY).withStyle(style -> {
                                return style.withHoverEvent(new HoverEvent.ShowText(Component.translatable("configured.gui.developer_file")));
                            })));
                    return;
                }
            }

            entries.add(new TitleItem(Component.translatable("configured.gui.title.server_configuration").getString()));
            List<Item> remoteEntries = new ArrayList<>();
            remoteConfigs.forEach(config -> remoteEntries.add(new FileItem(this, config)));
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

    public class FileItem extends Item implements EntryBackground
    {
        protected final TooltipScreen screen;
        protected final IModConfig config;
        protected final Component title;
        protected final Component fileName;
        protected final Component modifyTooltip;
        protected final Button modifyButton;
        @Nullable
        protected final Button restoreButton;

        public FileItem(TooltipScreen screen, IModConfig config)
        {
            super(createLabelFromModConfig(config));
            this.screen = screen;
            this.config = config;
            this.title = this.createTrimmedFileName(createLabelFromModConfig(config));
            this.fileName = this.createTrimmedFileName(config.getFileName()).withStyle(ChatFormatting.DARK_GRAY);
            this.modifyButton = this.createModifyButton(config);
            ActionResult result = config.canPlayerEdit(Minecraft.getInstance().player);
            this.modifyButton.active = result.asBoolean();
            this.modifyTooltip = result.message().orElse(Component.translatable("configured.gui.no_permission"));
            this.restoreButton = this.createRestoreButton(config);
            this.updateRestoreDefaultButton();
        }

        private void showRestoreScreen()
        {
            ConfirmationScreen confirmScreen = new ConfirmationScreen(ModConfigSelectionScreen.this, Component.translatable("configured.gui.restore_message"), ConfirmationScreen.Icon.WARNING, result -> {
                if(!result)
                    return true;
                this.config.restoreDefaultsTask().ifPresent(Runnable::run);
                this.updateRestoreDefaultButton();
                return true;
            });
            confirmScreen.setPositiveText(Component.translatable("configured.gui.restore").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            confirmScreen.setNegativeText(CommonComponents.GUI_CANCEL);
            Minecraft.getInstance().setScreen(confirmScreen);
        }

        private MutableComponent createTrimmedFileName(String fileName)
        {
            MutableComponent trimmedFileName = Component.literal(fileName);
            if(Minecraft.getInstance().font.width(fileName) > 150)
            {
                trimmedFileName = Component.literal(Minecraft.getInstance().font.plainSubstrByWidth(fileName, 140).trim() + "...");
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
            int width = ConfigHelper.canRestoreConfig(config, Minecraft.getInstance().player) ? 60 : 82;
            return new IconButton(0, 0, this.getModifyIconU(config), this.getModifyIconV(config), width, this.getModifyLabel(config), button ->
            {
                // Disable all handling if not active or visible
                if(!button.isActive() || !button.visible)
                    return;

                // Check if the player can edit the config
                if(!config.canPlayerEdit(Minecraft.getInstance().player).asBoolean())
                    return;

                // We can never edit dedicated server type configs
                if(config.getType() == ConfigType.DEDICATED_SERVER)
                    return;

                // If you're at the main menu, not loaded into a world
                ExecutionContext context = new ExecutionContext(Minecraft.getInstance().player);
                if(context.isMainMenu())
                {
                    if(config.getType().isWorld())
                    {
                        Minecraft.getInstance().setScreen(new WorldSelectionScreen(ModConfigSelectionScreen.this, config, this.title));
                        return;
                    }
                    else if(config.getType().isServer())
                    {
                        Component newTitle = ModConfigSelectionScreen.this.title.copy().append(Component.literal(" > ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)).append(this.title);
                        Minecraft.getInstance().setScreen(new ConfigScreen(ModConfigSelectionScreen.this, newTitle, config));
                        return;
                    }
                }

                // If you're playing on a dedicated server and the config is a non-sync server type
                if(context.isPlayingOnRemoteServer() && context.isConfiguredInstalledRemotely() && config.requestFromServerTask().isPresent() && context.isPlayerAnOperator() && context.isDeveloperPlayer())
                {
                    Component newTitle = ModConfigSelectionScreen.this.title.copy().append(Component.literal(" > ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)).append(this.title);
                    Minecraft.getInstance().setScreen(new RequestScreen(ModConfigSelectionScreen.this, newTitle, config));
                    return;
                }

                // Handle all remaining cases
                Component newTitle = ModConfigSelectionScreen.this.title.copy().append(Component.literal(" > ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)).append(this.title);
                Minecraft.getInstance().setScreen(new ConfigScreen(ModConfigSelectionScreen.this, newTitle, config));
            });
        }

        private int getModifyIconU(IModConfig config)
        {
            if(!ConfigHelper.isPlayingGame())
            {
                if(config.getType().isWorld())
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
                if(config.isReadOnly())
                {
                    return 33;
                }
            }
            else
            {
                if(config.isReadOnly() && !config.getType().isWorld())
                {
                    return 33;
                }
            }
            return 22;
        }

        private Component getModifyLabel(IModConfig config)
        {
            if(ClientConfigHelper.isMainMenu() && config.getType().isWorld())
            {
                return Component.translatable("configured.gui.select_world");
            }
            if(config.isReadOnly())
            {
                return Component.translatable("configured.gui.view");
            }
            return Component.translatable("configured.gui.modify");
        }

        private Button createRestoreButton(IModConfig config)
        {
            if(ConfigHelper.canRestoreConfig(config, Minecraft.getInstance().player))
            {
                IconButton restoreButton = new IconButton(0, 0, 0, 0, onPress -> this.showRestoreScreen());
                restoreButton.active = !config.isReadOnly() && config.isChanged();
                return restoreButton;
            }
            return null;
        }

        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovered, float partialTick)
        {
            graphics.drawString(Minecraft.getInstance().font, this.title, this.getX() + 26, this.getY() + 6, 0xFFFFFFFF);
            graphics.drawString(Minecraft.getInstance().font, this.fileName, this.getX() + 26, this.getY() + 16, 0xFFFFFFFF);
            graphics.blit(RenderPipelines.GUI_TEXTURED, IconButton.ICONS, this.getX() + 2, this.getY() + 4, this.getIconU(), this.getIconV(), 18, 22, 9, 11, 64, 64);

            if(this.config.isReadOnly())
            {
                graphics.blit(RenderPipelines.GUI_TEXTURED, IconButton.ICONS, this.getX() - 2, this.getY() + 19, 0, 33, 11, 11, 11, 11, 64, 64);
            }

            this.modifyButton.setX(this.getX() + this.getWidth() - 83);
            this.modifyButton.setY(this.getY() + 5);
            this.modifyButton.render(graphics, mouseX, mouseY, partialTick);

            if(this.restoreButton != null)
            {
                this.restoreButton.setX(this.getX() + this.getWidth() - 21);
                this.restoreButton.setY(this.getY() + 5);
                this.restoreButton.render(graphics, mouseX, mouseY, partialTick);
            }

            if(this.config.isReadOnly() && ScreenUtil.isMouseWithin(this.getX() - 2, this.getY() + 19, 11, 11, mouseX, mouseY))
            {
                ModConfigSelectionScreen.this.setActiveTooltip(graphics, Component.translatable("configured.gui.read_only_config"), mouseX, mouseY, TooltipStyle.HINT);
            }

            if(!this.modifyButton.active && this.modifyButton.isHoveredOrFocused())
            {
                this.screen.setActiveTooltip(graphics, this.modifyTooltip, mouseX, mouseY, TooltipStyle.ERROR);
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
            if(this.config != null && this.restoreButton != null && ConfigHelper.canRestoreConfig(this.config, Minecraft.getInstance().player))
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

    public static boolean isRunningUnpublishedLan()
    {
        return Minecraft.getInstance().getSingleplayerServer() != null && !Minecraft.getInstance().getSingleplayerServer().isPublished();
    }
}
