# TrueType Shield

<img src="src/main/resources/assets/truetype_shield/icon.png" alt="TrueType Shield icon" width="96">

TrueType Shield is a client-side Fabric mod that mitigates the minecraft2shell resource-pack font attack. It replaces Minecraft's `ttf` font providers with empty providers before the game opens the font files or passes them to FreeType.

> [!WARNING]
> Server and local packs that rely on TrueType for custom text or icons may show fallback glyphs or missing characters. When the first font is skipped, TrueType Shield writes a warning to `logs/latest.log`. It warns once per launch.

## How it works

During a resource reload, the client reads each font definition and prepares its providers. A required Fabric mixin cancels the vanilla `ttf` provider's load method at entry. It returns a provider with zero glyphs, so the rest of the font definition can still supply glyphs. This applies to built-in pack loading for both server and local resource packs.

| Provider | What Minecraft reads | TrueType Shield |
| --- | --- | --- |
| `ttf` | A font file passed to FreeType | Blocked before file access, regardless of filename extension |
| `bitmap` | An image and a glyph grid | Available. Uses the image decoder |
| `unihex` | Zipped hexadecimal glyph rows | Available. Uses Minecraft's Unihex loader |
| `space` | Advance widths from the JSON definition | Available |
| `reference` | Another font definition | Available. Referenced `ttf` providers are still blocked |

The guard covers the vanilla TrueType resource-pack path. Other resource decoders have their own security boundaries.

## Install

Use Fabric Loader 0.20.1 or newer and place the matching JAR in your client's `mods` folder. Fabric API and server installation are unnecessary.

| JAR | Minecraft Java Edition | Java |
| --- | --- | --- |
| `truetype-shield-1.2.0+1.21.x.jar` | Every stable release from 1.21 through 1.21.11 | 21 |
| `truetype-shield-1.2.0+26.x.jar` | 26.1, 26.1.1, 26.1.2, 26.2, 26.3, 26.4 Pre-Release 1 | 25 |

The 26.4 compatibility entry covers Pre-Release 1. The final release will need its own compatibility check.

## Build

Use JDK 25 for both targets. On Windows, use `gradlew.bat`.

```sh
./gradlew build -Ptarget=1.21.x
./gradlew build -Ptarget=26.x
```

The distributable JARs are in `build/libs/`.

## License

TrueType Shield v1.2.0 and later are All Rights Reserved. [License notice](LICENSE)

The project icon uses Lucide's Shield Ban design. [Icon source](https://lucide.dev/icons/shield-ban) · [Third-party license](LICENSE-LUCIDE)
