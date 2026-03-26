package com.mrcrayfish.configured.client.screen;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.NativeImage;
import com.mrcrayfish.configured.Constants;
import com.mrcrayfish.configured.api.ActionResult;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.client.screen.widget.IconButton;
import com.mrcrayfish.configured.client.util.ScreenUtil;
import com.mrcrayfish.configured.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import org.apache.commons.io.file.PathUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Author: MrCrayfish
 */
public class WorldSelectionScreen extends ListMenuScreen
{
    private static final LevelResource SERVER_CONFIG_FOLDER = Services.CONFIG.getServerConfigResource();
    private static final Identifier MISSING_ICON = Identifier.withDefaultNamespace("textures/misc/unknown_server.png");

    private final IModConfig config;

    public WorldSelectionScreen(Screen parent, IModConfig config, Component title)
    {
        super(parent, Component.translatable("configured.gui.edit_world_config", title.plainCopy().withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD)), 30);
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
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTicks)
    {
        super.extractRenderState(extractor, mouseX, mouseY, partialTicks);

        extractor.pose().pushMatrix();
        extractor.pose().translate(this.width - 30, 15);
        extractor.pose().scale(2.5F, 2.5F);
        extractor.text(this.font, Component.literal("?").withStyle(ChatFormatting.BOLD), 0, 0, 0xFFFFFFFF);
        extractor.pose().popMatrix();

        if(ScreenUtil.isMouseWithin(this.width - 30, 15, 23, 23, mouseX, mouseY))
        {
            this.setActiveTooltip(extractor, Component.translatable("configured.gui.server_config_info"), mouseX, mouseY);
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
        private Path iconFile;
        private final Button modifyButton;
        private final FaviconTexture icon;

        public WorldItem(LevelSummary summary)
        {
            super(summary.getLevelName());
            this.worldName = Component.literal(summary.getLevelName());
            this.folderName = Component.literal(summary.getLevelId()).withStyle(ChatFormatting.DARK_GRAY);
            this.icon = FaviconTexture.forWorld(Minecraft.getInstance().getTextureManager(), summary.getLevelId());
            this.iconFile = summary.getIcon();
            this.validateIcon();
            this.loadWorldIcon();
            this.modifyButton = new IconButton(0, 0, 0, this.getIconV(), 60, this.getButtonLabel(), onPress -> {
                this.loadWorldConfig(summary.getLevelId(), summary.getLevelName());
            });
        }

        private void validateIcon()
        {
            if(this.iconFile == null)
                return;

            try
            {
                BasicFileAttributes attributes = Files.readAttributes(this.iconFile, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                if(attributes.isSymbolicLink())
                {
                    List<ForbiddenSymlinkInfo> list = Minecraft.getInstance().directoryValidator().validateSymlink(this.iconFile);
                    if(!list.isEmpty())
                    {
                        this.iconFile = null;
                        return;
                    }
                    attributes = Files.readAttributes(this.iconFile, BasicFileAttributes.class);
                }
                if(!attributes.isRegularFile())
                {
                    this.iconFile = null;
                }
            }
            catch(IOException e)
            {
                this.iconFile = null;
            }
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
        public void extractContent(GuiGraphicsExtractor extractor, int mouseX, int mouseY, boolean hovered, float partialTick)
        {
            if(this.modifyButton.isMouseOver(mouseX, mouseY)) extractor.fill(this.getX() - 1, this.getY() - 1, this.getX() + 25, this.getY() + 25, 0xFFFFFFFF);
            extractor.blit(RenderPipelines.GUI_TEXTURED, this.icon.textureLocation(), this.getX(), this.getY(), 0, 0, 24, 24, 32, 32, 32, 32);
            extractor.text(WorldSelectionScreen.this.minecraft.font, this.worldName, this.getX() + 30, this.getY() + 3, 0xFFFFFFFF);
            extractor.text(WorldSelectionScreen.this.minecraft.font, this.folderName, this.getX() + 30, this.getY() + 13, 0xFFFFFFFF);
            this.modifyButton.setX(this.getX() + this.getWidth() - 61);
            this.modifyButton.setY(this.getY() + 2);
            this.modifyButton.extractRenderState(extractor, mouseX, mouseY, partialTick);
        }

        private void loadWorldIcon()
        {
            if(this.iconFile == null || !Files.isRegularFile(this.iconFile))
                return;
            try(InputStream is = Files.newInputStream(this.iconFile); NativeImage image = NativeImage.read(is))
            {
                if(image.getWidth() != 64 || image.getHeight() != 64)
                    return;
                this.icon.upload(image);
            }
            catch(IOException ignored) {}
        }

        public void disposeIcon()
        {
            this.icon.clear();
        }

        private void loadWorldConfig(String worldFileName, String worldName)
        {
            try(LevelStorageSource.LevelStorageAccess storageAccess = Minecraft.getInstance().getLevelSource().createAccess(worldFileName))
            {
                // TODO move to config specific
                Path worldConfigPath = storageAccess.getLevelPath(SERVER_CONFIG_FOLDER).toAbsolutePath();
                PathUtils.createParentDirectories(worldConfigPath);
                if(!Files.isDirectory(worldConfigPath))
                    Files.createDirectory(worldConfigPath);
                ActionResult result = WorldSelectionScreen.this.config.loadWorldConfig(worldConfigPath);
                if(result.asBoolean())
                {
                    Component configName = Component.literal(ModConfigSelectionScreen.createLabelFromModConfig(WorldSelectionScreen.this.config));
                    Component newTitle = Component.literal(worldName).copy().append(Component.literal(" > ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)).append(configName);
                    WorldSelectionScreen.this.minecraft.setScreen(new ConfigScreen(WorldSelectionScreen.this.parent, newTitle, WorldSelectionScreen.this.config));
                    return;
                }
                Component message = result.message().orElse(Component.translatable("configured.gui.load_world_config_failed"));
                ConfirmationScreen.showError(WorldSelectionScreen.this.minecraft, WorldSelectionScreen.this, message);
            }
            catch(IOException e)
            {
                Constants.LOG.error("Failed to load world config", e);
                ConfirmationScreen.showError(WorldSelectionScreen.this.minecraft, WorldSelectionScreen.this, Component.translatable("configured.gui.load_world_config_exception"));
            }
        }
    }
}
