package com.mrcrayfish.configured.impl.jei;

import com.google.common.collect.ImmutableList;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.ValueEntry;
import mezz.jei.api.runtime.config.IJeiConfigCategory;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Author: MrCrayfish
 */
public class JeiCategoryEntry implements IConfigEntry
{
    private final IJeiConfigCategory category;
    private @Nullable List<IConfigEntry> entries;

    public JeiCategoryEntry(IJeiConfigCategory category)
    {
        this.category = category;
    }

    @Override
    public List<IConfigEntry> getChildren()
    {
        if(this.entries == null)
        {
            ImmutableList.Builder<IConfigEntry> builder = ImmutableList.builder();
            this.category.getConfigValues()
                    .forEach(configValue ->
                    {
                        Objects.requireNonNull(configValue);
                        builder.add(new ValueEntry(new JeiValue<>(configValue)));
                    });
            this.entries = builder.build();
        }

        return this.entries;
    }

    @Override
    public boolean isRoot()
    {
        return false;
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
        return this.category.getName();
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
