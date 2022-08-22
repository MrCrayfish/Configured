package com.mrcrayfish.configured.client.screen;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mrcrayfish.configured.client.screen.widget.IconButton;
import com.mrcrayfish.configured.util.ConfigHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Author: MrCrayfish
 */
public class ModConfigSelectionScreen extends ListMenuScreen
{
    private final Map<ModConfig.Type, Set<ModConfig>> configMap;

    public ModConfigSelectionScreen(Screen parent, String displayName, ResourceLocation background, Map<ModConfig.Type, Set<ModConfig>> configMap)
    {
        super(parent, new TextComponent(displayName), background, 30);
        this.configMap = configMap;
    }

    @Override
    protected void constructEntries(List<Item> entries)
    {
        Set<ModConfig> clientConfigs = this.configMap.get(ModConfig.Type.CLIENT);
        if(clientConfigs != null)
        {
            entries.add(new TitleItem(new TranslatableComponent("configured.gui.title.client_configuration").getString()));
            clientConfigs.forEach(config ->
            {
                entries.add(new FileItem(config));
            });
        }
        Set<ModConfig> commonConfigs = this.configMap.get(ModConfig.Type.COMMON);
        if(commonConfigs != null)
        {
            entries.add(new TitleItem(new TranslatableComponent("configured.gui.title.common_configuration").getString()));
            commonConfigs.forEach(config ->
            {
                entries.add(new FileItem(config));
            });
        }
        Set<ModConfig> serverConfigs = this.configMap.get(ModConfig.Type.SERVER);
        if(serverConfigs != null)
        {
            entries.add(new TitleItem(new TranslatableComponent("configured.gui.title.server_configuration").getString()));
            if(isPlayingGame())
            {
                entries.add(new SubTitleItem(new TranslatableComponent("configured.gui.server_config_main_menu")));
            }
            else
            {
                serverConfigs.forEach(config -> {
                    entries.add(new FileItem(config));
                });
            }
        }
    }

    @Override
    protected void init()
    {
        super.init();
        this.addRenderableWidget(new Button(this.width / 2 - 75, this.height - 29, 150, 20, CommonComponents.GUI_BACK, button -> this.minecraft.setScreen(this.parent)));
    }

    @OnlyIn(Dist.CLIENT)
    public class FileItem extends Item
    {
        protected final ModConfig config;
        protected final Component title;
        protected final Component fileName;
        protected final Button modifyButton;
        @Nullable
        protected final Button restoreButton;
        private final List<Pair<ForgeConfigSpec.ConfigValue<?>, ForgeConfigSpec.ValueSpec>> allConfigValues;

