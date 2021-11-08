package com.mrcrayfish.configured.client.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.AnvilConverterException;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.storage.SaveFormat;

/**
 * Author: MrCrayfish
 */
public class WorldSelectionScreen extends ConfigScreen
{
    public WorldSelectionScreen(Screen parent, ResourceLocation background)
    {
        super(parent, new TranslationTextComponent("configured.gui.select_world").getString(), (ConfigFileEntry) null, background);
    }

    @Override
    protected void constructEntries()
    {
        try
        {
            SaveFormat saveFormat = this.minecraft.getSaveLoader();
            saveFormat.getSaveList().forEach(worldSummary -> {
                System.out.println(worldSummary.getFileName());
            });
        }
        catch(AnvilConverterException e)
        {
            e.printStackTrace();
        }
    }

    public class WorldEntry extends Entry
    {
        public WorldEntry(String label)
        {
            super(label);
        }

        @Override
        public void render(MatrixStack p_230432_1_, int p_230432_2_, int p_230432_3_, int p_230432_4_, int p_230432_5_, int p_230432_6_, int p_230432_7_, int p_230432_8_, boolean p_230432_9_, float p_230432_10_)
        {

        }
    }
}
