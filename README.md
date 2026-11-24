# TrueType Shield

<img src="src/main/resources/assets/truetype_shield/icon.png" alt="TrueType Shield icon" width="96">

TrueType Shield is a client-side mod for Fabric and NeoForge that mitigates the minecraft2shell resource-pack font attack. It replaces Minecraft's `ttf` font providers with empty providers before the game opens or decodes the font files.

> [!WARNING]
> Server and local packs that rely on TrueType for custom text or icons may show fallback glyphs or missing characters. When the first font is skipped, TrueType Shield writes a warning to `logs/latest.log`. It warns once per launch.

## How it works

During a resource reload, the client reads each font definition and prepares its providers. A required mixin cancels the vanilla `ttf` provider's load method at entry. It returns a provider with zero glyphs, so the rest of the font definition can still supply glyphs. This applies to built-in pack loading for both server and local resource packs.

| Provider | What Minecraft reads | TrueType Shield |
| --- | --- | --- |
| `ttf` | A font file passed to FreeType or STB, depending on Minecraft version | Blocked before file access, regardless of filename extension |
| `bitmap` | An image and a glyph grid | Available. Uses the image decoder |
| `unihex` | Zipped hexadecimal glyph rows | Available. Uses Minecraft's Unihex loader |
| `space` | Advance widths from the JSON definition | Available |
| `reference` | Another font definition | Available. Referenced `ttf` providers are still blocked |

The guard covers the vanilla TrueType resource-pack path. Other resource decoders have their own security boundaries.

## Local pack scanning

Open **Options → Resource Packs → Font settings** to run a scan or change the
`scanLocalPacksOnStartup` setting. Startup scanning is enabled by default and
the preference is stored in `config/truetype-shield.properties`.
Fabric players who have Mod Menu can also open the same screen from the gear
button next to TrueType Shield.

The scan checks ZIPs and folders directly under `resourcepacks`. It reports
outline font files and `ttf` declarations, including declarations that point
to files with a different extension. It compares candidate files with bundled
SHA-256 fingerprints from the published minecraft2shell archive, so a match can
be reported even when the original ZIP has been repacked. An ordinary font
notice means the pack may lose glyphs. A known fingerprint match is a separate
warning. Results, including incomplete scans, appear in `logs/latest.log`.

Scanning is read-only and runs off the client thread. It uses limits on pack,
entry, and decompressed read sizes and does not hand font contents to a font
decoder. The runtime `ttf` guard stays active regardless of scan settings.

## Install

Place the JAR for your loader and Minecraft version in your client's `mods` folder.
The Fabric files require Fabric Loader 0.20.1 or newer. The NeoForge files require
a matching NeoForge installation. Fabric API is optional for TrueType Shield.

| JAR | Minecraft Java Edition | Java |
| --- | --- | --- |
| Fabric `+1.20.x.jar` | Every stable release from 1.20 through 1.20.6 | 17 through 1.20.4, 21 on 1.20.5–1.20.6 |
| Fabric `+1.21.x.jar` | Every stable release from 1.21 through 1.21.11 | 21 |
| Fabric `+26.x.jar` | 26.1, 26.1.1, 26.1.2, 26.2, 26.3, 26.4 Pre-Release 1 | 25 |
| NeoForge `+neoforge.1.21.x.jar` | 1.21 through 1.21.11 | 21 |
| NeoForge `+neoforge.26.x.jar` | 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | 25 |

Minecraft 1.20 through 1.20.4 use STB for TrueType fonts. The 1.20.x build blocks
the pack's `ttf` provider before it reaches either STB or FreeType.

The 26.4 Pre-Release 1 compatibility entry is Fabric-only. Some of the supported
NeoForge Minecraft versions require a NeoForge beta build.

## Build

Use JDK 25 to build any target. On Windows, use `gradlew.bat`.

```sh
./gradlew build -Ptarget=1.20.x
./gradlew build -Ptarget=1.21.x
./gradlew build -Ptarget=26.x
```

The distributable JARs are in `build/libs/`.

Build NeoForge separately. Use JDK 25 for 26.x and JDK 21 for 1.21.x:

```sh
./gradlew -p neoforge build -Ptarget=26.x
./gradlew -p neoforge build -Ptarget=1.21.x
```

NeoForge JARs are in `neoforge/build/libs/`.

## Regression test

Run `./gradlew fontGuardRegression -Ptarget=1.20.x`, `-Ptarget=1.21.x`, or
`-Ptarget=26.x`. Each run loads the distributable
JAR through production Fabric and checks that a fixture pack's TrueType font
is blocked before lookup while its bitmap and Unihex glyphs still load.
The 1.20.x regression run uses a Java 17 toolchain.
GitHub Actions runs the check for every Fabric target and builds both NeoForge
targets before uploading artifacts.

## License

TrueType Shield v1.2.0 and later are All Rights Reserved. [License notice](LICENSE)

The project icon uses Lucide's Shield Ban design. [Icon source](https://lucide.dev/icons/shield-ban) · [Third-party license](LICENSE-LUCIDE)
