// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved
package io.github.daesaeho.truetypeshield.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class ScreenNavigation {
    private ScreenNavigation() {}

    public static void show(Screen screen) {
        Minecraft.getInstance().setScreen(screen);
    }
}
