package com.mrcrayfish.configured.api;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 
 * @author Speiger
 * 
 * Helper interface that allows to visualize the ConfigTree.
 * It contains all methods that Configured needs to do its work.
 */
public interface IConfigEntry
{
	/**
	 * @return list of ChildElements of the current entry. If non are provided return a empty list.
	 */
	List<IConfigEntry> getChildren();
	/**
	 * If the Entry is start point of the Configuration Tree
	 * @return true if it is the root node.
	 */
	boolean isRoot();
	/**
	 * Info function to determine if a entry is a value or folder.
	 * @return true if the config entry is a value.
	 */
	boolean isLeaf();
	/**
	 * Returns a Temporary ConfigValue holder that allows users to edit the value without making permanent changes.
	 * @return a ValueHolder of the current ConfigEntry.
	 */
	@Nullable
	IConfigValue<?> getValue();
	/**
	 * Helper function to return the currents Folder Name.
	 * @return name of the current folder.
	 */
	String getEntryName();
}
