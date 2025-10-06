package test.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Author: MrCrayfish
 */
@Mod("config_test")
public class ConfigTest
{
    static final ModConfigSpec clientSpec;
    public static final Test CLIENT_TEST;

    static final ModConfigSpec commonSpec;
    public static final Test COMMON_TEST;

    static final ModConfigSpec startupSpec;
    public static final Test STARTUP_TEST;

    static final ModConfigSpec serverSpec;
    public static final Test SERVER_TEST;

    static
    {
        final Pair<Test, ModConfigSpec> clientSpecPair = new ModConfigSpec.Builder().configure(Test::new);
        clientSpec = clientSpecPair.getRight();
        CLIENT_TEST = clientSpecPair.getLeft();

        final Pair<Test, ModConfigSpec> commonSpecPair = new ModConfigSpec.Builder().configure(Test::new);
        commonSpec = commonSpecPair.getRight();
        COMMON_TEST = commonSpecPair.getLeft();

        final Pair<Test, ModConfigSpec> startupSpecPair = new ModConfigSpec.Builder().configure(Test::new);
        startupSpec = startupSpecPair.getRight();
        STARTUP_TEST = startupSpecPair.getLeft();

        final Pair<Test, ModConfigSpec> serverSpecPair = new ModConfigSpec.Builder().configure(Test::new);
        serverSpec = serverSpecPair.getRight();
        SERVER_TEST = serverSpecPair.getLeft();
    }

    public ConfigTest(ModContainer container, IEventBus bus)
    {
        container.registerConfig(ModConfig.Type.CLIENT, clientSpec, "configured_test_config.toml");
        container.registerConfig(ModConfig.Type.COMMON, commonSpec, "configured_test_common.toml");
        container.registerConfig(ModConfig.Type.STARTUP, startupSpec, "configured_test_startup.toml");
        container.registerConfig(ModConfig.Type.SERVER, serverSpec, "configured_test_server.toml");
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
        public final ModConfigSpec.ConfigValue<List<? extends ArrayList<String>>> unsupportedList;

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
            this.intList = builder.comment("This is an Integer list").defineList("intList", Arrays.asList(5, 10), () -> 5, o -> o instanceof Integer);
            this.longList = builder.comment("This is an Long list").defineList("longList", Arrays.asList(5L, 10L), () -> 5L, o -> o instanceof Long || o instanceof Integer);
            this.doubleList = builder.comment("This is an Double list").defineList("doubleList", Arrays.asList(0.5, 1.0), () -> 0.5, o -> o instanceof Double);
            this.stringList = builder.comment("This is a String list").defineList("stringList", Arrays.asList("test", "yo"), () -> "", o -> o instanceof String);
            this.listOfItems = builder.comment("This is a List of Item Locations").defineList("listOfItems", Arrays.asList("minecraft:apple", "minecraft:iron_ingot"), () -> "", o -> {
                return o instanceof String && ResourceLocation.tryParse(o.toString()) != null && !ResourceLocation.parse(o.toString()).getPath().isEmpty();
            });
            this.unsupportedList = builder.comment("This is an unsupported list").defineList("unsupportedList", List.of(new ArrayList<>()), ArrayList::new, o -> o instanceof ArrayList<?>);
            builder.pop();
        }
    }
}
