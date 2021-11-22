package com.mrcrayfish.configured.client.screen;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mrcrayfish.configured.client.util.ScreenUtil;
import com.mrcrayfish.configured.util.ConfigHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Author: MrCrayfish
 */
public class WorldSelectionScreen extends ListMenuScreen
{
    private static final LevelResource SERVER_CONFIG_FOLDER = new LevelResource("serverconfig");

    private final ModConfig config;

    public WorldSelectionScreen(Screen parent, ResourceLocation background, ModConfig config, Component title)
    {
        super(parent, new TranslatableComponent("configured.gui.edit_world_config", title.plainCopy().withStyle(ChatFormatting.YELLOW)), background, 30);
        this.config = config;
    }

    @Override
    protected void constructEntries(List<Item> entries)
    {
        try
        {
            LevelStorageSource source = Minecraft.getInstance().getLevelSource();
            List<LevelSummary> levelList = source.getLevelList();
            Collections.sort(levelList);
            levelList.forEach(worldSummary -> entries.add(new WorldItem(worldSummary)));
        }
        catch(LevelStorageException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void init()
    {
        super.init();
        this.addRenderableWidget(new Button(this.width / 2 - 75, this.height - 29, 150, 20, CommonComponents.GUI_BACK, button -> this.minecraft.setScreen(this.parent)));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks)
    {
        super.render(poseStack, mouseX, mouseY, partialTicks);
        poseStack.pushPose();
        poseStack.translate(this.width - 30, 15, 0);
        poseStack.scale(2.5F, 2.5F, 2.5F);
        Screen.drawString(poseStack, this.font, new TextComponent("?").withStyle(ChatFormatting.BOLD), 0, 0, 0xFFFFFF);
        poseStack.popPose();
    }

    @Override
    protected void updateTooltip(int mouseX, int mouseY)
    {
        super.updateTooltip(mouseX, mouseY);
        if(ScreenUtil.isMouseWithin(this.width - 30, 15, 23, 23, mouseX, mouseY))
        {
            this.setActiveTooltip(Minecraft.getInstance().font.split(new TranslatableComponent("configured.gui.server_config_info"), 200));
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
        private final Component worldName;
        private final Component folderName;
        private final ResourceLocation iconId;
        private final File iconFile;
        private final DynamicTexture texture;
        private final Button modifyButton;

        public WorldItem(LevelSummary summary)
        {
            super(summary.getLevelName());
            this.worldName = new TextComponent(summary.getLevelName());
            this.folderName = new TextComponent(summary.getLevelId()).withStyle(ChatFormatting.GRAY);
            this.iconId = new ResourceLocation("minecraft", "worlds/" + Util.sanitizeName(summary.getLevelId(), ResourceLocation::validPathChar) + "/" + Hashing.sha1().hashUnencodedChars(summary.getLevelId()) + "/icon");
            this.iconFile = summary.getIcon().isFile() ? summary.getIcon() : null;
            this.texture = this.loadWorldIcon();
            this.modifyButton = new Button(0, 0, 50, 20, new TranslatableComponent("configured.gui.select"), onPress -> {
                this.loadServerConfig(summary.getLevelId(), summary.getLevelName());
            });
        }

        @Override
        public List<? extends GuiEventListener> children()
        {
            return ImmutableList.of(this.modifyButton);
        }

        @Override
        public void render(PoseStack poseStack, int x, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean p_230432_9_, float partialTicks)
        {
            RenderSystem.setShaderTexture(0, this.iconId);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            blit(poseStack, left + 4, top, 22, 22, 0, 0, 64, 64, 64, 64);
            Screen.drawString(poseStack, WorldSelectionScreen.this.minecraft.font, this.worldName, left + 32, top + 2, 0xFFFFFF);
            Screen.drawString(poseStack, WorldSelectionScreen.this.minecraft.font, this.folderName, left + 32, top + 12, 0xFFFFFF);
            this.modifyButton.x = left + width - 51;
            this.modifyButton.y = top;
            this.modifyButton.render(poseStack, mouseX, mouseY, partialTicks);
        }

        private DynamicTexture loadWorldIcon()
        {
            try(InputStream is = new FileInputStream(this.iconFile); NativeImage image = NativeImage.read(is))
            {
                if(image.getWidth() != 64 || image.getHeight() != 64)
                    return null;
                DynamicTexture texture = new DynamicTexture(image);
                WorldSelectionScreen.this.minecraft.getTextureManager().register(this.iconId, texture);
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
            try(LevelStorageSource.LevelStorageAccess storageAccess = Minecraft.getInstance().getLevelSource().createAccess(worldFileName))
            {
                Path serverConfigPath = storageAccess.getLevelPath(SERVER_CONFIG_FOLDER);
                FileUtils.getOrCreateDirectory(serverConfigPath, "serverconfig");
                final CommentedFileConfig data = config.getHandler().reader(serverConfigPath).apply(config);
                ConfigHelper.setConfigData(config, data);
                ModList.get().getModContainerById(config.getModId()).ifPresent(container -> {
                    WorldSelectionScreen.this.minecraft.setScreen(new ConfigScreen(parent, new TextComponent(worldName), config, background));
                });
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
