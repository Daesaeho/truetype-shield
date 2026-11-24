// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved
package io.github.daesaeho.truetypeshield.regression;

import com.mojang.blaze3d.font.GlyphProvider;
import io.github.daesaeho.truetypeshield.scan.PackScannerRegression;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.minecraft.client.gui.font.providers.GlyphProviderDefinition;
import net.minecraft.server.packs.resources.MultiPackResourceManager;

public final class FontProviderRegression implements PreLaunchEntrypoint {
    static final String NAMESPACE = "shield_regression";
    static final String FONT_PATH = "font/mixed.json";
    static final String BLOCKED_FONT_PATH = "font/unopened.ttf";

    @Override
    public void onPreLaunch() {
        String test = "verifyMixedFontPack";
        try {
            verifyMixedFontPack();
            System.out.println("PASS FontProviderRegression.verifyMixedFontPack");
            test = "verifyInstalledPacks";
            PackScannerRegression.verifyInstalledPacks();
            System.out.println("PASS PackScannerRegression.verifyInstalledPacks");
            System.exit(0);
        } catch (IOException | RuntimeException | AssertionError failure) {
            System.err.println("FontProviderRegression > " + test + " FAILED");
            failure.printStackTrace();
            System.exit(1);
        }
    }

    private static void verifyMixedFontPack() throws IOException {
        Path fixture = Path.of("fixture");
        writeFixture(fixture);
        try (var resources = RegressionPack.open(fixture)) {
            var providers = RegressionPack.readProviders(resources);
            require(providers.size() == 3, "Expected the fixture's TrueType, bitmap, and Unihex providers");
            try (var blocked = load(providers.get(0), resources);
                 var bitmap = load(providers.get(1), resources);
                 var unihex = load(providers.get(2), resources)) {
                require(blocked.getSupportedGlyphs().isEmpty() && blocked.getGlyph('B') == null,
                    "The blocked provider must leave glyphs available for fallback");
                require(bitmap.getSupportedGlyphs().contains('B') && bitmap.getGlyph('B') != null,
                    "The bitmap provider must still supply its glyph");
                require(unihex.getSupportedGlyphs().contains('U') && unihex.getGlyph('U') != null,
                    "The Unihex provider must still supply its glyph");
            }
        }
    }

    static void checkLookup(String namespace, String path) {
        require(!(NAMESPACE.equals(namespace) && BLOCKED_FONT_PATH.equals(path)),
            "The TrueType provider attempted to look up its font file");
    }

    private static GlyphProvider load(GlyphProviderDefinition provider, MultiPackResourceManager resources)
            throws IOException {
        return provider.unpack().left().orElseThrow().load(resources);
    }

    private static void writeFixture(Path fixture) throws IOException {
        Path assets = Files.createDirectories(fixture.resolve("assets").resolve(NAMESPACE));
        Path font = Files.createDirectories(assets.resolve("font"));
        Path textures = Files.createDirectories(assets.resolve("textures"));
        Files.writeString(font.resolve("mixed.json"), """
            {"providers":[
              {"type":"ttf","file":"shield_regression:unopened.ttf","size":11,"oversample":1,"shift":[0,0]},
              {"type":"bitmap","file":"shield_regression:glyph.png","height":8,"ascent":7,"chars":["B"]},
              {"type":"unihex","hex_file":"shield_regression:glyphs.zip","size_overrides":[]}
            ]}
            """);

        var image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xffffffff);
        require(ImageIO.write(image, "png", textures.resolve("glyph.png").toFile()), "Missing PNG writer");

        try (var zip = new ZipOutputStream(Files.newOutputStream(assets.resolve("glyphs.zip")))) {
            zip.putNextEntry(new ZipEntry("fixture.hex"));
            zip.write(("0055:" + "7E" + "42".repeat(13) + "7E00\n").getBytes(StandardCharsets.US_ASCII));
            zip.closeEntry();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
