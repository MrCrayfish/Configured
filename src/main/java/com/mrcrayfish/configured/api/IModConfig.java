package com.mrcrayfish.configured.api;

import com.mrcrayfish.configured.impl.forge.ForgeConfig;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * @author Speiger
 * <p>
 * Config interface that allows you to implement custom config formats into Configured.
 * This isn't a full automatic system. It is just a interface to make such things actually possible.
 */
public interface IModConfig
{
    /**
     * This function expects you to do everything necessary to save the config.
     * If you want a example Lookup {@link ForgeConfig} for how it should be done.
     *
     * @param entry the entry that is used or should be checked for updates.
     *              Also make sure to check children if children of said entry have been changed too.
     */
    void update(IConfigEntry entry);

    /**
     * This function returns provides the Entry point of the Configuration File.
     * So users can traverse through it.
     *
     * @return the root node.
     */
    IConfigEntry getRoot();

    /**
     * The storage type of this config. This determines where the configuration is loaded from and saved to.
     *
     * @return the storage type of the config
     */
    ConfigType getType();

    /**
     * @return the filename of the config
     */
    String getFileName();

    /**
     * @return the modId of the config.
     */
    String getModId();

    /**
     * @return the name to display on the file list
     */
    @Nullable
    default String getTranslationKey()
    {
        return null;
    }

    /**
     * A Helper function that allows to load the config from the server into the config instance.
     * Since this is highly dynamic it has to be done on a per implementation basis.
     *
     * @param path   to the expected config folder.
     * @param result send self if self got updated. if nothing got updated dont push anything into the result
     * @throws IOException since its IO work the function will be expected to maybe throw IOExceptions
     */
    void loadWorldConfig(Path path, Consumer<IModConfig> result) throws IOException;

    /**
     * A simple utility function to determine if this config has been changed, as in differs from its
     * default values. This method will always return false if this config is a world config type. It
     * should be noted that if the config will be temporarily loaded from file and closed immediately
     * after; this should be considered before calling this method!
     *
     * @return true if this config differs from its default values
     */
    default boolean isChanged()
    {
        return false;
    }

    default void restoreDefaults() {}

    /**
     * An event that is fired when this config is starting to be edited by the player using the
     * in-game menu. This is only fired once during the initial opening of the config.
     */
    default void startEditing() {}

    /**
     * An event that is fired when this config is no longer being edited by the player using the
     * in-game menu. This is only fired once after the player has exited the menu.
     */
    default void stopEditing() {}

    default void syncToServer() {}

    default void requestFromServer() {}
}
