package com.mrcrayfish.configured.client.screen;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mrcrayfish.configured.client.screen.widget.IconButton;
import com.mrcrayfish.configured.client.util.ConfigUtil;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Author: MrCrayfish
 */
public class ModConfigSelectionScreen extends ConfigScreen
{
    private final Map<ModConfig.Type, Set<ModConfig>> configMap;

    public ModConfigSelectionScreen(Screen parent, String displayName, ResourceLocation background, Map<ModConfig.Type, Set<ModConfig>> configMap)
    {
        super(parent, displayName, background, null, false, true);
        this.configMap = configMap;
    }

    @Override
    protected void constructEntries()
    {
        List<Entry> entries = new ArrayList<>();
        Set<ModConfig> clientConfigs = this.configMap.get(ModConfig.Type.CLIENT);
        if(clientConfigs != null)
        {
            entries.add(new TitleEntry(new TranslationTextComponent("configured.gui.title.client_configuration").getString()));
            clientConfigs.forEach(config ->
            {
                entries.add(new FileEntry(config));
            });
        }
        Set<ModConfig> commonConfigs = this.configMap.get(ModConfig.Type.COMMON);
        if(commonConfigs != null)
        {
            entries.add(new TitleEntry(new TranslationTextComponent("configured.gui.title.common_configuration").getString()));
            commonConfigs.forEach(config ->
            {
                entries.add(new FileEntry(config));
            });
        }
        Set<ModConfig> serverConfigs = this.configMap.get(ModConfig.Type.SERVER);
        if(serverConfigs != null)
        {
            entries.add(new TitleEntry(new TranslationTextComponent("configured.gui.title.server_configuration").getString()));
            serverConfigs.forEach(config ->
            {
                entries.add(new FileEntry(config));
            });
        }
        this.entries = ImmutableList.copyOf(entries);
    }

    @OnlyIn(Dist.CLIENT)
    public class FileEntry extends Entry
    {
        protected final ModConfig config;
        protected final ITextComponent title;
        protected final ITextComponent fileName;
        protected final Button modifyButton;
        @Nullable
        protected final Button restoreButton;
        private final List<Pair<ForgeConfigSpec.ConfigValue<?>, ForgeConfigSpec.ValueSpec>> allConfigValues;

        public FileEntry(ModConfig config)
        {
            super(createLabelFromModConfig(config).getString());
            this.config = config;
            this.title = createLabelFromModConfig(config);
            this.allConfigValues = ConfigUtil.gatherAllConfigValues(config);
            this.fileName = this.createTrimmedFileName(config.getFileName());
            this.modifyButton = this.createModifyButton(config);
            if(config.getType() != ModConfig.Type.SERVER)
            {
                Button.ITooltip tooltip = (button, matrixStack, mouseX, mouseY) ->
                {
                    if(button.active && button.isHovered())
                    {
                        ModConfigSelectionScreen.this.renderTooltip(matrixStack, ModConfigSelectionScreen.this.minecraft.fontRenderer.trimStringToWidth(new TranslationTextComponent("configured.gui.restore_defaults"), Math.max(ModConfigSelectionScreen.this.width / 2 - 43, 170)), mouseX, mouseY);
                    }
                };
                this.restoreButton = new IconButton(0, 0, 20, 20, 0, 0, tooltip, onPress -> {
                    ConfirmationScreen confirmScreen = new ConfirmationScreen(ModConfigSelectionScreen.this, new TranslationTextComponent("configured.gui.restore_message"), result -> {
                        if(!result || this.allConfigValues == null) return;
                        // Resets all config values
                        this.allConfigValues.forEach(pair -> {
                            ForgeConfigSpec.ConfigValue configValue = pair.getLeft();
                            ForgeConfigSpec.ValueSpec valueSpec = pair.getRight();
                            configValue.set(valueSpec.getDefault());
                        });
                    });
                    confirmScreen.setBackground(background);
                    confirmScreen.setPositiveText(new TranslationTextComponent("configured.gui.restore").mergeStyle(TextFormatting.GOLD, TextFormatting.BOLD));
                    confirmScreen.setNegativeText(DialogTexts.GUI_CANCEL);
                    ModConfigSelectionScreen.this.minecraft.displayGuiScreen(confirmScreen);
                    this.updateRestoreDefaultButton();
                    config.save();
                });
                this.updateRestoreDefaultButton();
            }
            else
            {
                this.restoreButton = null;
            }
        }

