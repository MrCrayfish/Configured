package com.mrcrayfish.configured.api;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Author: MrCrayfish
 */
public enum ConfigType
{
    /**
     * This type is the equivalent of the regular Forge client config type.
     *
     * A configuration that is only created and loaded on the client. This configuration
     * is stored in the normal config directory, located from the working directory of the game.
     * Properties from this config should only ever be accessed from client code.
     *
     * This is a good config type for rendering properties since they will only ever be used on the client.
     */
    CLIENT(Environment.CLIENT, false, false),

    /**
     * This type is the equivalent of the regular Forge common config type.
     *
     * A configuration that is created and loaded on both the client and server. This is a good config type
     * for rendering properties since they will only ever be used on the client. This configuration
     * will be stored in the normal config directory, located from the working directory of the game
     */
    UNIVERSAL(null, false, false),

    /**
     * A configuration that is only created and loaded on the server. This configuration is stored
     * in the normal config directory, located from the working directory of the game.
     */
    SERVER(null, true, false),

    /**
     * A configuration that is created and loaded on the initialization of a server, and is synced
     * to clients upon joining the server. This means it is accessible by clients, but only while
     * playing on the server. This configuration is stored in the normal config directory, located
     * from the working directory of the game.
     *
     * This is a good config type to use if you want to give control to the server while having the
     * properties accessible on clients. Unlike Forge server configs, this config is not hidden
     * in the world folder which makes it a little less confusing when locating in the file explorer.
     */
    SERVER_SYNC(null, true, true),

    //TODO should this be editable?
    /**
     * A configuration that is only created and loaded on a dedicated server
     */
    DEDICATED_SERVER(Environment.DEDICATED_SERVER, true, false),

    /**
     * A configuration that is created and loaded from a world. This means that each world can have
     * a different configuration. This configuration is stored in the serverconfig directory, located
     * from the directory of the world. The config properties are only accessible when a world is
     * loaded and is only available on the server (integrated and dedicated). Properties should only
     * be accessed in server only code.
     *
     * This is a good config type to use if you want to the config to be customisable per world and
     * the properties only need to be accessible for the server.
     */
    WORLD(null, true, false),

    /**
     * This type is the equivalent of the regular Forge server config type.
     *
     * A configuration that is created and loaded from a world, and is synced to clients upon
     * joining the server. This means that each world can have  different configuration and clients
     * will also have access to the properties. If a server, the config properties are only accessible
     * when the world is loaded, however for a client, only when connected to a server (integrated and dedicated).
     * This configuration is stored in the serverconfig directory, located from the directory of the world.
     *
     * This is a good config type to use if you want a server controlled config that is customisable
     * per world, while also allowing the properties to be accessible on clients.
     */
    WORLD_SYNC(null, true, true),

    /**
     * Stores the config in the memory. This will not load or save anything.
     */
    MEMORY(null, false, false);

    private final Environment env;
    private final boolean server;
    private final boolean sync;

    /**
     * Default constructor for Storage Types
     *
     * @param env   the distribution the config can load on. A null dist means any.
     * @param server if this config type is loaded when the server is starting
     * @param sync   if the config should sync to clients upon connecting
     */
    ConfigType(@Nullable Environment env, boolean server, boolean sync)
    {
        this.env = env;
        this.server = server;
        this.sync = sync;
    }

    public Optional<Environment> getEnv()
    {
        return Optional.ofNullable(this.env);
    }

    public boolean isServer()
    {
        return this.server;
    }

    public boolean isSync()
    {
        return this.sync;
    }
}
