// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved
package io.github.daesaeho.truetypeshield.scan;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;
import java.util.stream.Collectors;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

public final class PackScanner {
    private static final int MAX_PACKS = 256;
    private static final int MAX_ENTRIES = 16_384;
    private static final int MAX_DEPTH = 32;
    private static final long MAX_ZIP_BYTES = 128L * 1024 * 1024;
    private static final int MAX_MANIFEST_BYTES = 128 * 1024;
    private static final int MAX_FONT_BYTES = 2 * 1024 * 1024;
    private static final int MAX_READ_BYTES = 32 * 1024 * 1024;
    private static final Set<String> FONT_EXTENSIONS = Set.of("ttf", "ttc", "otf", "otc", "woff", "woff2");

    public record PackFinding(String name, int declarations, int fontFiles, int knownMatches, boolean incomplete) {
        public boolean hasFonts() {
            return declarations > 0 || fontFiles > 0;
        }
    }

    public record Report(List<PackFinding> packs, boolean incomplete) {
        public long packsWithFonts() {
            return packs.stream().filter(PackFinding::hasFonts).count();
        }

        public int knownMatches() {
            return packs.stream().mapToInt(PackFinding::knownMatches).sum();
        }

        public long incompletePacks() {
            return packs.stream().filter(PackFinding::incomplete).count();
        }
    }

    private final Set<String> knownFonts;

    public PackScanner() {
        this(readSignatures());
    }

    PackScanner(Set<String> knownFonts) {
        this.knownFonts = Set.copyOf(knownFonts);
    }

    public Report scan(Path root) throws IOException {
        if (Files.notExists(root, NOFOLLOW_LINKS)) {
            return new Report(List.of(), false);
        }
        if (!Files.isDirectory(root, NOFOLLOW_LINKS)) {
            return new Report(List.of(), true);
        }
        List<Path> packs;
        try (var children = Files.list(root)) {
            packs = children.filter(PackScanner::isPack).limit(MAX_PACKS + 1L).toList();
        } catch (UncheckedIOException error) {
            throw error.getCause();
        }
        var findings = new ArrayList<PackFinding>();
        for (Path pack : packs.subList(0, Math.min(MAX_PACKS, packs.size()))) {
            findings.add(scanPack(pack));
        }
        return new Report(List.copyOf(findings), packs.size() > MAX_PACKS);
    }

    private static boolean isPack(Path path) {
        return Files.isDirectory(path, NOFOLLOW_LINKS) || Files.isRegularFile(path, NOFOLLOW_LINKS)
            && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip");
    }

    private PackFinding scanPack(Path path) {
        String name = path.getFileName().toString().replaceAll("\\p{C}", "?");
        try {
            if (Files.isDirectory(path, NOFOLLOW_LINKS)) {
                return scanDirectory(path, name);
            }
            if (Files.size(path) <= MAX_ZIP_BYTES) {
                return scanArchive(path, name);
            }
        } catch (IOException | UncheckedIOException | SecurityException error) {
            return new PackFinding(name, 0, 0, 0, true);
        }
        return new PackFinding(name, 0, 0, 0, true);
    }

    private PackFinding scanDirectory(Path root, String name) throws IOException {
        var entries = new LinkedHashMap<String, Entry>();
        boolean incomplete = false;
        try (var paths = Files.walk(root, MAX_DEPTH)) {
            var iterator = paths.iterator();
            for (int count = 0; iterator.hasNext(); count++) {
                if (count == MAX_ENTRIES) {
                    incomplete = true;
                    break;
                }
                Path path = iterator.next();
                Path relative = root.relativize(path);
                if (Files.isRegularFile(path, NOFOLLOW_LINKS)) {
                    String key = relative.toString().replace(path.getFileSystem().getSeparator(), "/");
                    entries.put(key, new Entry(Files.size(path), () -> Files.newInputStream(path, NOFOLLOW_LINKS)));
                } else if (Files.isSymbolicLink(path) || relative.getNameCount() == MAX_DEPTH) {
                    incomplete = true;
                }
            }
        }
        return inspect(name, entries, incomplete);
    }

    private PackFinding scanArchive(Path path, String name) throws IOException {
        try (var zip = new ZipFile(path.toFile())) {
            var entries = new LinkedHashMap<String, Entry>();
            boolean incomplete = zip.size() > MAX_ENTRIES;
            var iterator = zip.entries();
            for (int count = 0; count < MAX_ENTRIES && iterator.hasMoreElements(); count++) {
                var entry = iterator.nextElement();
                if (!entry.isDirectory() && entries.putIfAbsent(entry.getName(),
                    new Entry(entry.getSize(), () -> zip.getInputStream(entry))) != null) {
                    incomplete = true;
                }
            }
            return inspect(name, entries, incomplete);
        }
    }

