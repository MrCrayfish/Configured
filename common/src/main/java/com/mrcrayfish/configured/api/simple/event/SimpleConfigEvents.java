package com.mrcrayfish.configured.api.simple.event;

import com.mrcrayfish.framework.api.event.FrameworkEvent;
import com.mrcrayfish.framework.api.event.IFrameworkEvent;

/**
 * Author: MrCrayfish
 */
public class SimpleConfigEvents
{
    public static final FrameworkEvent<Load> LOAD = new FrameworkEvent<>(listeners -> object -> {
       listeners.forEach(listener -> listener.handle(object));
    });

    public static final FrameworkEvent<Unload> UNLOAD = new FrameworkEvent<>(listeners -> object -> {
        listeners.forEach(listener -> listener.handle(object));
    });

    public static final FrameworkEvent<Reload> RELOAD = new FrameworkEvent<>(listeners -> object -> {
        listeners.forEach(listener -> listener.handle(object));
    });

    public interface Load extends IFrameworkEvent
    {
        void handle(Object object);
    }

    public interface Unload extends IFrameworkEvent
    {
        void handle(Object object);
    }

    public interface Reload extends IFrameworkEvent
    {
        void handle(Object object);
    }
}
