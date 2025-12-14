package test.config;

import com.mrcrayfish.framework.api.config.BoolProperty;
import com.mrcrayfish.framework.api.config.ConfigProperty;
import com.mrcrayfish.framework.api.config.ConfigType;
import com.mrcrayfish.framework.api.config.FrameworkConfig;

/**
 * Author: MrCrayfish
 */
public class FrameworkConfigTest
{
    @FrameworkConfig(id = "config_test", name = "client", type = ConfigType.CLIENT)
    public static final Test CLIENT = new Test();

    @FrameworkConfig(id = "config_test", name = "universal", type = ConfigType.UNIVERSAL)
    public static final Test UNIVERSAL = new Test();

    @FrameworkConfig(id = "config_test", name = "server", type = ConfigType.SERVER)
    public static final Test SERVER = new Test();

    @FrameworkConfig(id = "config_test", name = "server_sync", type = ConfigType.SERVER_SYNC)
    public static final Test SERVER_SYNC = new Test();

    @FrameworkConfig(id = "config_test", name = "world", type = ConfigType.WORLD)
    public static final Test WORLD = new Test();

    @FrameworkConfig(id = "config_test", name = "world_sync", type = ConfigType.WORLD_SYNC)
    public static final Test WORLD_SYNC = new Test();

    @FrameworkConfig(id = "config_test", name = "dedicated_server", type = ConfigType.DEDICATED_SERVER)
    public static final Test DEDICATED_SERVER = new Test();

    @FrameworkConfig(id = "config_test", name = "memory", type = ConfigType.MEMORY)
    public static final Test MEMORY = new Test();

    @FrameworkConfig(id = "config_test", name = "client_read_only", type = ConfigType.CLIENT, readOnly = true)
    public static final Test CLIENT_READ_ONLY = new Test();

    @FrameworkConfig(id = "config_test", name = "universal_read_only", type = ConfigType.UNIVERSAL, readOnly = true)
    public static final Test UNIVERSAL_READ_ONLY = new Test();

    @FrameworkConfig(id = "config_test", name = "server_read_only", type = ConfigType.SERVER, readOnly = true)
    public static final Test SERVER_READ_ONLY = new Test();

    @FrameworkConfig(id = "config_test", name = "server_sync_read_only", type = ConfigType.SERVER_SYNC, readOnly = true)
    public static final Test SERVER_SYNC_READ_ONLY = new Test();

    @FrameworkConfig(id = "config_test", name = "world_read_only", type = ConfigType.WORLD, readOnly = true)
    public static final Test WORLD_READ_ONLY = new Test();

    @FrameworkConfig(id = "config_test", name = "world_sync_read_only", type = ConfigType.WORLD_SYNC, readOnly = true)
    public static final Test WORLD_SYNC_READ_ONLY = new Test();

    @FrameworkConfig(id = "config_test", name = "dedicated_server_read_only", type = ConfigType.DEDICATED_SERVER, readOnly = true)
    public static final Test DEDICATED_SERVER_READ_ONLY = new Test();

    public static class Test
    {
        @ConfigProperty(name = "test", comment = "Hello", gameRestart = true)
        public final BoolProperty test = BoolProperty.create(false);
    }
}
