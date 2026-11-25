// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved
package io.github.daesaeho.truetypeshield.mixin;

import io.github.daesaeho.truetypeshield.client.ScreenNavigation;
import io.github.daesaeho.truetypeshield.client.ShieldSettingsScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PackSelectionScreen.class)
abstract class PackSelectionScreenMixin extends Screen {
    @Unique
    private Button truetypeShield$settingsButton;

    protected PackSelectionScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void truetypeShield$addSettingsButton(CallbackInfo callback) {
        truetypeShield$settingsButton = addRenderableWidget(Button.builder(Component.literal("Font settings"),
            button -> ScreenNavigation.show(new ShieldSettingsScreen(this)))
            .bounds(width - 104, 4, 100, 20).build());
    }

    @Inject(method = "repositionElements", at = @At("TAIL"), require = 0)
    private void truetypeShield$repositionSettingsButton(CallbackInfo callback) {
        if (truetypeShield$settingsButton != null) {
            truetypeShield$settingsButton.setX(width - truetypeShield$settingsButton.getWidth() - 4);
        }
    }
}
