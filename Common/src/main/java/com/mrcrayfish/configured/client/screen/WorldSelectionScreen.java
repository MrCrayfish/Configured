package com.mrcrayfish.configured.client.screen;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.client.screen.widget.IconButton;
import com.mrcrayfish.configured.client.util.ScreenUtil;
import com.mrcrayfish.configured.impl.simple.SimpleConfigManager;
import com.mrcrayfish.configured.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Author: MrCrayfish
 */
public class WorldSelectionScreen extends ListMenuScreen
{
    private static final LevelResource SERVER_CONFIG_FOLDER = SimpleConfigManager.createLevelResource("serverconfig");
    private static final ResourceLocation MISSING_ICON = new ResourceLocation("textures/misc/unknown_server.png");

    private final IModConfig config;

    public WorldSelectionScreen(Screen parent, ResourceLocation background, IModConfig config, Component title)
    {
        super(parent, Component.translatable("configured.gui.edit_world_config", title.plainCopy().withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD)), background, 30);
        this.config = config;
    }

    @Override
    protected void constructEntries(List<Item> entries)
    {
        try
        {
            LevelStorageSource source = Minecraft.getInstance().getLevelSource();
            List<LevelSummary> levels = new ArrayList<>(source.loadLevelSummaries(source.findLevelCandidates()).join());
            if(levels.size() > 6)
            {
                entries.add(new TitleItem(Component.translatable("configured.gui.title.recently_played").withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW)));
                List<LevelSummary> recent = levels.stream().sorted(Comparator.comparing(s -> -s.getLastPlayed())).limit(3).toList();
                recent.forEach(summary -> entries.add(new WorldItem(summary)));
                levels.removeAll(recent);
                entries.add(new TitleItem(Component.translatable("configured.gui.title.other_worlds").withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW)));
            }
            levels.stream().sorted(Comparator.comparing(LevelSummary::getLevelName)).forEach(summary -> {
                entries.add(new WorldItem(summary));
            });
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
        this.addRenderableWidget(ScreenUtil.button(this.width / 2 - 75, this.height - 29, 150, 20, CommonComponents.GUI_BACK, button -> this.minecraft.setScreen(this.parent)));
    }

    @Override
    public void render(GuiGraphics poseStack, int mouseX, int mouseY, float partialTicks)
    {
        super.render(poseStack, mouseX, mouseY, partialTicks);
        poseStack.pose().pushPose();
        poseStack.pose().translate(this.width - 30, 15, 0);
        poseStack.pose().scale(2.5F, 2.5F, 2.5F);
        poseStack.drawString(this.font, Component.literal("?").withStyle(ChatFormatting.BOLD), 0, 0, 0xFFFFFF);
        poseStack.pose().popPose();
    }

    @Override
    protected void updateTooltip(int mouseX, int mouseY)
    {
        super.updateTooltip(mouseX, mouseY);
        if(ScreenUtil.isMouseWithin(this.width - 30, 15, 23, 23, mouseX, mouseY))
        {
            this.setActiveTooltip(Component.translatable("configured.gui.server_config_info"));
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
        private Path iconFile;
        private final DynamicTexture texture;
        private final Button modifyButton;

        public WorldItem(LevelSummary summary)
        {
            super(summary.getLevelName());
            this.worldName = Component.literal(summary.getLevelName());
            this.folderName = Component.literal(summary.getLevelId()).withStyle(ChatFormatting.DARK_GRAY);
            this.iconId = new ResourceLocation("minecraft", "worlds/" + Util.sanitizeName(summary.getLevelId(), ResourceLocation::validPathChar) + "/" + Hashing.sha1().hashUnencodedChars(summary.getLevelId()) + "/icon");
            this.iconFile = summary.getIcon();
            if (!Files.isRegularFile(this.iconFile)) {
                this.iconFile = null;
            }
            this.texture = this.loadWorldIcon();
            this.modifyButton = new IconButton(0, 0, 0, this.getIconV(), 60, this.getButtonLabel(), onPress -> {
                this.loadWorldConfig(summary.getLevelId(), summary.getLevelName());
            });
        }

        private Component getButtonLabel()
        {
            if(WorldSelectionScreen.this.config.isReadOnly())
            {
                return Component.translatable("configured.gui.view");
            }
            return Component.translatable("configured.gui.select");
        }

        private int getIconV()
        {
            if(WorldSelectionScreen.this.config.isReadOnly())
            {
                return 33;
            }
            return 22;
        }

        @Override
        public List<? extends GuiEventListener> children()
        {
            return ImmutableList.of(this.modifyButton);
        }

        @Override
        public void render(GuiGraphics poseStack, int x, int top, int left, int width, int p_230432_6_, int mouseX, int mouseY, boolean p_230432_9_, float partialTicks)
        {
            if(x % 2 != 0) poseStack.fill(left, top, left + width, top + 24, 0x55000000);
            if(this.modifyButton.isMouseOver(mouseX, mouseY)) poseStack.fill(left - 1, top - 1, left + 25, top + 25, 0xFFFFFFFF);
            RenderSystem.setShaderTexture(0, this.texture != null ? this.iconId : MISSING_ICON);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            poseStack.blit(this.texture != null ? this.iconId : MISSING_ICON, left, top, 24, 24, 0, 0, 64, 64, 64, 64);
            poseStack.drawString(WorldSelectionScreen.this.minecraft.font, this.worldName, left + 30, top + 3, 0xFFFFFF);
            poseStack.drawString(WorldSelectionScreen.this.minecraft.font, this.folderName, left + 30, top + 13, 0xFFFFFF);
            this.modifyButton.setX(left + width - 61);
            this.modifyButton.setY(top + 2);
            this.modifyButton.render(poseStack, mouseX, mouseY, partialTicks);
        }

        private DynamicTexture loadWorldIcon()
        {
            if(this.iconFile == null)
                return null;
            try(InputStream is = Files.newInputStream(this.iconFile); NativeImage image = NativeImage.read(is))
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

        private void loadWorldConfig(String worldFileName, String worldName)
        {
            try(LevelStorageSource.LevelStorageAccess storageAccess = Minecraft.getInstance().getLevelSource().createAccess(worldFileName))
            {
                Path worldConfigPath = storageAccess.getLevelPath(SERVER_CONFIG_FOLDER);
                SimpleConfigManager.createDirectory(worldConfigPath);
                WorldSelectionScreen.this.config.loadWorldConfig(worldConfigPath, T -> {
                    if(Services.PLATFORM.isModLoaded(T.getModId())) {
                        Component configName = Component.literal(ModConfigSelectionScreen.createLabelFromModConfig(WorldSelectionScreen.this.config));
                        Component newTitle = Component.literal(worldName).copy().append(Component.literal(" > ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)).append(configName);
                        WorldSelectionScreen.this.minecraft.setScreen(new ConfigScreen(WorldSelectionScreen.this.parent, newTitle, T, WorldSelectionScreen.this.background));
                    }
                });
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