        public FileItem(ModConfig config)
        {
            super(createLabelFromModConfig(config));
            this.config = config;
            this.title = this.createTrimmedFileName(createLabelFromModConfig(config));
            this.allConfigValues = ConfigHelper.gatherAllConfigValues(config);
            this.fileName = this.createTrimmedFileName(config.getFileName()).withStyle(ChatFormatting.GRAY);
            this.modifyButton = this.createModifyButton(config);
            this.modifyButton.active = !ConfigScreen.isPlayingGame() || this.config.getType() != ModConfig.Type.SERVER;
            if(config.getType() != ModConfig.Type.SERVER || Minecraft.getInstance().player != null)
            {
                this.restoreButton = new IconButton(0, 0, 0, 0, onPress -> this.showRestoreScreen(), (button, poseStack, mouseX, mouseY) ->
                {
                    if(button.isHovered())
                    {
                        if(this.hasRequiredPermission() && button.active)
                        {
                            ModConfigSelectionScreen.this.renderTooltip(poseStack, Minecraft.getInstance().font.split(new TranslatableComponent("configured.gui.reset_all"), Math.max(ModConfigSelectionScreen.this.width / 2 - 43, 170)), mouseX, mouseY);
                        }
                        else if(!this.hasRequiredPermission())
                        {
                            ModConfigSelectionScreen.this.renderTooltip(poseStack, Minecraft.getInstance().font.split(new TranslatableComponent("configured.gui.no_permission").withStyle(ChatFormatting.RED), Math.max(ModConfigSelectionScreen.this.width / 2 - 43, 170)), mouseX, mouseY);
                        }
                    }
                });
                this.restoreButton.active = this.hasRequiredPermission();
                this.updateRestoreDefaultButton();
            }
            else
            {
                this.restoreButton = null;
            }
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private void showRestoreScreen()
        {
            ConfirmationScreen confirmScreen = new ConfirmationScreen(ModConfigSelectionScreen.this, new TranslatableComponent("configured.gui.restore_message"), result ->
            {
                if(!result || this.allConfigValues == null)
                    return true;

                // Resets all config values
                CommentedConfig newConfig = CommentedConfig.copy(this.config.getConfigData());
                this.allConfigValues.forEach(pair ->
                {
                    ForgeConfigSpec.ConfigValue configValue = pair.getLeft();
                    ForgeConfigSpec.ValueSpec valueSpec = pair.getRight();
                    newConfig.set(configValue.getPath(), valueSpec.getDefault());
                });
                this.updateRestoreDefaultButton();
                this.config.getConfigData().putAll(newConfig);
                ConfigHelper.resetCache(this.config);
                return true;
            });
            confirmScreen.setBackground(background);
            confirmScreen.setPositiveText(new TranslatableComponent("configured.gui.restore").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            confirmScreen.setNegativeText(CommonComponents.GUI_CANCEL);
            Minecraft.getInstance().setScreen(confirmScreen);
        }

        private boolean hasRequiredPermission()
        {
            if(this.config.getType() == ModConfig.Type.SERVER && Minecraft.getInstance().player != null)
            {
                return Minecraft.getInstance().player.hasPermissions(2);
            }
            return true;
        }

        private MutableComponent createTrimmedFileName(String fileName)
        {
            MutableComponent trimmedFileName = new TextComponent(fileName);
            if(Minecraft.getInstance().font.width(fileName) > 150)
            {
                trimmedFileName = new TextComponent(Minecraft.getInstance().font.plainSubstrByWidth(fileName, 140) + "...");
            }
            return trimmedFileName;
        }

        /**
         * Creates and returns a new modify button instance. Since server configurations are handled
         * different, the label and click handler of this button is different if the given ModConfig
         * instance is of the server type.
         * @param config
         * @return
         */
        private Button createModifyButton(ModConfig config)
        {
            boolean serverConfig = config.getType() == ModConfig.Type.SERVER && Minecraft.getInstance().level == null;
            String langKey = serverConfig ? "configured.gui.select_world" : "configured.gui.modify";
            return new IconButton(0, 0, serverConfig ? 44 : 33, 0, serverConfig ? 80 : 60, new TranslatableComponent(langKey), onPress ->
            {
                if(ConfigScreen.isPlayingGame() && this.config.getType() == ModConfig.Type.SERVER)
                    return;

                if(serverConfig)
                {
                    Minecraft.getInstance().setScreen(new WorldSelectionScreen(ModConfigSelectionScreen.this, ModConfigSelectionScreen.this.background, config, this.title));
                }
                else
                {
                    ModList.get().getModContainerById(config.getModId()).ifPresent(container ->
                    {
                        Minecraft.getInstance().setScreen(new ConfigScreen(ModConfigSelectionScreen.this, new TextComponent(container.getModInfo().getDisplayName()), config, ModConfigSelectionScreen.this.background));
                    });
                }
            });
        }

        @Override
        public void render(PoseStack poseStack, int x, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean p_230432_9_, float partialTicks)
        {
            Screen.drawString(poseStack, Minecraft.getInstance().font, this.title, left + 28, top + 2, 0xFFFFFF);
            Screen.drawString(poseStack, Minecraft.getInstance().font, this.fileName, left + 28, top + 12, 0xFFFFFF);
            float brightness = true ? 1.0F : 0.5F;
            RenderSystem.setShaderTexture(0, IconButton.ICONS);
            RenderSystem.setShaderColor(brightness, brightness, brightness, 1.0F);
            blit(poseStack, left + 4, top, 18, 22, this.getIconU(), 11, 9, 11, 64, 64);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            this.modifyButton.x = left + width - 83;
            this.modifyButton.y = top;
            this.modifyButton.render(poseStack, mouseX, mouseY, partialTicks);

            if(this.restoreButton != null)
            {
                this.restoreButton.x = left + width - 21;
                this.restoreButton.y = top;
                this.restoreButton.render(poseStack, mouseX, mouseY, partialTicks);
            }
        }

        private int getIconU()
        {
            switch(this.config.getType())
            {
                case COMMON:
                    return 9;
                case SERVER:
                    return 18;
                default:
                    return 0;
            }
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
            if(this.config != null && this.restoreButton != null && this.hasRequiredPermission())
            {
                this.restoreButton.active = ConfigHelper.isModified(this.config);
            }
        }
    }

    /**
     * Tries to create a readable label from the file name of the given mod config.
     *
     * @param config
     * @return
     */
    private static String createLabelFromModConfig(ModConfig config)
    {
        String fileName = config.getFileName();
        fileName = fileName.replace(config.getModId() + "-", "");
        fileName = fileName.substring(0, fileName.length() - ".toml".length());
        fileName = FilenameUtils.getName(fileName);
        fileName = ConfigScreen.createLabel(fileName);
        return fileName;
    }
}
