package test.simpleconfig;

import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.simple.BoolProperty;
import com.mrcrayfish.configured.api.simple.DoubleProperty;
import com.mrcrayfish.configured.api.simple.EnumProperty;
import com.mrcrayfish.configured.api.simple.IntProperty;
import com.mrcrayfish.configured.api.simple.ListProperty;
import com.mrcrayfish.configured.api.simple.LongProperty;
import com.mrcrayfish.configured.api.simple.SimpleConfig;
import com.mrcrayfish.configured.api.simple.SimpleProperty;
import com.mrcrayfish.configured.api.simple.StringProperty;
import com.mrcrayfish.configured.api.simple.event.SimpleConfigEvent;
import com.mrcrayfish.configured.api.simple.validate.Validator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Author: MrCrayfish
 */
@Mod(value = Constants.ID)
public class SimpleConfigTest
{
    @SimpleConfig(id = Constants.ID, name = "client", type = ConfigType.CLIENT, readOnly = true)
    private static final MyConfig CLIENT = new MyConfig();

    @SimpleConfig(id = Constants.ID, name = "universal", type = ConfigType.UNIVERSAL)
    private static final MyConfig UNIVERSAL = new MyConfig();

    @SimpleConfig(id = Constants.ID, name = "server", type = ConfigType.SERVER)
    private static final MyConfig SERVER = new MyConfig();

    @SimpleConfig(id = Constants.ID, name = "server_sync", type = ConfigType.SERVER_SYNC)
    private static final MyConfig SERVER_SYNC = new MyConfig();

    @SimpleConfig(id = Constants.ID, name = "dedicated_server", type = ConfigType.DEDICATED_SERVER)
    private static final MyConfig DEDICATED_SERVER = new MyConfig();

    @SimpleConfig(id = Constants.ID, name = "world", type = ConfigType.WORLD)
    private static final MyConfig WORLD = new MyConfig();

    @SimpleConfig(id = Constants.ID, name = "world_sync", type = ConfigType.WORLD_SYNC)
    private static final MyConfig WORLD_SYNC = new MyConfig();

    @SimpleConfig(id = Constants.ID, name = "memory", type = ConfigType.MEMORY)
    private static final MyConfig MEMORY = new MyConfig();

    @SimpleConfig(id = Constants.ID, name = "deep_config", type = ConfigType.UNIVERSAL)
    private static final DeepConfig DEEP_CONFIG = new DeepConfig();

    @SimpleConfig(id = Constants.ID, name = "validator", type = ConfigType.UNIVERSAL)
    private static final Validator VALIDATOR_CONFIG = new Validator();

    public SimpleConfigTest()
    {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.addListener(this::onCommonSetup);
        eventBus.addListener(this::onConfigLoad);
        eventBus.addListener(this::onConfigUnload);
        eventBus.addListener(this::onConfigReload);
    }

