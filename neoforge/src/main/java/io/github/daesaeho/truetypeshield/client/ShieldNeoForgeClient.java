// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved
package io.github.daesaeho.truetypeshield.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;

@Mod(value = "truetype_shield", dist = Dist.CLIENT)
public final class ShieldNeoForgeClient {
    public ShieldNeoForgeClient() {
        ShieldClient.initialize(FMLPaths.CONFIGDIR.get(), FMLPaths.GAMEDIR.get());
    }
}
