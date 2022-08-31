package com.mrcrayfish.configured.api;

import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

/**
 * @param <T> A Temporary Data Holder that allows to make changes to a Config File without actually changing the config file.
 *            This makes changing the config for the user a lot simpler since the user gets time to validate all its changes.
 * @author Speiger
 */
public interface IConfigValue<T>
{
    /**
     * @return currently set value in the holder.
     */
    T get();

    /**
     * This method allows to get the Default value specified by the configuration file itself.
     *
     * @return the defaultValue from the config.
     */
    T getDefault();

    /**
     * Sets the current value in the holder.
     *
     * @param value to set into the holder.
     */
    void set(T value);

    /**
     * @param value that should be tested if it can be put into the config.
     * @return true if the value presented is correct.
     */
    boolean isValid(T value);

    /**
     * @return true if the current Holder value is the default value
     */
    boolean isDefault();

    /**
     * @return true if the current value is different from the value when this object was created
     */
    boolean isChanged();

    /**
     * Sets the current holder value to the default config value.
     */
    void restore();

    /**
     * @return the comment of the current config. If present.
     */
    @Nullable
    String getComment();

    /**
     * If the config has translation support included this can return the translation key into the config
     *
     * @return the translation key
     */
    @Nullable
    String getTranslationKey();

    @Nullable
    Component getValidationHint();

    /**
     * @return current directory name of the config entry this value is in.
     */
    String getName();

    /**
     * If your config has a cache this is when it should be cleaned
     */
    void cleanCache();

    boolean requiresWorldRestart();
}
