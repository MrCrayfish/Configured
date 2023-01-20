package com.mrcrayfish.configured.impl.simple;

import com.google.common.collect.ImmutableList;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.ValueEntry;
import com.mrcrayfish.configured.api.simple.EnumProperty;
import com.mrcrayfish.configured.api.simple.ListProperty;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Author: MrCrayfish
 */
public class SimpleFolderEntry implements IConfigEntry
{
    private final SimpleConfigManager.PropertyMap map;
    private List<IConfigEntry> entries;

    public SimpleFolderEntry(SimpleConfigManager.PropertyMap map)
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
                builder.add(new SimpleFolderEntry(pair.getRight()));
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
        String key = this.map.getTranslationKey() + ".tooltip";
        if(I18n.exists(key))
        {
            return Component.translatable(key);
        }
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
        if(list.size() > 0)
        {
            return list.get(list.size() - 1);
        }
        return defaultValue;
    }
}
