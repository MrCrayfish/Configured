![Configured_Banner](https://github.com/MrCrayfish/Configured/assets/4958241/d9ddfa31-7ff3-4489-b4b2-77723e0e361f)


![Forge](https://github.com/MrCrayfish/Configured/assets/4958241/5f1efaa6-2649-43e3-83aa-3c7891d922bc) ![Fabric](https://github.com/MrCrayfish/Configured/assets/4958241/47926b83-c53a-4074-a2ca-fdcfb9eba1fc) ![_x__xampp_htdocs_mrcrayfish (3)](https://github.com/MrCrayfish/Configured/assets/4958241/e9b19f76-71af-4903-8059-bce00b5c55ce) [![Download on Website](https://github.com/MrCrayfish/Configured/assets/4958241/460bd882-4918-4241-a0be-c701f71b7f07)](https://mrcrayfish.com/mods/configured) [![Curseforge](http://cf.way2muchnoise.eu/full_configured_downloads.svg?badge_style=for_the_badge)](https://www.curseforge.com/minecraft/mc-mods/configured)
 


# Configured

Configured is a simple and lightweight mod that dynamically creates configuration menus for every mod with a supported config system. The mod was initially created due to the removal of Forge's built-in GUI system but it has now expnaded in features and supported modloaders. Configured aims to make it easier for players to modify the configurations of mods by providing a beautiful GUI and an intuitive experience. The best part is that Configured doesn't need other mods to write extra code in order for a menu to be generated, assuming they are using a supported config system. This mod can be safely added to any modpack without any problems.

## Features

* Allows you to edit mod configurations directly in-game. This makes it easy to quickly modify properties!
* Automatically supports every mod that utilise the supported config systems. No extra work needed mod developers!
* A beautiful GUI design with a focus on creating an intuitive experience.
* Adds a new keybinding to open the mod list from in-game without additional mods.
* Optional support for mod developers to set the background texture of their config menu (See below)
* Lightweight and just works! Add it to your modpack without any problems.

## Supported Config Systems
* Forge (all mods using either client, common, and/or world configs)
* NeoForge (all mods using client, common, and/or world configs)
* Framework (all mods using client, common, server, and/or world configs)
* JEI (Supports editing clients configs)

## Developers

This mod has support to change the background texture of the config screen through the mod's metadata. Below you can find an example for each modloader.

**Forge/NeoForge** - Add this under your `[[mods]]` section in your `mods.toml`
```toml
[modproperties.<your_mod_id>]
  configuredBackground="minecraft:textures/block/stone.png"
```

**Fabric** - Merge this JSON into your `fabric.mod.json`
```json
{
  "custom": {
    "configured": {
      "background": "minecraft:textures/block/stone.png"
    }
  }
}
```

## Screenshots
![Screenshot6](https://user-images.githubusercontent.com/4958241/141668789-094a0da7-92a0-404b-ac93-c4d1a8d3e7bf.png)
![Screenshot2](https://user-images.githubusercontent.com/4958241/141668794-655e84fd-5399-4d6d-a754-4e124f904115.png)
![Screenshot3](https://user-images.githubusercontent.com/4958241/141668796-fd10121d-7b5e-4330-a2bc-5779a0d96e53.png)
