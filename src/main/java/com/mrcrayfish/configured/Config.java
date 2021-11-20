package com.mrcrayfish.configured;

import net.minecraft.ChatFormatting;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.List;

/**
 * Author: MrCrayfish
 */
public class Config
{
    public static class Client
    {
        public final ForgeConfigSpec.ConfigValue<String> stringValue;
        public final ForgeConfigSpec.BooleanValue booleanValue;
        public final ForgeConfigSpec.IntValue intValue;
        public final ForgeConfigSpec.DoubleValue doubleValue;
        public final ForgeConfigSpec.LongValue longValue;
        public final ForgeConfigSpec.EnumValue<ChatFormatting> enumValue;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> listValue;

        public Client(ForgeConfigSpec.Builder builder)
        {
            this.stringValue = builder.comment("This is an String value").define("stringValue", "YEP");
            this.booleanValue = builder.comment("This is a Boolean value").define("booleanValue", false);
            this.listValue = builder.comment("This is a List of Strings").defineList("listValue", Collections.emptyList(), o -> true);
            builder.comment("YEP").push("more_properties");
            this.intValue = builder.comment("This is an Integer value").defineInRange("int_Value", 0, 0, 10);
            this.doubleValue = builder.comment("This is a Double value").defineInRange("doubleValue", 0.0, 0.0, 10.0);
            this.longValue = builder.comment("This is a Long value").defineInRange("longValue", 0L, 0L, 10L);
            this.enumValue = builder.comment("This is an Enum value").defineEnum("enumValue", ChatFormatting.BLACK);
            builder.pop();
        }
    }

    static final ForgeConfigSpec clientSpec;
    public static final Config.Client CLIENT;

    static final ForgeConfigSpec customSpec;
    public static final Config.Client CUSTOM;

    static
    {
        final Pair<Client, ForgeConfigSpec> clientSpecPair = new ForgeConfigSpec.Builder().configure(Config.Client::new);
        clientSpec = clientSpecPair.getRight();
        CLIENT = clientSpecPair.getLeft();

        final Pair<Client, ForgeConfigSpec> customSpecPair = new ForgeConfigSpec.Builder().configure(Config.Client::new);
        customSpec = customSpecPair.getRight();
        CUSTOM = customSpecPair.getLeft();
    }
}
