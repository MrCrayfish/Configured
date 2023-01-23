package com.mrcrayfish.configured.impl.jei;

import com.mrcrayfish.configured.api.ConfigType;
import com.mrcrayfish.configured.api.IConfigEntry;
import com.mrcrayfish.configured.api.IModConfig;
import com.mrcrayfish.configured.util.ConfigHelper;
import mezz.jei.api.runtime.config.IJeiConfigCategory;
import mezz.jei.api.runtime.config.IJeiConfigFile;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Author: MrCrayfish
 */
public class JeiConfig implements IModConfig
{
    private final String name;
    private final ConfigType type;
    private final List<? extends IJeiConfigCategory> categories;
    private final IJeiConfigFile configFile;

    public JeiConfig(String name, ConfigType type, IJeiConfigFile configFile)
    {
        this.name = name;
        this.type = type;
        this.categories = configFile.getCategories();
        this.configFile = configFile;
    }

    @Override
    public void update(IConfigEntry entry)
    {
        ConfigHelper.getChangedValues(entry)
                .stream()
                .filter(JeiValue.class::isInstance)
                .map(JeiValue.class::cast)
                .forEach(JeiValue::updateConfigValue);
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
        return this.configFile
                .getPath()
                .getFileName()
                .toString();
    }

    @Override
    public String getModId()
    {
        return "jei";
    }

    @Override
    public void loadWorldConfig(Path path, Consumer<IModConfig> result)
    {
    }

}
