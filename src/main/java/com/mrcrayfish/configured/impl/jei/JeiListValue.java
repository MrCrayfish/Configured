package com.mrcrayfish.configured.impl.jei;

import com.mrcrayfish.configured.client.screen.list.IListConfigValue;
import com.mrcrayfish.configured.client.screen.list.IListType;
import mezz.jei.api.runtime.config.IJeiConfigListValueSerializer;
import mezz.jei.api.runtime.config.IJeiConfigValue;
import mezz.jei.api.runtime.config.IJeiConfigValueSerializer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * Author: MrCrayfish
 */
public class JeiListValue<T> extends JeiValue<List<T>> implements IListConfigValue<T>
{
    public JeiListValue(IJeiConfigValue<List<T>> configValue)
    {
        super(configValue);
    }

    @Nullable
    @Override
    public IListType<T> getListType()
    {
        IJeiConfigValueSerializer<List<T>> serializer = this.configValue.getSerializer();
        if(serializer instanceof IJeiConfigListValueSerializer<T> listSerializer)
        {
            IJeiConfigValueSerializer<T> listValueSerializer = listSerializer.getListValueSerializer();
            return new JeiListType<>(listValueSerializer);
        }
        return null;
    }

    private record JeiListType<T>(IJeiConfigValueSerializer<T> serializer) implements IListType<T>
    {
        @Override
        public Function<T, String> getStringParser()
        {
            return this.serializer::serialize;
        }

        @Override
        public Function<String, T> getValueParser()
        {
            return s -> this.serializer.deserialize(s).getResult().orElse(null);
        }

        @Override
        public Component getHint()
        {
            return Component.literal(this.serializer.getValidValuesDescription());
        }
    }
}
