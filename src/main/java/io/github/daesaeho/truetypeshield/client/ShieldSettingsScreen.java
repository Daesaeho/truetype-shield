// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved
package io.github.daesaeho.truetypeshield.client;

import io.github.daesaeho.truetypeshield.scan.PackScanner;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ShieldSettingsScreen extends Screen {
    private final Screen parent;
    private Button scanButton;
    private StringWidget summary;
    private StringWidget detail;
    private boolean saveFailed;

    public ShieldSettingsScreen(Screen parent) {
        super(Component.literal("TrueType Shield"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int panel = Math.min(width - 20, 270);
        int left = (width - panel) / 2;
        int top = Math.max(8, (height - 146) / 2);
        label(left, top, panel, getTitle());
        label(left, top + 15, panel, Component.literal("TrueType blocking is always on"));
        addRenderableWidget(Button.builder(startupLabel(), button -> {
            saveFailed = !ShieldClient.setScanOnStartup(!ShieldClient.scanOnStartup());
            button.setMessage(startupLabel());
            updateStatus();
        }).bounds(left, top + 33, panel, 20).build());
        scanButton = addRenderableWidget(Button.builder(Component.literal("Scan local packs now"), button -> {
            saveFailed = false;
            ShieldClient.scanLocalPacks();
            updateStatus();
        }).bounds(left, top + 57, panel, 20).build());
        summary = label(left, top + 80, panel, Component.empty());
        detail = label(left, top + 94, panel, Component.empty());
        label(left, top + 108, panel, Component.literal("Pack names and details are in latest.log"));
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
            .bounds(left, top + 124, panel, 20).build());
        updateStatus();
    }

    @Override
    public void tick() {
        updateStatus();
    }

    @Override
    public void onClose() {
        ScreenNavigation.show(parent);
    }

    private StringWidget label(int x, int y, int width, Component text) {
        return addRenderableWidget(new StringWidget(x, y, width, 12, text, font));
    }

    private static Component startupLabel() {
        return Component.literal("Scan local packs on startup: " + (ShieldClient.scanOnStartup() ? "On" : "Off"));
    }

    private void updateStatus() {
        var scan = ShieldClient.lastScan();
        scanButton.active = scan == null || scan.isDone();
        if (scan == null) {
            summary.setMessage(Component.literal("No local scan yet"));
            detail.setMessage(Component.empty());
        } else if (!scan.isDone()) {
            summary.setMessage(Component.literal("Scanning local packs…"));
            detail.setMessage(Component.empty());
        } else if (scan.isCompletedExceptionally()) {
            summary.setMessage(Component.literal("Scan failed"));
            detail.setMessage(Component.literal("See the log for details"));
        } else {
            PackScanner.Report result = scan.join();
            summary.setMessage(Component.literal(result.packs().size() + " scanned · "
                + result.packsWithFonts() + " with outline fonts"));
            String matches = result.knownMatches() + " known font matches";
            detail.setMessage(Component.literal(result.incomplete() || result.incompletePacks() > 0
                ? matches + " · scan incomplete" : matches));
        }
        if (saveFailed) {
            detail.setMessage(Component.literal("Could not save the startup scan setting"));
        }
    }
}