    private void onCommonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            //System.out.println("Test Double: " + CLIENT.testDouble.get());
            //System.out.println("Test Integer: " + CLIENT.moreProperties.testInt.get());
            //CLIENT.testDouble.set(0.1);
        });
    }

    public static class MyConfig
    {
        @SimpleProperty(name = "lists", comment = "Lists of properties")
        public final Lists lists = new Lists();

        @SimpleProperty(name = "more_properties", comment = "A sub config")
        public final NestedConfig moreProperties = new NestedConfig();

        @SimpleProperty(name = "test_double", comment = "A test double property", worldRestart = true)
        public final DoubleProperty testDouble = DoubleProperty.create(1.0, 0.0, 1.0);

        @SimpleProperty(name = "test_bool", comment = "A test boolean property", gameRestart = true)
        public final BoolProperty testBoolean = BoolProperty.create(false);

        @SimpleProperty(name = "test_enum", comment = "A test enum property")
        public final EnumProperty<Direction> testEnum = EnumProperty.create(Direction.NORTH);

        public static class NestedConfig
        {
            @SimpleProperty(name = "test_int", comment = "A test int property")
            public final IntProperty testInt = IntProperty.create(1);

            @SimpleProperty(name = "test_long", comment = "A test long property")
            public final LongProperty testLong = LongProperty.create(1L);

            @SimpleProperty(name = "test_string", comment = "A test string property")
            public final StringProperty testString = StringProperty.create("Hello!");

            @SimpleProperty(name = "test_list", comment = "A test list property")
            public final ListProperty<Integer> testList = ListProperty.create(ListProperty.INT, () -> Arrays.asList(1, 2, 3, 4));
        }

        public static class Lists
        {
            @SimpleProperty(name = "test_integer_list", comment = "A test integer list property")
            public final ListProperty<Integer> testIntegerList = ListProperty.create(ListProperty.INT, () -> Arrays.asList(1, 2, 3, 4));

            @SimpleProperty(name = "test_long_list", comment = "A test long list property")
            public final ListProperty<Long> testLongList = ListProperty.create(ListProperty.LONG, () -> Arrays.asList(1L, 2L, 3L, 4L));

            @SimpleProperty(name = "test_boolean_list", comment = "A test boolean list property")
            public final ListProperty<Boolean> testBooleanList = ListProperty.create(ListProperty.BOOL, () -> Arrays.asList(false, true, false, true));

            @SimpleProperty(name = "test_double_list", comment = "A test double list property")
            public final ListProperty<Double> testDoubleList = ListProperty.create(ListProperty.DOUBLE, () -> Arrays.asList(1.0, 2.0, 3.0, 4.0));

            @SimpleProperty(name = "test_string_list", comment = "A test string list property")
            public final ListProperty<String> testStringList = ListProperty.create(ListProperty.STRING, () -> Arrays.asList("1", "2", "3", "4"));
        }
    }

    public static class Validator
    {
        @SimpleProperty(name = "int_validation", comment = "A test int property with custom validation. The value must be one of the following: 1, 2, 4, 8, 16, or 32")
        public final IntProperty intValue = IntProperty.create(1, new com.mrcrayfish.configured.api.simple.validate.Validator<>()
        {
            private final Set<Integer> allowed = Set.of(1, 2, 4, 8, 16, 32);

            @Override
            public boolean test(Integer value)
            {
                return this.allowed.contains(value);
            }

            @Override
            public Component getHint()
            {
                return new TextComponent("Value must be one of the following: 1, 2, 4, 8, 16, or 32");
            }
        });

        @SimpleProperty(name = "double_validation", comment = "A test double property with custom validation. The value must be greater than/equal to 10.0 or less than/equal to -10.0")
        public final DoubleProperty doubleValue = DoubleProperty.create(10.0, new com.mrcrayfish.configured.api.simple.validate.Validator<>()
        {
            @Override
            public boolean test(Double value)
            {
                return value >= 10.0 || value <= -10.0;
            }

            @Override
            public Component getHint()
            {
                return new TextComponent("Value must be greater than/equal to 10.0 or less than/equal to -10.0");
            }
        });

        @SimpleProperty(name = "list_element_validation", comment = "A test list property with custom element validation. An element in the list must start with https://mrcrayfish.com/mods?id=")
        public final ListProperty<String> stringList = ListProperty.create(ListProperty.STRING, new com.mrcrayfish.configured.api.simple.validate.Validator<>()
        {
            @Override
            public boolean test(String value)
            {
                return value.startsWith("https://mrcrayfish.com/mods?id=");
            }

            @Override
            public Component getHint()
            {
                return new TextComponent("Value must start with ").append(new TextComponent("https://mrcrayfish.com/mods?id=").withStyle(ChatFormatting.YELLOW));
            }
        });

        @SimpleProperty(name = "long_validation", comment = "A test long property with custom validation. The value must divisible by two.")
        public final LongProperty longValue = LongProperty.create(0L, new com.mrcrayfish.configured.api.simple.validate.Validator<>()
        {
            @Override
            public boolean test(Long value)
            {
                return value % 2 == 0;
            }

            @Override
            public Component getHint()
            {
                return new TextComponent("Value must divisible by two");
            }
        });

        @SimpleProperty(name = "string_validation", comment = "A test string property with custom validation. The value can only contain lowercase letters or numbers.")
        public final StringProperty stringValue = StringProperty.create("hello", new com.mrcrayfish.configured.api.simple.validate.Validator<>()
        {
            private static final Predicate<String> NAME_PATTERN = Pattern.compile("^[a-z\\d]+$").asMatchPredicate();

            @Override
            public boolean test(String value)
            {
                return NAME_PATTERN.test(value);
            }

            @Override
            public Component getHint()
            {
                return new TextComponent("Value can only contain lowercase letters or numbers");
            }
        });

        @SimpleProperty(name = "restricted_enum", comment = "A test enum property")
        public final EnumProperty<Direction> testEnum = EnumProperty.create(Direction.NORTH, Set.of(Direction.UP, Direction.NORTH, Direction.DOWN));
    }

    public static class DeepConfig
    {
        @SimpleProperty(name = "once")
        public final Once once = new Once();

        public static class Once
        {
            @SimpleProperty(name = "upon")
            public final Upon upon = new Upon();

            public static class Upon
            {
                @SimpleProperty(name = "a")
                public final A a = new A();

                public static class A
                {
                    @SimpleProperty(name = "time")
                    public final Time time = new Time();

                    public static class Time
                    {
                        @SimpleProperty(name = "yo", comment = "What's up?", worldRestart = true)
                        public final DoubleProperty yo = DoubleProperty.create(1.0, 0.0, 1.0);
                    }
                }
            }
        }
    }

    public void onConfigLoad(SimpleConfigEvent.Load event)
    {
        System.out.println("Loaded config: " + event.getSource());
    }

    public void onConfigUnload(SimpleConfigEvent.Unload event)
    {
        System.out.println("Unloaded config: " + event.getSource());
    }

    public void onConfigReload(SimpleConfigEvent.Reload event)
    {
        System.out.println("Reloading config: " + event.getSource());
    }
}
