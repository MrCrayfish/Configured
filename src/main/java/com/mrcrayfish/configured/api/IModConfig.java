package com.mrcrayfish.configured.api;

import com.mrcrayfish.configured.impl.ForgeConfig;
import net.minecraftforge.fml.config.ModConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * 
 * @author Speiger
 * 
 * Config interface that allows you to implement custom config formats into Configured.
 * This isn't a full automatic system. It is just a interface to make such things actually possible.
 */
public interface IModConfig
{
	/**
	 * This function expects you to do everything necessary to save the config.
	 * If you want a example Lookup {@link ForgeConfig} for how it should be done.
	 * @param entry the entry that is used or should be checked for updates.
	 * Also make sure to check children if children of said entry have been changed too.
	 */
	void saveConfig(IConfigEntry entry);
	/**
	 * This function returns provides the Entry point of the Configuration File.
	 * So users can traverse through it.
	 * @return the root node.
	 */
	IConfigEntry getRoot();
	/**
	 * If the configuration file is a server (local world or multiplayer) this function should return true
	 * @return if the configuration is serversided
	 */
	ModConfig.Type getConfigType();
	
	/**
	 * @return the filename of the config
	 */
	String getFileName();
	/**
	 * @return the modId of the config.
	 */
	String getModId();
	
	/**
	 * A Helper function that allows to load the config from the server into the config instance.
	 * Since this is highly dynamic it has to be done on a per implementation basis.
	 * @param path to the expected config folder.
	 * @param result send self if self got updated. if nothing got updated dont push anything into the result
	 * @throws IOException since its IO work the function will be expected to maybe throw IOExceptions
	 */
	void loadServerConfig(Path path, Consumer<IModConfig> result) throws IOException;
}
