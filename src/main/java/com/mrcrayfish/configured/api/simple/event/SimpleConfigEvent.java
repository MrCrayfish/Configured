package com.mrcrayfish.configured.api.simple.event;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

/**
 * Author: MrCrayfish
 */
public abstract class SimpleConfigEvent extends Event implements IModBusEvent
{
    private final Object source;

    public SimpleConfigEvent(Object source)
    {
        this.source = source;
    }

    /**
     * @return The config object linked to the event
     */
    public Object getSource()
    {
        return this.source;
    }

    /**
     * An event that is called when a simple config is loaded. Server configs that are temporarily
     * loaded when editing from the main menu will not send this event when loaded.
     */
    public static class Load extends SimpleConfigEvent
    {
        public Load(Object source)
        {
            super(source);
        }
    }

    /**
     * An event that is called when a simple config is unloaded. Server configs that are temporarily
     * loaded when editing from the main menu will not send this event when unloaded.
     */
    public static class Unload extends SimpleConfigEvent
    {
        public Unload(Object source)
        {
            super(source);
        }
    }

    /**
     * An event that is called when a simple config is reloaded/updated. Server configs that are
     * temporarily loaded for editing from the main menu will not send this event when updated.
     */
    public static class Reload extends SimpleConfigEvent
    {
        public Reload(Object source)
        {
            super(source);
        }
    }
}
