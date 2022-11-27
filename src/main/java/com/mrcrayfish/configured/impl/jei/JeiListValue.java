package com.mrcrayfish.configured.impl.jei;

import com.mrcrayfish.configured.client.screen.EditListScreen;
import mezz.jei.common.config.file.ConfigValue;
import mezz.jei.common.config.file.serializers.ChatFormattingSerializer;
import mezz.jei.common.config.file.serializers.ColorNameSerializer;
import mezz.jei.common.config.file.serializers.IConfigValueSerializer;
import mezz.jei.common.config.file.serializers.ListSerializer;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Author: MrCrayfish
 */
public class JeiListValue<T> extends JeiValue<List<T>> implements EditListScreen.ListTypeProvider
{
    private final EditListScreen.IListType listType;

    @SuppressWarnings("unchecked")
    public JeiListValue(ConfigValue<?> configValue)
    {
        super((ConfigValue<List<T>>) configValue);
        this.listType = this.determineType((ConfigValue<List<?>>) configValue);
    }

    @Override
    public boolean isValid(List<T> value)
    {
        if(this.configValue.getSerializer() instanceof ListSerializer<T> serializer)
        {
            JeiReflection.getListValueSerializer(serializer);
        }
        return true;
    }

    @Override
    @Nullable
    public EditListScreen.IListType getListType()
    {
        return this.listType;
    }

    @Nullable
    private EditListScreen.IListType determineType(ConfigValue<List<?>> configValue)
    {
        IConfigValueSerializer<?> serializer = configValue.getSerializer();
        if(serializer instanceof ListSerializer<?> listSerializer)
        {
            serializer = JeiReflection.getListValueSerializer(listSerializer);
        }
        if(serializer instanceof ChatFormattingSerializer)
        {
            return JeiListTypes.JEI_CHAT_FORMATTING;
        }
        else if(serializer instanceof ColorNameSerializer)
        {
            return JeiListTypes.JEI_COLOR_NAME;
        }
        return null;
    }
}
