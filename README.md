![Configured Banner](https://i.imgur.com/2GR7I6q.png)

![Minecraft](https://img.shields.io/static/v1?label=&message=1.17%20|%201.16&color=2d2d2d&labelColor=dddddd&style=for-the-badge&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAAZdEVYdFNvZnR3YXJlAHBhaW50Lm5ldCA0LjAuMjCGJ1kDAAACoElEQVQ4T22SeU8aURTF/ULGtNRWWVQY9lXABWldIDPIMgVbNgEVtaa0damiqGBdipXaJcY2ofEf4ycbTt97pVAabzK5b27u+Z377kwXgK77QthRy7OfXbeJM+ttqKSXN8sdwbT/A0L7elmsYqrPHZmROLPh5YkV4oEBwaKuHj+yyJptLDoAhbq3O1V1XCVObY3FL24mfn5oRPrcwSCRfQOyNWcjVjZdCbtcdwcgXrXUspdOKbDN/XE9tiBJMhXHT60gUIT2dMhcDLMc3NVKQklz0QIkf5qlyEcO6Qs7yPhMJB4amDMFimQSmqNlE8SKAZFzDfxHfVILIIZ10sJ3OwIbcqSuiOjchkzNCboHev9o2YhgiUP8mxnLN24I6/3ghYdtQG5iUMpFBuCP9iKwLsfiLyeCp2rMnZgwX3NArGoxW1Ridl+BzLEVKa8KSxOqNmDdz0kFnxaLHhWEgAyZigWhHXL+pEDy2ozsDxv8vAzTnh7w5kcghqCaFmCT10of4iPIT2mRdPUh4HoCcVwBH/8Ac2kzUkEV5r3EfVSOvbAJa5NDyI0r2oDtWb1EClh+OoC3Pg7v/Bw7p939yI4rsRW2Y3lKh01eh7WpIRyKZqzyjjYgPdIvlaMWRqYuG7wWryYHsRM0sFolZiPvQ3jheIwSmSBPdkByG/B6Wi3RYiVmRX7GiAPiUCRisii8D+jZNKvPBrHCW1GY0bAz6WkDCtOaSyKQFsi4K5NqNiZtehN2Y5uAShETqolhBqJXpfdPuPsuWwAaRdHSkxdc11mPqkGnyY4pyKbpl1GyJ0Pel7yqBoFcF3zqno5f+d8ohYy9Sx7lzQpxo1eirluCDgt++00p6uxttrG4F/A39sJGZWZMfrcp6O6+5kaVzXJHAOj6DeSs8qw5o8oxAAAAAElFTkSuQmCC) [![Curseforge](http://cf.way2muchnoise.eu/full_configured_downloads.svg?badge_style=for_the_badge)](https://www.curseforge.com/minecraft/mc-mods/configured)

# Configured

Configured is a simple and lightweight mod that dynamically creates a configuration menu for every mod. Previously mods would use Forge's built-in GUI system however that no longer exists in newer versions. This mod aims to reintroduce that system and create fresh new experience with an updated easy-to-use layout. The best part is that Configured supports* every mod and doesn't require other mods to write extra code. Configured allows you to change client, common and server configurations! This mod can be safely added to any modpack without any problems.

*Configured only supports mods that use Forge's config system, anything custom will not work.*

## Features

* Supports editing client, common and server configurations. This can be done from the main menu or even in-game!
* Automatically supports every mod that utilises the Forge config system. No extra work needed mod developers!
* A simple and intuitive layout for quick modification of config values
* Adds a new keybinding to open the mod list from in-game without additional mods.
* Optional support for mod developers to set the background texture of their config menu (See below)
* Lightweight and just works! Add it to your modpack without any problems.

## Developers

This mod has support to change the background texture of the config screen. Just add `[modproperties.<yourmodid>]` to the bottom of your `mods.toml` then under it add an entry called `configuredBackground` set the value to `"minecraft:textures/block/stone.png"` or another location to a texture file. You do not need to make your mod depend on this mod, however on CurseForge I appreciate if you mark this mod as an optional dependency. See `mods.toml` in this repo for an example.

## Screenshots

![Screenshot 1](https://media.forgecdn.net/attachments/407/576/screenshot6.png)
![Screenshot 2](https://media.forgecdn.net/attachments/407/572/screenshot2.png)
![Screenshot 3](https://media.forgecdn.net/attachments/407/573/screenshot3.png)
