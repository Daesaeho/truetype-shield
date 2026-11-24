// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved
package io.github.daesaeho.truetypeshield.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class ShieldFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        var loader = FabricLoader.getInstance();
        ShieldClient.initialize(loader.getConfigDir(), loader.getGameDir());
    }
}
