// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved
package io.github.daesaeho.truetypeshield.client;

import io.github.daesaeho.truetypeshield.scan.PackScanner;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ShieldClient {
    private static final String STARTUP_KEY = "scanLocalPacksOnStartup";
    private static final Logger LOGGER = LoggerFactory.getLogger("TrueType Shield");
    private static volatile boolean scanOnStartup = true;
    private static volatile CompletableFuture<PackScanner.Report> lastScan;
    private static Path configPath;
    private static Path resourcePacks;

    private ShieldClient() {}

    public static void initialize(Path configDirectory, Path gameDirectory) {
        configPath = configDirectory.resolve("truetype-shield.properties");
        resourcePacks = gameDirectory.resolve("resourcepacks");
        Path config = configPath;
        if (Files.isRegularFile(config)) {
            try (var input = Files.newInputStream(config)) {
                var properties = new Properties();
                properties.load(input);
                scanOnStartup = !"false".equalsIgnoreCase(properties.getProperty(STARTUP_KEY));
            } catch (IOException | IllegalArgumentException error) {
                LOGGER.warn("Could not read TrueType Shield settings. Using startup scan defaults.", error);
            }
        }
        if (scanOnStartup) {
            scanLocalPacks();
        }
    }

    static boolean scanOnStartup() {
        return scanOnStartup;
    }

    static boolean setScanOnStartup(boolean enabled) {
        Path config = configPath;
        Path temporary = null;
        try {
            Files.createDirectories(config.getParent());
            temporary = Files.createTempFile(config.getParent(), "truetype-shield-", ".tmp");
            var properties = new Properties();
            properties.setProperty(STARTUP_KEY, Boolean.toString(enabled));
            try (var output = Files.newOutputStream(temporary)) {
                properties.store(output, "TrueType Shield settings");
            }
            try {
                Files.move(temporary, config, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException error) {
                Files.move(temporary, config, StandardCopyOption.REPLACE_EXISTING);
            }
            scanOnStartup = enabled;
            return true;
        } catch (IOException error) {
            LOGGER.warn("Could not save TrueType Shield settings.", error);
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException cleanupError) {
                    LOGGER.debug("Could not remove temporary settings file.", cleanupError);
                }
            }
            return false;
        }
    }

    static CompletableFuture<PackScanner.Report> lastScan() {
        return lastScan;
    }

    static synchronized void scanLocalPacks() {
        if (lastScan != null && !lastScan.isDone()) {
            return;
        }
        Path folder = resourcePacks;
        lastScan = CompletableFuture.supplyAsync(() -> {
            try {
                return new PackScanner().scan(folder);
            } catch (IOException error) {
                throw new java.io.UncheckedIOException(error);
            }
        }).whenComplete((report, failure) -> {
            if (failure == null) {
                logScan(report);
            } else {
                LOGGER.warn("Could not scan the local resource packs.", failure);
            }
        });
    }

    private static void logScan(PackScanner.Report report) {
        LOGGER.info("Scanned {} local resource packs. {} contain outline font files or declarations.",
            report.packs().size(), report.packsWithFonts());
        for (var pack : report.packs()) {
            if (pack.knownMatches() > 0) {
                LOGGER.warn("Known minecraft2shell font file matches in '{}': {}.", pack.name(), pack.knownMatches());
            } else if (pack.hasFonts()) {
                LOGGER.info("Pack '{}' contains outline font files or declarations. This is a compatibility notice.", pack.name());
            }
            if (pack.incomplete()) {
                LOGGER.warn("Pack '{}' could only be partially scanned.", pack.name());
            }
        }
        if (report.incomplete()) {
            LOGGER.warn("The local pack list could only be partially scanned.");
        }
    }

}