        private ITextComponent createTrimmedFileName(String fileName)
        {
            ITextComponent trimmedFileName = new StringTextComponent(fileName).mergeStyle(TextFormatting.GRAY);
            if(ModConfigSelectionScreen.this.minecraft.fontRenderer.getStringWidth(fileName) > 160)
            {
                trimmedFileName = new StringTextComponent(ModConfigSelectionScreen.this.minecraft.fontRenderer.func_238412_a_(fileName, 150) + "...").mergeStyle(TextFormatting.GRAY);
            }
            return trimmedFileName;
        }

        /**
         * Creates and returns a new modify button instance. Since server configurations are handled different, the label and click handler
         * of this button is different if the given ModConfig instance is of the server type. It's just better to reuse
         * @param config
         * @return
         */
        private Button createModifyButton(ModConfig config)
        {
            boolean serverConfig = config.getType() == ModConfig.Type.SERVER;
            String langKey = serverConfig ? "configured.gui.select_world" : "configured.gui.modify";
            Button button = new Button(0, 0, serverConfig ? 72 : 50, 20, new TranslationTextComponent(langKey), onPress -> {
                if(serverConfig) {
                    ModConfigSelectionScreen.this.minecraft.displayGuiScreen(new WorldSelectionScreen(ModConfigSelectionScreen.this.parent, ModConfigSelectionScreen.this.background));
                } else {
                    ModList.get().getModContainerById(config.getModId()).ifPresent(container -> {
                        ModConfigSelectionScreen.this.minecraft.displayGuiScreen(new ConfigScreen(ModConfigSelectionScreen.this, container.getModInfo().getDisplayName(), config, ModConfigSelectionScreen.this.background));
                    });
                }
            });
            return button;
        }

        @Override
        public void render(MatrixStack matrixStack, int x, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean p_230432_9_, float partialTicks)
        {
            AbstractGui.drawString(matrixStack, ModConfigSelectionScreen.this.minecraft.fontRenderer, this.title, left + 28, top + 2, 0xFFFFFF);
            AbstractGui.drawString(matrixStack, ModConfigSelectionScreen.this.minecraft.fontRenderer, this.fileName, left + 28, top + 12, 0xFFFFFF);
            ModConfigSelectionScreen.this.minecraft.getTextureManager().bindTexture(IconButton.ICONS);
            float brightness = true ? 1.0F : 0.5F;
            RenderSystem.color4f(brightness, brightness, brightness, 1.0F);
            blit(matrixStack, left + 4, top, 18, 22, this.getIconU(), 11, 9, 11, 32, 32);
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

            this.modifyButton.x = left + width - 73;
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
            if(this.config != null && this.restoreButton != null)
            {
                this.restoreButton.active = ConfigUtil.isModified(this.config);
            }
        }
    }

    @Override
    protected Predicate<Entry> getSearchPredicate(String s)
    {
        return entry -> (entry instanceof FileEntry) && entry.getLabel().toLowerCase(Locale.ENGLISH).contains(s.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Tries to create a readable label from the file name of the given mod config.
     *
     * @param config
     * @return
     */
    private static ITextComponent createLabelFromModConfig(ModConfig config)
    {
        String fileName = config.getFileName();
        fileName = fileName.replace(config.getModId() + "-", "");
        fileName = fileName.substring(0, fileName.length() - ".toml".length());
        fileName = FilenameUtils.getName(fileName);
        fileName = ConfigScreen.createLabel(fileName);
        return new StringTextComponent(fileName);
    }
}
