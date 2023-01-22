package com.mrcrayfish.configured.api;

import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class ValueEntry implements IConfigEntry
{
    IConfigValue<?> value;

    public ValueEntry(IConfigValue<?> value)
    {
        this.value = value;
    }

    @Override
    public List<IConfigEntry> getChildren()
    {
        return Collections.emptyList();
    }

    @Override
    public boolean isRoot()
    {
        return false;
    }

    @Override
    public boolean isLeaf()
    {
        return true;
    }

    @Override
    public IConfigValue<?> getValue()
    {
        return this.value;
    }

    @Override
    public String getEntryName()
    {
        return "I am Error";
    }

    @Nullable
    @Override
    public Component getTooltip()
    {
        return this.value.getComment();
    }

    @Nullable
    @Override
    public String getTranslationKey()
    {
        return this.value.getTranslationKey();
    }
}
