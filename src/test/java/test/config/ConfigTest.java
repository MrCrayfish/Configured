package test.config;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

/**
 * Author: MrCrayfish
 */
@Mod("config_test")
public class ConfigTest
{
    static final ForgeConfigSpec testSpec;
    public static final Test TEST;

    static
    {
        final Pair<Test, ForgeConfigSpec> testSpecPair = new ForgeConfigSpec.Builder().configure(Test::new);
        testSpec = testSpecPair.getRight();
        TEST = testSpecPair.getLeft();
    }

    public ConfigTest()
    {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, testSpec, "configured_test_config.toml");
    }

    public static class Test
    {
        public final ForgeConfigSpec.ConfigValue<String> stringValue;
        public final ForgeConfigSpec.BooleanValue booleanValue;
        public final ForgeConfigSpec.IntValue intValue;
        public final ForgeConfigSpec.DoubleValue doubleValue;
        public final ForgeConfigSpec.LongValue longValue;
        public final ForgeConfigSpec.EnumValue<ChatFormatting> enumValue;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> stringList;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> listOfItems;
        public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> intList;
        public final ForgeConfigSpec.ConfigValue<List<? extends Long>> longList;
        public final ForgeConfigSpec.ConfigValue<List<? extends Double>> doubleList;

        public Test(ForgeConfigSpec.Builder builder)
        {
            this.stringValue = builder.comment("This is an String value").define("stringValue", "YEP");
            this.booleanValue = builder.comment("This is a Boolean value").define("booleanValue", false);
            builder.comment("YEP").push("more_properties");
            this.intValue = builder.comment("This is an Integer value").defineInRange("int_Value", 0, 0, 10);
            this.doubleValue = builder.comment("This is a Double value").defineInRange("doubleValue", 0.0, 0.0, 10.0);
            this.longValue = builder.comment("This is a Long value").defineInRange("longValue", 0L, 0L, 10L);
            this.enumValue = builder.comment("This is an Enum value").defineEnum("enumValue", ChatFormatting.BLACK);
            builder.pop();
            builder.push("lists");
            this.intList = builder.comment("This is an Integer list").defineList("intList", Arrays.asList(5, 10), o -> o instanceof Integer);
            this.longList = builder.comment("This is an Long list").defineList("longList", Arrays.asList(5L, 10L), o -> o instanceof Long);
            this.doubleList = builder.comment("This is an Double list").defineList("doubleList", Arrays.asList(0.5, 1.0), o -> o instanceof Double);
            this.stringList = builder.comment("This is a String list").defineList("stringList", Arrays.asList("test", "yo"), o -> o instanceof String);
            this.listOfItems = builder.comment("This is a List of Item Locations").defineList("listOfItems", Arrays.asList("minecraft:apple", "minecraft:iron_ingot"), o -> o instanceof String && ResourceLocation.isValidResourceLocation(o.toString()) && !new ResourceLocation(o.toString()).getPath().isEmpty());
            builder.pop();
        }
    }
}
