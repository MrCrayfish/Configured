package com.mrcrayfish.configured;

import net.minecraft.util.Direction;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
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
        public final ForgeConfigSpec.EnumValue<Direction> enumValue;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> listValue;

        public Client(ForgeConfigSpec.Builder builder)
        {
            this.stringValue = builder.comment("This is an String value").define("stringValue", "YEP");
            this.booleanValue = builder.comment("This is a Boolean value").define("booleanValue", false);
            this.listValue = builder.comment("This is a List of Strings").defineList("listValue", Arrays.asList("Yo", "Yes", "No"), o -> true);
            builder.comment("YEP").push("more_properties");
            this.intValue = builder.comment("This is an Integer value").defineInRange("int_Value",0, 1, 10);
            this.doubleValue = builder.comment("This is a Double value").defineInRange("doubleValue", 0.0, 1.0, 10.0);
            this.longValue = builder.comment("This is a Long value").defineInRange("longValue", 0L, 1L, 10L);
            this.enumValue = builder.comment("This is an Enum value").defineEnum("enumValue", Direction.NORTH);
            builder.pop();
        }
    }

    static final ForgeConfigSpec clientSpec;
    public static final Config.Client CLIENT;

    static
    {
        final Pair<Client, ForgeConfigSpec> clientSpecPair = new ForgeConfigSpec.Builder().configure(Config.Client::new);
        clientSpec = clientSpecPair.getRight();
        CLIENT = clientSpecPair.getLeft();
    }
}
