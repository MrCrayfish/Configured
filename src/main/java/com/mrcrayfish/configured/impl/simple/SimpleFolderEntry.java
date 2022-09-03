package com.mrcrayfish.configured.impl.simple;

import com.google.common.collect.ImmutableList;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.ValueEntry;
import com.mrcrayfish.configured.api.simple.EnumProperty;
import com.mrcrayfish.configured.api.simple.ListProperty;
import com.mrcrayfish.configured.config.ConfigManager;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Author: MrCrayfish
 */
public class SimpleFolderEntry implements IConfigEntry
{
    private final String label;
    private final ConfigManager.PropertyMap map;
    private final boolean root;
    private List<IConfigEntry> entries;

    public SimpleFolderEntry(String label, ConfigManager.PropertyMap map, boolean root)
    {
        this.label = label;
        this.map = map;
        this.root = root;
    }

    @Override
    public List<IConfigEntry> getChildren()
    {
        if(this.entries == null)
        {
            ImmutableList.Builder<IConfigEntry> builder = ImmutableList.builder();
            this.map.getConfigMaps().forEach(pair ->
            {
                builder.add(new SimpleFolderEntry(pair.getLeft(), pair.getRight(), false));
            });
            this.map.getConfigProperties().forEach(property ->
            {
                if(property instanceof ListProperty<?> listProperty)
                {
                    builder.add(new ValueEntry(new SimpleListValue<>(listProperty)));
                }
                else if(property instanceof EnumProperty<?> enumProperty)
                {
                    builder.add(new ValueEntry(new SimpleEnumValue<>(enumProperty)));
                }
                else
                {
                    builder.add(new ValueEntry(new SimpleValue<>(property)));
                }
            });
            this.entries = builder.build();
        }
        return this.entries;
    }

    @Override
    public boolean isRoot()
    {
        return this.root;
    }

    @Override
    public boolean isLeaf()
    {
        return false;
    }

    @Nullable
    @Override
    public IConfigValue<?> getValue()
    {
        return null;
    }

    @Override
    public String getEntryName()
    {
        return this.label;
    }
}
