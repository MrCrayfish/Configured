package com.mrcrayfish.configured.impl.framework;

import com.google.common.collect.ImmutableList;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.ValueEntry;
import com.mrcrayfish.framework.api.config.EnumProperty;
import com.mrcrayfish.framework.api.config.ListProperty;
import com.mrcrayfish.framework.config.FrameworkConfigManager;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Author: MrCrayfish
 */
public class FrameworkFolderEntry implements IConfigEntry
{
    private final FrameworkModConfig.PropertyMap map;
    private List<IConfigEntry> entries;

    public FrameworkFolderEntry(FrameworkModConfig.PropertyMap map)
    {
        this.map = map;
    }

    @Override
    public List<IConfigEntry> getChildren()
    {
        if(this.entries == null)
        {
            ImmutableList.Builder<IConfigEntry> builder = ImmutableList.builder();
            this.map.getConfigMaps().forEach(pair ->
            {
                builder.add(new FrameworkFolderEntry(pair.right()));
            });
            this.map.getConfigProperties().forEach(property ->
            {
                if(property instanceof ListProperty<?> listProperty)
                {
                    builder.add(new ValueEntry(new FrameworkListValue<>(listProperty)));
                }
                else if(property instanceof EnumProperty<?> enumProperty)
                {
                    builder.add(new ValueEntry(new FrameworkEnumValue<>(enumProperty)));
                }
                else
                {
                    builder.add(new ValueEntry(new FrameworkValue<>(property)));
                }
            });
            this.entries = builder.build();
        }
        return this.entries;
    }

    @Override
    public boolean isRoot()
    {
        return this.map.getPath().isEmpty();
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
        return lastValue(this.map.getPath(), "Root");
    }

    @Nullable
    @Override
    public Component getTooltip()
    {
        String comment = this.map.getComment();
        if(comment != null)
        {
            return Component.literal(comment);
        }
        return null;
    }

    @Nullable
    @Override
    public String getTranslationKey()
    {
        return this.map.getTranslationKey();
    }

    /**
     * Gets the last element in a list
     *
     * @param list         the list of get the value from
     * @param defaultValue if the list is empty, return this value instead
     * @param <V>          the type of list
     * @return the last element
     */
    public static <V> V lastValue(List<V> list, V defaultValue)
    {
        if(!list.isEmpty())
        {
            return list.get(list.size() - 1);
        }
        return defaultValue;
    }
}
