package com.mrcrayfish.configured.client.screen;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mrcrayfish.configured.client.screen.widget.IconButton;
import com.mrcrayfish.configured.util.ConfigHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
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
        super(parent, new StringTextComponent(displayName), background, 30);
        this.configMap = configMap;
    }

    @Override
    protected void constructEntries(List<Item> entries)
    {
        Set<ModConfig> clientConfigs = this.configMap.get(ModConfig.Type.CLIENT);
        if(clientConfigs != null)
        {
            entries.add(new TitleItem(new TranslationTextComponent("configured.gui.title.client_configuration").getString()));
            clientConfigs.forEach(config ->
            {
                entries.add(new FileItem(config));
            });
        }
        Set<ModConfig> commonConfigs = this.configMap.get(ModConfig.Type.COMMON);
        if(commonConfigs != null)
        {
            entries.add(new TitleItem(new TranslationTextComponent("configured.gui.title.common_configuration").getString()));
            commonConfigs.forEach(config ->
            {
                entries.add(new FileItem(config));
            });
        }
        Set<ModConfig> serverConfigs = this.configMap.get(ModConfig.Type.SERVER);
        if(serverConfigs != null)
        {
            entries.add(new TitleItem(new TranslationTextComponent("configured.gui.title.server_configuration").getString()));
            serverConfigs.forEach(config ->
            {
                entries.add(new FileItem(config));
            });
        }
    }

    @Override
    protected void init()
    {
        super.init();
        this.addButton(new Button(this.width / 2 - 75, this.height - 29, 150, 20, DialogTexts.GUI_BACK, button -> this.minecraft.displayGuiScreen(this.parent)));
    }

    @OnlyIn(Dist.CLIENT)
    public class FileItem extends Item
    {
        protected final ModConfig config;
        protected final ITextComponent title;
        protected final ITextComponent fileName;
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
            this.fileName = this.createTrimmedFileName(config.getFileName()).mergeStyle(TextFormatting.GRAY);
            this.modifyButton = this.createModifyButton(config);
            this.modifyButton.active = !ConfigScreen.isPlayingGame() || this.config.getType() != ModConfig.Type.SERVER || ConfigHelper.isConfiguredInstalledOnServer() && this.hasRequiredPermission();
            if(config.getType() != ModConfig.Type.SERVER || Minecraft.getInstance().player != null)
            {
                this.restoreButton = new IconButton(0, 0, 0, 0, onPress -> this.showRestoreScreen(), (button, matrixStack, mouseX, mouseY) ->
                {
                    if(button.isHovered())
                    {
                        if(this.hasRequiredPermission() && button.active)
                        {
                            ModConfigSelectionScreen.this.renderTooltip(matrixStack, Minecraft.getInstance().fontRenderer.trimStringToWidth(new TranslationTextComponent("configured.gui.reset_all"), Math.max(ModConfigSelectionScreen.this.width / 2 - 43, 170)), mouseX, mouseY);
                        }
                        else if(!this.hasRequiredPermission())
                        {
                            ModConfigSelectionScreen.this.renderTooltip(matrixStack, Minecraft.getInstance().fontRenderer.trimStringToWidth(new TranslationTextComponent("configured.gui.no_permission").mergeStyle(TextFormatting.RED), Math.max(ModConfigSelectionScreen.this.width / 2 - 43, 170)), mouseX, mouseY);
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
            ConfirmationScreen confirmScreen = new ConfirmationScreen(ModConfigSelectionScreen.this, new TranslationTextComponent("configured.gui.restore_message"), result ->
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

                // Post logic for server configs
                if(this.config.getType() == ModConfig.Type.SERVER)
                {
                    ConfigHelper.sendConfigDataToServer(this.config);
                }
                return true;
            });
            confirmScreen.setBackground(background);
            confirmScreen.setPositiveText(new TranslationTextComponent("configured.gui.restore").mergeStyle(TextFormatting.GOLD, TextFormatting.BOLD));
            confirmScreen.setNegativeText(DialogTexts.GUI_CANCEL);
            Minecraft.getInstance().displayGuiScreen(confirmScreen);
        }

        private boolean hasRequiredPermission()
        {
            if(this.config.getType() == ModConfig.Type.SERVER && Minecraft.getInstance().player != null)
            {
                return Minecraft.getInstance().player.hasPermissionLevel(2);
            }
            return true;
        }

        private StringTextComponent createTrimmedFileName(String fileName)
        {
            StringTextComponent trimmedFileName = new StringTextComponent(fileName);
            if(Minecraft.getInstance().fontRenderer.getStringWidth(fileName) > 150)
            {
                trimmedFileName = new StringTextComponent(Minecraft.getInstance().fontRenderer.func_238412_a_(fileName, 140) + "...");
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
            boolean serverConfig = config.getType() == ModConfig.Type.SERVER && Minecraft.getInstance().world == null;
            String langKey = serverConfig ? "configured.gui.select_world" : "configured.gui.modify";
            return new IconButton(0, 0, serverConfig ? 44 : 33, 0, serverConfig ? 80 : 60, new TranslationTextComponent(langKey), onPress ->
            {
                if(ConfigScreen.isPlayingGame() && this.config.getType() == ModConfig.Type.SERVER && (!ConfigHelper.isConfiguredInstalledOnServer() || !this.hasRequiredPermission()))
                    return;

                if(serverConfig)
                {
                    Minecraft.getInstance().displayGuiScreen(new WorldSelectionScreen(ModConfigSelectionScreen.this, ModConfigSelectionScreen.this.background, config, this.title));
                }
                else
                {
                    ModList.get().getModContainerById(config.getModId()).ifPresent(container ->
                    {
                        Minecraft.getInstance().displayGuiScreen(new ConfigScreen(ModConfigSelectionScreen.this, new StringTextComponent(container.getModInfo().getDisplayName()), config, ModConfigSelectionScreen.this.background));
                    });
                }
            }, (button, matrixStack, mouseX, mouseY) ->
            {
                if(button.isHovered())
                {
                    if(ConfigScreen.isPlayingGame() && !ConfigHelper.isConfiguredInstalledOnServer())
                    {
                        ModConfigSelectionScreen.this.renderTooltip(matrixStack, Minecraft.getInstance().fontRenderer.trimStringToWidth(new TranslationTextComponent("configured.gui.not_installed").mergeStyle(TextFormatting.RED), Math.max(ModConfigSelectionScreen.this.width / 2 - 43, 170)), mouseX, mouseY);
                    }
                    else if(!this.hasRequiredPermission())
                    {
                        ModConfigSelectionScreen.this.renderTooltip(matrixStack, Minecraft.getInstance().fontRenderer.trimStringToWidth(new TranslationTextComponent("configured.gui.no_permission").mergeStyle(TextFormatting.RED), Math.max(ModConfigSelectionScreen.this.width / 2 - 43, 170)), mouseX, mouseY);
                    }
                }
            });
        }

        @Override
        public void render(MatrixStack matrixStack, int x, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean p_230432_9_, float partialTicks)
        {
            AbstractGui.drawString(matrixStack, Minecraft.getInstance().fontRenderer, this.title, left + 28, top + 2, 0xFFFFFF);
            AbstractGui.drawString(matrixStack, Minecraft.getInstance().fontRenderer, this.fileName, left + 28, top + 12, 0xFFFFFF);
            Minecraft.getInstance().getTextureManager().bindTexture(IconButton.ICONS);
            float brightness = true ? 1.0F : 0.5F;
            RenderSystem.color4f(brightness, brightness, brightness, 1.0F);
            blit(matrixStack, left + 4, top, 18, 22, this.getIconU(), 11, 9, 11, 64, 64);
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

            this.modifyButton.x = left + width - 83;
            this.modifyButton.y = top;
            this.modifyButton.render(matrixStack, mouseX, mouseY, partialTicks);

            if(this.restoreButton != null)
            {
                this.restoreButton.x = left + width - 21;
                this.restoreButton.y = top;
                this.restoreButton.render(matrixStack, mouseX, mouseY, partialTicks);
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
        public List<? extends IGuiEventListener> getEventListeners()
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
