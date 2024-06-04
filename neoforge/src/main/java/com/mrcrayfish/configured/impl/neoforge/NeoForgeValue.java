package com.mrcrayfish.configured.impl.neoforge;

import com.mrcrayfish.configured.api.IConfigValue;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Objects;

public class NeoForgeValue<T> implements IConfigValue<T>
{
    public final ModConfigSpec.ConfigValue<T> configValue;
    public final ModConfigSpec.ValueSpec valueSpec;
    protected final T initialValue;
    protected T value;
    protected Pair<T, T> range;
    protected Component validationHint;

    public NeoForgeValue(ModConfigSpec.ConfigValue<T> configValue, ModConfigSpec.ValueSpec valueSpec)
    {
        this.configValue = configValue;
        this.valueSpec = valueSpec;
        this.initialValue = configValue.get();
        this.set(configValue.get());
    }

    @Override
    public T get()
    {
        return this.value;
    }

    @Override
    public void set(T value)
    {
        this.value = value;
    }

    @Override
    public boolean isDefault()
    {
        return Objects.equals(this.get(), this.valueSpec.getDefault());
    }

    @Override
    public boolean isChanged()
    {
        return !Objects.equals(this.get(), this.initialValue);
    }

    @Override
    public void restore()
    {
        this.set(this.getDefault());
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getDefault()
    {
        return (T) this.valueSpec.getDefault();
    }

    @Override
    public boolean isValid(T value)
    {
        return this.valueSpec.test(value);
    }

    @Override
    @Nullable
    public Component getComment()
    {
        String rawComment = this.valueSpec.getComment();
        String key = this.getTranslationKey() + ".tooltip";
        if(I18n.exists(key))
        {
            MutableComponent comment = Component.translatable(key);
            if(rawComment != null)
            {
                int rangeIndex = rawComment.indexOf("Range: ");
                int allowedValIndex = rawComment.indexOf("Allowed Values: ");
                if(rangeIndex >= 0 || allowedValIndex >= 0)
                {
                    comment.append(Component.literal(rawComment.substring(Math.max(rangeIndex, allowedValIndex) - 1))); // - 1 to include new line char
                }
            }
            return comment;
        }
        return rawComment != null ? Component.literal(rawComment) : null;
    }

    @Override
    public String getTranslationKey()
    {
        return this.valueSpec.getTranslationKey();
    }

    @Nullable
    @Override
    public Component getValidationHint()
    {
        if(this.validationHint == null)
        {
            this.loadRange();
            if(this.range != null && this.range.getLeft() != null && this.range.getRight() != null)
            {
                this.validationHint = Component.translatable("configured.validator.range_hint", this.range.getLeft().toString(), this.range.getRight().toString());
            }
        }
        return this.validationHint;
    }

    @Override
    public String getName()
    {
        return lastValue(this.configValue.getPath(), "");
    }

    @Override
    public void cleanCache()
    {
        this.configValue.clearCache();
    }

    @Override
    public boolean requiresWorldRestart()
    {
        return this.valueSpec.needsWorldRestart();
    }

    @Override
    public boolean requiresGameRestart()
    {
        return false;
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

    /**
     * Gets Forge's range of a value
     */
    @SuppressWarnings("unchecked")
    public void loadRange()
    {
        if(this.range == null)
        {
            ModConfigSpec.Range<?> range = this.valueSpec.getRange();
            if(range != null)
            {
                this.range = Pair.of((T) range.getMin(), (T) range.getMax());
            }
            else
            {
                this.range = Pair.of(null, null);
            }
        }
    }
}
