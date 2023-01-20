package com.mrcrayfish.configured.client;

import com.mrcrayfish.configured.Configured;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.client.screen.IEditing;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.client.ClientGuiEvent;

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

    public static void registerEvents()
    {
        ClientGuiEvent.INIT_PRE.register((screen, screenAccess) ->
        {
            EditingTracker tracker = instance();
            // Keeps track of the config currently being editing and runs events accordingly
            if(screen instanceof IEditing editing)
            {
                if(tracker.editingConfig == null)
                {
                    tracker.editingConfig = editing.getActiveConfig();
                    tracker.editingConfig.startEditing();
                    Configured.LOGGER.info("Started editing '" + tracker.editingConfig.getFileName() + "'");
                }
                else if(editing.getActiveConfig() == null)
                {
                    throw new NullPointerException("A null config was returned when getting active config");
                }
                else if(tracker.editingConfig != editing.getActiveConfig())
                {
                    throw new IllegalStateException("Trying to edit a config while one is already loaded. This should not happen!");
                }
            }
            else if(tracker.editingConfig != null)
            {
                Configured.LOGGER.info("Stopped editing '" + tracker.editingConfig.getFileName() + "'");
                tracker.editingConfig.stopEditing();
                tracker.editingConfig = null;
            }
            return EventResult.pass();
        });
    }
}
