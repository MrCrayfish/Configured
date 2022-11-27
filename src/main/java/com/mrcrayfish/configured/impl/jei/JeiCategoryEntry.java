package com.mrcrayfish.configured.impl.jei;

import com.google.common.collect.ImmutableList;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.ValueEntry;
import com.mrcrayfish.configured.api.simple.validate.Validator;
import mezz.jei.common.config.file.ConfigCategory;
import mezz.jei.common.config.file.ConfigValue;
import mezz.jei.common.config.file.serializers.IntegerSerializer;
import mezz.jei.common.config.file.serializers.ListSerializer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Author: MrCrayfish
 */
public class JeiCategoryEntry implements IConfigEntry
{
    private final String name;
    private final ConfigCategory category;
    private List<IConfigEntry> entries;

    public JeiCategoryEntry(String name, ConfigCategory category)
    {
        this.name = name;
        this.category = category;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<IConfigEntry> getChildren()
    {
        if(this.entries == null)
        {
            ImmutableList.Builder<IConfigEntry> builder = ImmutableList.builder();
            this.category.getValueNames().forEach(name ->
            {
                ConfigValue<?> configValue = this.category.getConfigValue(name);
                Objects.requireNonNull(configValue);
                if(configValue.getSerializer() instanceof IntegerSerializer serializer)
                {
                    Validator<Integer> range = JeiReflection.getRange(serializer);
                    builder.add(new ValueEntry(new JeiValue<>((ConfigValue<Integer>) configValue, range)));
                }
                else if(configValue.getSerializer() instanceof ListSerializer<?> || configValue.getValue() instanceof List<?>)
                {
                    builder.add(new ValueEntry(new JeiListValue<>(configValue)));
                }
                else
                {
                    builder.add(new ValueEntry(new JeiValue<>(configValue)));
                }
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
