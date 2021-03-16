package com.mrcrayfish.configured;

import net.minecraft.util.Direction;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

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

        public Client(ForgeConfigSpec.Builder builder)
        {
            this.stringValue = builder.define("stringValue", "YEP");
            this.booleanValue = builder.define("booleanValue", false);
            builder.push("special");
            this.intValue = builder.defineInRange("intValue",0, 1, 10);
            this.doubleValue = builder.defineInRange("doubleValue", 0.0, 1.0, 10.0);
            this.longValue = builder.defineInRange("longValue", 0L, 1L, 10L);
            this.enumValue = builder.defineEnum("enumValue", Direction.NORTH);
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
