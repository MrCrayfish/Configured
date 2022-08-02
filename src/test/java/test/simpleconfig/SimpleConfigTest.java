package test.simpleconfig;

import com.mrcrayfish.configured.api.config.DoubleProperty;
import com.mrcrayfish.configured.api.config.IntProperty;
import com.mrcrayfish.configured.api.config.SimpleConfig;
import com.mrcrayfish.configured.api.config.SimpleProperty;
import com.mrcrayfish.configured.api.config.StorageType;
import net.minecraftforge.fml.common.Mod;

/**
 * Author: MrCrayfish
 */
@Mod(value = Constants.ID)
public class SimpleConfigTest
{
    @SimpleConfig(id = Constants.ID, name = "my_config", storage = StorageType.WORLD)
    private static final MyConfig CLIENT = new MyConfig();

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
