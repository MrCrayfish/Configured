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
import net.minecraft.core.Direction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.Arrays;
import java.util.List;

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

    @SimpleConfig(id = Constants.ID, name = "memory", type = ConfigType.MEMORY, readOnly = true)
    private static final MyConfig MEMORY = new MyConfig();

    public SimpleConfigTest()
    {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.addListener(this::onCommonSetup);
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

        @SimpleProperty(name = "test_double", comment = "A test double property")
        public final DoubleProperty testDouble = DoubleProperty.create(1.0, 0.0, 1.0);

        @SimpleProperty(name = "test_bool", comment = "A test boolean property")
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
}
