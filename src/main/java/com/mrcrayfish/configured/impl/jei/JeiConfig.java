package com.mrcrayfish.configured.impl.jei;

import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IConfigValue;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.util.ConfigHelper;
import mezz.jei.common.config.file.ConfigCategory;
import mezz.jei.common.config.file.ConfigSchema;
import mezz.jei.common.config.file.ConfigSerializer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Author: MrCrayfish
 */
public class JeiConfig implements IModConfig
{
    private final String name;
    private final ConfigType type;
    private final Map<String, ConfigCategory> categories;
    private final Path path;

    public JeiConfig(String name, ConfigType type, ConfigSchema schema)
    {
        this.name = name;
        this.type = type;
        this.categories = JeiReflection.getConfigCategories(schema);
        this.path = JeiReflection.getConfigPath(schema);
    }

    @Override
    public void update(IConfigEntry entry)
    {
        Set<IConfigValue<?>> changedValues = ConfigHelper.getChangedValues(entry);
        changedValues.forEach(value ->
        {
            if(value instanceof JeiValue jeiValue)
            {
                JeiReflection.setJeiValue(jeiValue.getConfigValue(), jeiValue.get());
            }
        });

        try
        {
            // JEI doesn't save config when memory value is changed, so have to manually save.
            ConfigSerializer.save(this.path, this.categories.values());
        }
        catch(IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IConfigEntry getRoot()
    {
        return new JeiCategoryListEntry(this.name, this.categories);
    }

    @Override
    public ConfigType getType()
    {
        return this.type;
    }

    @Override
    public String getFileName()
    {
        return this.path.getFileName().toString();
    }

    @Override
    public String getModId()
    {
        return "jei";
    }

    @Override
    public void loadWorldConfig(Path path, Consumer<IModConfig> result) {}

}
