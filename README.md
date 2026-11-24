# TrueType Shield

TrueType Shield is a client-side Fabric mod that mitigates the minecraft2shell resource-pack font attack. It replaces Minecraft's `ttf` font providers with empty providers before the game opens the font files or passes them to FreeType.

Server and local resource packs still load. Bitmap, Unihex, space, and reference providers remain available. A pack that relies on TrueType for text or icons may show fallback glyphs or missing characters.

## Install

Use Fabric Loader 0.20.1 or newer and place the matching JAR in your client's `mods` folder. Fabric API and server installation are unnecessary.

| JAR | Minecraft Java Edition | Java |
| --- | --- | --- |
| `truetype-shield-1.0.0+1.21.11.jar` | 1.21.11 | 21 |
| `truetype-shield-1.0.0+26.x.jar` | 26.1, 26.1.1, 26.1.2, 26.2, 26.3, 26.4 Pre-Release 1 | 25 |

The 26.4 compatibility entry covers Pre-Release 1. The final release will need its own compatibility check.

## Build

Use JDK 25 for both targets. On Windows, use `gradlew.bat`.

```sh
./gradlew build -Ptarget=1.21.11
./gradlew build -Ptarget=26.x
```

The distributable JARs are in `build/libs/`.
