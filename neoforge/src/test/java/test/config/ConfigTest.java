package test.config;

import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

/**
 * Author: MrCrayfish
 */
@Mod("config_test")
public class ConfigTest
{
    static final ModConfigSpec testSpec;
    public static final Test TEST;

    static
    {
        final Pair<Test, ModConfigSpec> testSpecPair = new ModConfigSpec.Builder().configure(Test::new);
        testSpec = testSpecPair.getRight();
        TEST = testSpecPair.getLeft();
    }

    public ConfigTest(IEventBus bus)
    {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, testSpec, "configured_test_config.toml");
    }

    public static class Test
    {
        public final ModConfigSpec.ConfigValue<String> stringValue;
        public final ModConfigSpec.BooleanValue booleanValue;
        public final ModConfigSpec.IntValue intValue;
        public final ModConfigSpec.DoubleValue doubleValue;
        public final ModConfigSpec.LongValue longValue;
        public final ModConfigSpec.EnumValue<ChatFormatting> enumValue;
        public final ModConfigSpec.ConfigValue<List<? extends String>> stringList;
        public final ModConfigSpec.ConfigValue<List<? extends String>> listOfItems;
        public final ModConfigSpec.ConfigValue<List<? extends Integer>> intList;
        public final ModConfigSpec.ConfigValue<List<? extends Long>> longList;
        public final ModConfigSpec.ConfigValue<List<? extends Double>> doubleList;
        public final ModConfigSpec.EnumValue<ChatFormatting> restrictedEnums;

        public Test(ModConfigSpec.Builder builder)
        {
            this.stringValue = builder.comment("This is an String value").translation("forge_config.config_test.client.string_value").define("stringValue", "YEP");
            this.booleanValue = builder.comment("This is a Boolean value").define("booleanValue", false);
            builder.comment("YEP").push("more_properties");
            this.intValue = builder.comment("This is an Integer value").defineInRange("int_Value", 0, 0, 10);
            this.doubleValue = builder.comment("This is a Double value").defineInRange("doubleValue", 0.0, 0.0, 10.0);
            this.longValue = builder.comment("This is a Long value").defineInRange("longValue", 0L, 0L, 10L);
            this.enumValue = builder.comment("This is an Enum value").defineEnum("enumValue", ChatFormatting.BLACK);
            this.restrictedEnums = builder.comment("An enum value but with restricted values").defineEnum("restrictedEnums", ChatFormatting.RED, ChatFormatting.RED, ChatFormatting.GREEN, ChatFormatting.BLUE);
            builder.pop();
            builder.translation("forge_config.config_test.client.lists").push("lists");
            this.intList = builder.comment("This is an Integer list").defineList("intList", Arrays.asList(5, 10), o -> o instanceof Integer);
            this.longList = builder.comment("This is an Long list").defineList("longList", Arrays.asList(5L, 10L), o -> o instanceof Long);
            this.doubleList = builder.comment("This is an Double list").defineList("doubleList", Arrays.asList(0.5, 1.0), o -> o instanceof Double);
            this.stringList = builder.comment("This is a String list").defineList("stringList", Arrays.asList("test", "yo"), o -> o instanceof String);
            this.listOfItems = builder.comment("This is a List of Item Locations").defineList("listOfItems", Arrays.asList("minecraft:apple", "minecraft:iron_ingot"), o -> o instanceof String && ResourceLocation.isValidResourceLocation(o.toString()) && !new ResourceLocation(o.toString()).getPath().isEmpty());
            builder.pop();
        }
    }
}
