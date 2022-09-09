package com.mrcrayfish.configured.api.simple.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Author: MrCrayfish
 */
public final class SimpleConfigEvents
{
    public static final Event<Load> LOAD = EventFactory.createArrayBacked(Load.class, callbacks -> source -> {
        for(Load callback : callbacks) {
            callback.call(source);
        }
    });

    public static final Event<Unload> UNLOAD = EventFactory.createArrayBacked(Unload.class, callbacks -> source -> {
        for(Unload callback : callbacks) {
            callback.call(source);
        }
    });

    public static final Event<Reload> RELOAD = EventFactory.createArrayBacked(Reload.class, callbacks -> source -> {
        for(Reload callback : callbacks) {
            callback.call(source);
        }
    });

    /**
     * An event that is called when a simple config is loaded. Server configs that are temporarily
     * loaded when editing from the main menu will not send this event when loaded.
     */
    @FunctionalInterface
    public interface Load
    {
        void call(Object source);
    }

    /**
     * An event that is called when a simple config is unloaded. Server configs that are temporarily
     * loaded when editing from the main menu will not send this event when unloaded.
     */
    @FunctionalInterface
    public interface Unload
    {
        void call(Object source);
    }

    /**
     * An event that is called when a simple config is reloaded/updated. Server configs that are
     * temporarily loaded for editing from the main menu will not send this event when updated.
     */
    @FunctionalInterface
    public interface Reload
    {
        void call(Object source);
    }
}
