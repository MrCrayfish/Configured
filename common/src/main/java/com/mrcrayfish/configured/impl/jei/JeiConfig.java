package com.mrcrayfish.configured.impl.jei;

import com.mrcrayfish.configured.api.*;
import com.mrcrayfish.configured.util.ConfigHelper;
import mezz.jei.api.runtime.config.IJeiConfigCategory;
import mezz.jei.api.runtime.config.IJeiConfigFile;
import mezz.jei.api.runtime.config.IJeiConfigValue;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
    public ConfigType getType()
    {
        return this.type;
    }

    @Override
    public String getFileName()
    {
        return this.configFile.getPath().getFileName().toString();
    }

    @Override
    public String getModId()
    {
        return "jei";
    }

    @Override
    public IConfigEntry createRootEntry()
    {
        return new JeiCategoryListEntry(this.name, this.categories);
    }

    @Override
    public ActionResult update(IConfigEntry entry)
    {
        ConfigHelper.getChangedValues(entry)
            .stream()
            .filter(JeiValue.class::isInstance)
            .map(JeiValue.class::cast)
            .forEach(JeiValue::updateConfigValue);
        return ActionResult.success();
    }

    @Override
    public ActionResult canPlayerEdit(Player player)
    {
        ExecutionContext context = new ExecutionContext(player);
        if(context.isClient())
        {
            if(context.isMainMenu() || context.isLocalPlayer())
            {
                return ActionResult.success();
            }
        }
        return ActionResult.fail();
    }

    @Override
    public boolean isChanged()
    {
        return this.categories.stream().anyMatch(category -> {
            return category.getConfigValues().stream().anyMatch(value -> {
                return !Objects.equals(value.getValue(), value.getDefaultValue());
            });
        });
    }

    @Override
    public Optional<Runnable> restoreDefaultsTask()
    {
        return Optional.of(() -> {
            this.categories.forEach(category -> {
                category.getConfigValues().forEach(JeiConfig::restoreDefaultValue);
            });
        });
    }

    private static <T> void restoreDefaultValue(IJeiConfigValue<T> value)
    {
        value.set(value.getDefaultValue());
    }
}
