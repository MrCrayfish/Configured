package com.mrcrayfish.configured.client.screen;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mrcrayfish.configured.client.util.ScreenUtil;
import com.mrcrayfish.configured.util.ConfigHelper;
import net.minecraft.client.AnvilConverterException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.storage.FolderName;
import net.minecraft.world.storage.SaveFormat;
import net.minecraft.world.storage.WorldSummary;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * Author: MrCrayfish
 */
public class WorldSelectionScreen extends ListMenuScreen
{
    private static final FolderName SERVER_CONFIG_FOLDER = new FolderName("serverconfig");

    private final ModConfig config;

    public WorldSelectionScreen(Screen parent, ResourceLocation background, ModConfig config, ITextComponent title)
    {
        super(parent, new TranslationTextComponent("configured.gui.edit_world_config", title.copyRaw().mergeStyle(TextFormatting.YELLOW)), background, 30);
        this.config = config;
    }

    @Override
    protected void constructEntries(List<Item> entries)
    {
        try
        {
            SaveFormat saveFormat = Minecraft.getInstance().getSaveLoader();
            saveFormat.getSaveList().forEach(worldSummary -> entries.add(new WorldItem(worldSummary)));
        }
        catch(AnvilConverterException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void init()
    {
        super.init();
        this.addButton(new Button(this.width / 2 - 75, this.height - 29, 150, 20, DialogTexts.GUI_BACK, button -> this.minecraft.displayGuiScreen(this.parent)));
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks)
    {
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        matrixStack.push();
        matrixStack.translate(this.width - 30, 15, 0);
        matrixStack.scale(2.5F, 2.5F, 2.5F);
        AbstractGui.drawString(matrixStack, this.font, new StringTextComponent("?").mergeStyle(TextFormatting.BOLD), 0, 0, 0xFFFFFF);
        matrixStack.pop();
    }

    @Override
    protected void updateTooltip(int mouseX, int mouseY)
    {
        super.updateTooltip(mouseX, mouseY);
        if(ScreenUtil.isMouseWithin(this.width - 30, 15, 23, 23, mouseX, mouseY))
        {
            this.setActiveTooltip(Minecraft.getInstance().fontRenderer.trimStringToWidth(new TranslationTextComponent("configured.gui.server_config_info"), 200));
        }
    }

    @Override
    public void onClose()
    {
        super.onClose();
        this.entries.forEach(item ->
        {
            if(item instanceof WorldItem)
            {
                ((WorldItem) item).disposeIcon();
            }
        });
    }

    public class WorldItem extends Item
    {
        private final ITextComponent worldName;
        private final ITextComponent folderName;
        private final ResourceLocation iconId;
        private final File iconFile;
        private final DynamicTexture texture;
        private final Button modifyButton;

        public WorldItem(WorldSummary summary)
        {
            super(summary.getDisplayName());
            this.worldName = new StringTextComponent(summary.getDisplayName());
            this.folderName = new StringTextComponent(summary.getFileName()).mergeStyle(TextFormatting.GRAY);
            this.iconId = new ResourceLocation("minecraft", "worlds/" + Util.func_244361_a(summary.getFileName(), ResourceLocation::validatePathChar) + "/" + Hashing.sha1().hashUnencodedChars(summary.getFileName()) + "/icon");
            this.iconFile = summary.getIconFile().isFile() ? summary.getIconFile() : null;
            this.texture = this.loadWorldIcon();
            this.modifyButton = new Button(0, 0, 50, 20, new TranslationTextComponent("configured.gui.select"), onPress -> {
                this.loadServerConfig(summary.getFileName(), summary.getDisplayName());
            });
        }

        @Override
        public List<? extends IGuiEventListener> getEventListeners()
        {
            return ImmutableList.of(this.modifyButton);
        }

        @Override
        public void render(MatrixStack matrixStack, int x, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean p_230432_9_, float partialTicks)
        {
            WorldSelectionScreen.this.minecraft.getTextureManager().bindTexture(this.iconId);
            blit(matrixStack, left + 4, top, 22, 22, 0, 0, 64, 64, 64, 64);
            AbstractGui.drawString(matrixStack, WorldSelectionScreen.this.minecraft.fontRenderer, this.worldName, left + 32, top + 2, 0xFFFFFF);
            AbstractGui.drawString(matrixStack, WorldSelectionScreen.this.minecraft.fontRenderer, this.folderName, left + 32, top + 12, 0xFFFFFF);
            this.modifyButton.x = left + width - 51;
            this.modifyButton.y = top;
            this.modifyButton.render(matrixStack, mouseX, mouseY, partialTicks);
        }

        private DynamicTexture loadWorldIcon()
        {
            try(InputStream is = new FileInputStream(this.iconFile); NativeImage image = NativeImage.read(is))
            {
                if(image.getWidth() != 64 || image.getHeight() != 64)
                    return null;
                DynamicTexture texture = new DynamicTexture(image);
                WorldSelectionScreen.this.minecraft.getTextureManager().loadTexture(this.iconId, texture);
                return texture;
            }
            catch(IOException ignored) {}
            return null;
        }

        public void disposeIcon()
        {
            if(this.texture != null)
            {
                this.texture.close();
            }
        }

        private void loadServerConfig(String worldFileName, String worldName)
        {
            try(SaveFormat.LevelSave levelSave = WorldSelectionScreen.this.minecraft.getSaveLoader().getLevelSave(worldFileName))
            {
                Path serverConfigPath = levelSave.resolveFilePath(SERVER_CONFIG_FOLDER);
                FileUtils.getOrCreateDirectory(serverConfigPath, "serverconfig");
                final CommentedFileConfig data = config.getHandler().reader(serverConfigPath).apply(config);
                ConfigHelper.setConfigData(config, data);
                ModList.get().getModContainerById(config.getModId()).ifPresent(container -> {
                    WorldSelectionScreen.this.minecraft.displayGuiScreen(new ConfigScreen(parent, new StringTextComponent(worldName), config, background));
                });
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
