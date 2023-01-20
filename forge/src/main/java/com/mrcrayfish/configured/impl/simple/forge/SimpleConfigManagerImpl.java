package com.mrcrayfish.configured.impl.simple.forge;

import com.mrcrayfish.configured.api.simple.SimpleConfig;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class SimpleConfigManagerImpl {
    private static final Type SIMPLE_CONFIG = Type.getType(SimpleConfig.class);

    public static List<Pair<SimpleConfig, Object>> getForgeSpecificConfigs() {
        List<ModFileScanData.AnnotationData> annotations = ModList.get().getAllScanData().stream().map(ModFileScanData::getAnnotations).flatMap(Collection::stream).filter(a -> SIMPLE_CONFIG.equals(a.annotationType())).toList();
        List<Pair<SimpleConfig, Object>> configs = new ArrayList<>();
        annotations.forEach(data ->
        {
            try
            {
                Class<?> configClass = Class.forName(data.clazz().getClassName());
                Field field = configClass.getDeclaredField(data.memberName());
                field.setAccessible(true);
                Object object = field.get(null);
                Optional.ofNullable(field.getDeclaredAnnotation(SimpleConfig.class)).ifPresent(simpleConfig -> {
                    configs.add(Pair.of(simpleConfig, object));
                });
            }
            catch(NoSuchFieldException | ClassNotFoundException | IllegalAccessException e)
            {
                throw new RuntimeException(e);
            }
        });
        return configs;
    }
}
