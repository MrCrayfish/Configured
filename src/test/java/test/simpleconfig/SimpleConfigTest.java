package test.simpleconfig;

import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.simple.DoubleProperty;
import com.mrcrayfish.configured.api.simple.IntProperty;
import com.mrcrayfish.configured.api.simple.SimpleConfig;
import com.mrcrayfish.configured.api.simple.SimpleProperty;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Author: MrCrayfish
 */
@Mod(value = Constants.ID)
public class SimpleConfigTest
{
    @SimpleConfig(id = Constants.ID, name = "client", storage = ConfigType.CLIENT)
    private static final MyConfig CLIENT = new MyConfig();

    @SimpleConfig(id = Constants.ID, name = "universal", storage = ConfigType.UNIVERSAL)
    private static final MyConfig UNIVERSAL = new MyConfig();

    @SimpleConfig(id = Constants.ID, name = "server", storage = ConfigType.SERVER)
    private static final MyConfig SERVER = new MyConfig();

    @SimpleConfig(id = Constants.ID, name = "server_sync", storage = ConfigType.SERVER_SYNC)
    private static final MyConfig SERVER_SYNC = new MyConfig();

    @SimpleConfig(id = Constants.ID, name = "dedicated_server", storage = ConfigType.DEDICATED_SERVER)
    private static final MyConfig DEDICATED_SERVER = new MyConfig();

    @SimpleConfig(id = Constants.ID, name = "world", storage = ConfigType.WORLD)
    private static final MyConfig WORLD = new MyConfig();

    @SimpleConfig(id = Constants.ID, name = "world_sync", storage = ConfigType.WORLD_SYNC)
    private static final MyConfig WORLD_SYNC = new MyConfig();

    @SimpleConfig(id = Constants.ID, name = "world_sync", storage = ConfigType.MEMORY)
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
        @SimpleProperty("more_properties")
        public final NestedConfig moreProperties = new NestedConfig();

        @SimpleProperty("test_double")
        public final DoubleProperty testDouble = DoubleProperty.create(1.0, 0.0, 1.0);

        public static class NestedConfig
        {
            @SimpleProperty("test_int")
            public final IntProperty testInt = IntProperty.create(1);
        }
    }
}
