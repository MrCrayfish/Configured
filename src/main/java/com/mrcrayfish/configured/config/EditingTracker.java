package com.mrcrayfish.configured.config;

import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.client.screen.IEditing;
import net.minecraftforge.client.event.ScreenOpenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Author: MrCrayfish
 */
public class EditingTracker
{
    private IModConfig editingConfig;

    private static EditingTracker instance;

    public static EditingTracker instance()
    {
        if(instance == null)
        {
            instance = new EditingTracker();
        }
        return instance;
    }

    private EditingTracker() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onScreenOpen(ScreenOpenEvent event)
    {
        // Keeps track of the config currently being editing and runs events accordingly
        if(event.getScreen() instanceof IEditing editing)
        {
            if(this.editingConfig == null)
            {
                this.editingConfig = editing.getActiveConfig();
                this.editingConfig.startEditing();
                Configured.LOGGER.info("Started editing '" + this.editingConfig.getFileName() + "'");
            }
            else if(editing.getActiveConfig() == null)
            {
                throw new NullPointerException("A null config was returned when getting active config");
            }
            else if(this.editingConfig != editing.getActiveConfig())
            {
                throw new IllegalStateException("Trying to edit a config while one is already loaded. This should not happen!");
            }
        }
        else if(this.editingConfig != null)
        {
            Configured.LOGGER.info("Stopped editing '" + this.editingConfig.getFileName() + "'");
            this.editingConfig.stopEditing();
            this.editingConfig = null;
        }
    }
}
