package com.mrcrayfish.configured.api;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

import com.mrcrayfish.configured.impl.ForgeConfig;

import net.minecraftforge.fml.config.ModConfig;

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
	public void saveConfig(IConfigEntry entry);
	/**
	 * This function returns provides the Entry point of the Configuration File.
	 * So users can traverse through it.
	 * @return the root node.
	 */
	public IConfigEntry getRoot();
	/**
	 * If the configuration file is a server (local world or multiplayer) this function should return true
	 * @return if the configuration is serversided
	 */
	public ModConfig.Type getConfigType();
	
	public String getFileName();
	public String getModId();
	
	public void loadServerConfig(Path path, Consumer<IModConfig> result) throws IOException;
}
