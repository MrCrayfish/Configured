package com.mrcrayfish.configured.impl.jei;

import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import mezz.jei.api.runtime.config.IJeiConfigCategory;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Author: MrCrayfish
 */
public class JeiCategoryListEntry implements IConfigEntry
{
    private final String name;
    private final List<IConfigEntry> entries;

    public JeiCategoryListEntry(String name, List<? extends IJeiConfigCategory> categories)
    {
        this.name = name;
        this.entries = categories.stream().<IConfigEntry>map(JeiCategoryEntry::new).toList();
    }

    @Override
    public List<IConfigEntry> getChildren()
    {
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

    @Override
    public @Nullable IConfigValue<?> getValue()
    {
        return null;
    }

    @Override
    public String getEntryName()
    {
        return this.name;
    }

    @Override
    public @Nullable Component getTooltip()
    {
        return null;
    }

    @Override
    public @Nullable String getTranslationKey()
    {
        return null;
    }
}
