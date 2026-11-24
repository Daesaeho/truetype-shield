// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved
package io.github.daesaeho.truetypeshield.regression;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.font.providers.GlyphProviderDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.MultiPackResourceManager;

final class RegressionPack {
    static MultiPackResourceManager open(Path fixture) {
        var location = new PackLocationInfo(FontProviderRegression.NAMESPACE, Component.literal("Font regression fixture"),
            PackSource.DEFAULT, Optional.empty());
        var pack = new PathPackResources(location, fixture) {
            @Override
            public IoSupplier<InputStream> getResource(PackType type, Identifier id) {
                FontProviderRegression.checkLookup(id.getNamespace(), id.getPath());
                return super.getResource(type, id);
            }
        };
        return new MultiPackResourceManager(PackType.CLIENT_RESOURCES, List.of(pack));
    }

    static List<GlyphProviderDefinition> readProviders(MultiPackResourceManager resources) throws IOException {
        var id = Identifier.fromNamespaceAndPath(FontProviderRegression.NAMESPACE, FontProviderRegression.FONT_PATH);
        try (var reader = resources.getResourceOrThrow(id).openAsReader()) {
            return GlyphProviderDefinition.Conditional.CODEC.listOf().parse(JsonOps.INSTANCE,
                JsonParser.parseReader(reader).getAsJsonObject().get("providers")).getOrThrow().stream()
                .map(GlyphProviderDefinition.Conditional::definition).toList();
        }
    }
}