    private PackFinding inspect(String name, Map<String, Entry> entries, boolean incomplete) {
        var candidateNames = new LinkedHashSet<String>();
        var budget = new ReadBudget();
        int declarations = 0;
        for (var asset : entries.entrySet()) {
            String path = asset.getKey();
            if (isFontFile(path)) {
                candidateNames.add(path);
            }
            if (path.endsWith(".json") && isFontAsset(path)) {
                try {
                    var manifest = readDeclarations(budget.read(asset.getValue(), MAX_MANIFEST_BYTES));
                    declarations += manifest.count();
                    candidateNames.addAll(manifest.targets());
                    incomplete |= manifest.incomplete();
                } catch (IOException error) {
                    incomplete = true;
                }
            }
        }
        int matches = 0;
        int files = 0;
        var digest = sha256();
        for (String candidate : candidateNames) {
            Entry asset = entries.get(candidate);
            if (asset == null) {
                incomplete = true;
                continue;
            }
            files++;
            try {
                String hash = HexFormat.of().formatHex(digest.digest(budget.read(asset, MAX_FONT_BYTES)));
                if (knownFonts.contains(hash)) {
                    matches++;
                }
            } catch (IOException error) {
                incomplete = true;
            }
        }
        return new PackFinding(name, declarations, files, matches, incomplete);
    }

    private static boolean isFontAsset(String path) {
        String[] parts = path.split("/", 4);
        return parts.length == 4 && parts[0].equals("assets") && !parts[1].isEmpty() && parts[2].equals("font");
    }

    private static boolean isFontFile(String path) {
        int dot = path.lastIndexOf('.');
        return path.startsWith("assets/") && dot >= 0
            && FONT_EXTENSIONS.contains(path.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    private static DeclarationScan readDeclarations(byte[] json) {
        var targets = new ArrayList<String>();
        int count = 0;
        boolean incomplete = false;
        try (var reader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(json), StandardCharsets.UTF_8))) {
            reader.beginObject();
            while (reader.hasNext()) {
                if (!reader.nextName().equals("providers")) {
                    reader.skipValue();
                    continue;
                }
                reader.beginArray();
                while (reader.hasNext()) {
                    if (reader.peek() != JsonToken.BEGIN_OBJECT) {
                        reader.skipValue();
                        incomplete = true;
                        continue;
                    }
                    var provider = readProvider(reader);
                    if (provider.type() == null) {
                        incomplete = true;
                    } else if (provider.type().equals("ttf") || provider.type().equals("minecraft:ttf")) {
                        count++;
                        String target = fontPath(provider.file());
                        if (target == null) {
                            incomplete = true;
                        } else {
                            targets.add(target);
                        }
                    }
                }
                reader.endArray();
            }
            reader.endObject();
            incomplete |= reader.peek() != JsonToken.END_DOCUMENT;
        } catch (IOException | IllegalStateException error) {
            incomplete = true;
        }
        return new DeclarationScan(count, targets, incomplete);
    }

    private static Provider readProvider(JsonReader reader) throws IOException {
        String type = null;
        String file = null;
        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "type" -> type = readString(reader);
                case "file" -> file = readString(reader);
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        return new Provider(type, file);
    }

    private static String readString(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.STRING) {
            return reader.nextString();
        }
        reader.skipValue();
        return null;
    }

    private static String fontPath(String file) {
        if (file == null) {
            return null;
        }
        int separator = file.indexOf(':');
        String namespace = separator < 0 ? "minecraft" : file.substring(0, separator);
        String path = file.substring(separator + 1);
        if (namespace.isEmpty() || path.isEmpty()) {
            return null;
        }
        // Use the resource as an index key only. No archive member is extracted or resolved on disk.
        return "assets/" + namespace + "/font/" + path;
    }

    private static Set<String> readSignatures() {
        try (var stream = PackScanner.class.getResourceAsStream("/assets/truetype_shield/known-fonts.sha256")) {
            if (stream == null) {
                throw new IOException("Missing font fingerprint list");
            }
            Set<String> signatures = new String(stream.readAllBytes(), StandardCharsets.UTF_8).lines()
                .filter(line -> line.matches("[a-f0-9]{64}")).collect(Collectors.toUnmodifiableSet());
            if (signatures.isEmpty()) {
                throw new IOException("Empty font fingerprint list");
            }
            return signatures;
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 unavailable", error);
        }
    }

    @FunctionalInterface
    private interface Opener {
        InputStream open() throws IOException;
    }

    private record Entry(long size, Opener opener) {}

    private record Provider(String type, String file) {}

    private record DeclarationScan(int count, List<String> targets, boolean incomplete) {}

    private static final class ReadBudget {
        private int remaining = MAX_READ_BYTES;

        byte[] read(Entry entry, int limit) throws IOException {
            int allowance = Math.min(remaining, limit);
            if (allowance == 0 || entry.size() > allowance) {
                throw new IOException("Pack scan read limit reached");
            }
            try (var stream = entry.opener().open()) {
                byte[] data = stream.readNBytes(allowance + 1);
                remaining = Math.max(0, remaining - data.length);
                if (data.length > allowance) {
                    throw new IOException("Pack entry exceeds scan read limit");
                }
                return data;
            }
        }
    }
}
