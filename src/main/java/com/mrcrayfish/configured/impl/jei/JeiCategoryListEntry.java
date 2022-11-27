package com.mrcrayfish.configured.impl.jei;

import com.google.common.collect.ImmutableList;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import mezz.jei.common.config.file.ConfigCategory;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Author: MrCrayfish
 */
public class JeiCategoryListEntry implements IConfigEntry
{
    private final String name;
    private final Map<String, ConfigCategory> categories;
    private List<IConfigEntry> entries;

    public JeiCategoryListEntry(String name, Map<String, ConfigCategory> categories)
    {
        this.name = name;
        this.categories = categories;
    }

    @Override
    public List<IConfigEntry> getChildren()
    {
        if(this.entries == null)
        {
            ImmutableList.Builder<IConfigEntry> builder = ImmutableList.builder();
            this.categories.forEach((name, category) -> builder.add(new JeiCategoryEntry(name, category)));
            this.entries = builder.build();
        }
        return this.entries;
    }

    @Override
    public boolean isRoot()
    {
        return true;
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
        return this.name;
    }

    @Nullable
    @Override
    public Component getTooltip()
    {
        return null;
    }

    @Nullable
    @Override
    public String getTranslationKey()
    {
        return null;
    }
}
