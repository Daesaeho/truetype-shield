// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved
package io.github.daesaeho.truetypeshield.regression;

import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.font.providers.BitmapProvider;
import net.minecraft.client.gui.font.providers.GlyphProviderDefinition;
import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition;
import net.minecraft.client.gui.font.providers.UnihexProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.Resource;

final class RegressionPack {
    private static final Map<String, MapCodec<? extends GlyphProviderDefinition>> CODECS = Map.of(
        "ttf", TrueTypeGlyphProviderDefinition.CODEC,
        "bitmap", BitmapProvider.Definition.CODEC,
        "unihex", UnihexProvider.Definition.CODEC
    );

    static MultiPackResourceManager open(Path fixture) {
        // Minecraft 1.20.5 changed both the path pack constructor and the dispatch codec.
        // Supply fixture files through the stable resource lookup and provider codecs.
        return new MultiPackResourceManager(PackType.CLIENT_RESOURCES, List.of()) {
            @Override
            public Optional<Resource> getResource(ResourceLocation id) {
                FontProviderRegression.checkLookup(id.getNamespace(), id.getPath());
                Path file = fixture.resolve("assets").resolve(id.getNamespace()).resolve(id.getPath());
                if (!Files.isRegularFile(file)) {
                    return Optional.empty();
                }
                return Optional.of(new Resource(null, () -> Files.newInputStream(file)));
            }
        };
    }

    static List<GlyphProviderDefinition> readProviders(MultiPackResourceManager resources) throws IOException {
        var id = new ResourceLocation(FontProviderRegression.NAMESPACE, FontProviderRegression.FONT_PATH);
        try (var reader = resources.getResourceOrThrow(id).openAsReader()) {
            var entries = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("providers");
            List<GlyphProviderDefinition> providers = new ArrayList<>();
            for (var entry : entries) {
                String type = entry.getAsJsonObject().get("type").getAsString();
                var codec = CODECS.get(type);
                if (codec == null) {
                    throw new IllegalArgumentException("Unexpected fixture provider " + type);
                }
                providers.add(readResult(codec.codec().parse(JsonOps.INSTANCE, entry), type));
            }
            return providers;
        }
    }

    private static GlyphProviderDefinition readResult(DataResult<?> result, String type) {
        // DataResult became an interface in 1.20.5, so a direct call compiled on 1.20.1 cannot link there.
        try {
            var decoded = (Optional<?>) DataResult.class.getMethod("result").invoke(result);
            return (GlyphProviderDefinition) decoded
                .orElseThrow(() -> new IllegalArgumentException("Invalid fixture provider " + type));
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot read the fixture codec result", error);
        }
    }
}
