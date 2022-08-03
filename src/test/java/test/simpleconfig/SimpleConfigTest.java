package test.simpleconfig;

import com.mrcrayfish.configured.api.config.DoubleProperty;
import com.mrcrayfish.configured.api.config.IntProperty;
import com.mrcrayfish.configured.api.config.SimpleConfig;
import com.mrcrayfish.configured.api.config.SimpleProperty;
import com.mrcrayfish.configured.api.config.StorageType;
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
    @SimpleConfig(id = Constants.ID, name = "my_config", storage = StorageType.WORLD)
    private static final MyConfig CLIENT = new MyConfig();

    public SimpleConfigTest()
    {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.addListener(this::onCommonSetup);
    }

    private void onCommonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            System.out.println("Test Double: " + CLIENT.testDouble.get());
            System.out.println("Test Integer: " + CLIENT.moreProperties.testInt.get());
            CLIENT.testDouble.set(0.1);
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
