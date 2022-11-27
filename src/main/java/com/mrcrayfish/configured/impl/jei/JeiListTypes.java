package com.mrcrayfish.configured.impl.jei;

import com.mrcrayfish.configured.client.screen.EditListScreen;
import mezz.jei.common.color.ColorName;
import mezz.jei.common.config.file.serializers.ChatFormattingSerializer;
import mezz.jei.common.config.file.serializers.ColorNameSerializer;
import mezz.jei.common.config.file.serializers.DeserializeResult;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.function.Function;

/**
 * Author: MrCrayfish
 */
public final class JeiListTypes
{
    public static final EditListScreen.IListType JEI_CHAT_FORMATTING = new EditListScreen.IListType()
    {
        @Override
        public Function<Object, String> getStringParser()
        {
            return o -> o instanceof ChatFormatting formatting ? formatting.getName() : "";
        }

        @Override
        public Function<String, ?> getValueParser()
        {
            return ChatFormatting::getByName;
        }

        @Override
        public Component getHint()
        {
            return Component.literal(ChatFormattingSerializer.INSTANCE.getValidValuesDescription());
        }
    };

    public static final EditListScreen.IListType JEI_COLOR_NAME = new EditListScreen.IListType()
    {
        @Override
        public Function<Object, String> getStringParser()
        {
            return o -> ColorNameSerializer.INSTANCE.serialize((ColorName) o);
        }

        @Override
        public Function<String, ?> getValueParser()
        {
            return s ->
            {
                DeserializeResult<ColorName> result = ColorNameSerializer.INSTANCE.deserialize(s);
                return result.getResult() != null ? result.getResult() : null;
            };
        }

        @Override
        public Component getHint()
        {
            return Component.literal(ColorNameSerializer.INSTANCE.getValidValuesDescription());
        }
    };
}
