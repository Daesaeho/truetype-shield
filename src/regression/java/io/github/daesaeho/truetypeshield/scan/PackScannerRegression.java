// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved
package io.github.daesaeho.truetypeshield.scan;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class PackScannerRegression {
    private static final String FONT = "assets/fixture/font/ordinary.bin";
    private static final String MANIFEST = "assets/fixture/font/sample.json";
    private static final byte[] SAFE_SAMPLE = "Benign fingerprint fixture".getBytes(StandardCharsets.UTF_8);
    private static final String DECLARATION = """
        {"providers":[{"type":"minecraft:ttf","file":"fixture:ordinary.bin"}]}
        """;

    private PackScannerRegression() {}

    public static void verifyInstalledPacks() throws IOException {
        Path root = Files.createDirectories(Path.of("pack-scan-fixture"));
        Path ordinaryFont = root.resolve("ordinary/assets/fixture/font/normal.ttf");
        Files.createDirectories(ordinaryFont.getParent());
        Files.writeString(ordinaryFont, "Unlisted benign fixture");

        Path bitmapManifest = root.resolve("bitmap/" + MANIFEST);
        Files.createDirectories(bitmapManifest.getParent());
        Files.writeString(bitmapManifest, """
            {"providers":[{"type":"bitmap","file":"fixture:glyph.png","chars":["A"]}]}
            """);

        Path broken = root.resolve("broken.zip");
        Files.writeString(broken, "This fixture is intentionally not a ZIP.");
        Path renamed = root.resolve("renamed.zip");
        try (var zip = new ZipOutputStream(Files.newOutputStream(renamed))) {
            entry(zip, MANIFEST, DECLARATION.getBytes(StandardCharsets.UTF_8));
            entry(zip, FONT, SAFE_SAMPLE);
        }

        Path overLimit = root.resolve("over-limit.zip");
        try (var zip = new ZipOutputStream(Files.newOutputStream(overLimit))) {
            entry(zip, "assets/fixture/font/large.ttf", new byte[2 * 1024 * 1024 + 1]);
        }

        var report = new PackScanner(Set.of(sampleDigest())).scan(root);
        check(report.packs().size() == 5 && !report.incomplete(), "Expected five test packs");
        expect(report, "ordinary", pack -> pack.hasFonts() && pack.knownMatches() == 0 && !pack.incomplete());
        expect(report, "bitmap", pack -> !pack.hasFonts() && !pack.incomplete());
        expect(report, "broken.zip", PackScanner.PackFinding::incomplete);
        expect(report, "renamed.zip", pack -> pack.declarations() == 1 && pack.fontFiles() == 1
            && pack.knownMatches() == 1 && !pack.incomplete());
        expect(report, "over-limit.zip", pack -> pack.hasFonts() && pack.incomplete());
        check(Files.isRegularFile(renamed) && Files.isRegularFile(broken), "Scan must leave pack files in place");
    }

    private static String sampleDigest() {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(SAFE_SAMPLE));
        } catch (NoSuchAlgorithmException failure) {
            throw new AssertionError(failure);
        }
    }

    private static void entry(ZipOutputStream zip, String path, byte[] data) throws IOException {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(data);
        zip.closeEntry();
    }

    private static void expect(PackScanner.Report report, String name, Predicate<PackScanner.PackFinding> expectation) {
        check(report.packs().stream().anyMatch(pack -> pack.name().equals(name) && expectation.test(pack)),
            "Unexpected scan result for " + name);
    }

    private static void check(boolean condition, String reason) {
        if (!condition) {
            throw new AssertionError(reason);
        }
    }
}
