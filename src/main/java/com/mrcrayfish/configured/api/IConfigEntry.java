package com.mrcrayfish.configured.api;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * @author Speiger
 * <p>
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
     *
     * @return true if it is the root node.
     */
    boolean isRoot();

    /**
     * Info function to determine if a entry is a value or folder.
     *
     * @return true if the config entry is a value.
     */
    boolean isLeaf();

    /**
     * Returns a Temporary ConfigValue holder that allows users to edit the value without making permanent changes.
     *
     * @return a ValueHolder of the current ConfigEntry.
     */
    @Nullable
    IConfigValue<?> getValue();

    /**
     * Helper function to return the currents Folder Name.
     *
     * @return name of the current folder.
     */
    String getEntryName();

    default Set<IConfigValue<?>> getChangedValues()
    {
        Set<IConfigValue<?>> changed = new HashSet<>();
        Queue<IConfigEntry> found = new ArrayDeque<>();
        found.add(this);
        while(!found.isEmpty())
        {
            IConfigEntry toSave = found.poll();
            if(!toSave.isLeaf())
            {
                found.addAll(toSave.getChildren());
                continue;
            }

            IConfigValue<?> value = toSave.getValue();
            if(value != null && value.isChanged())
            {
                changed.add(value);
            }
        }
        return changed;
    }
}
