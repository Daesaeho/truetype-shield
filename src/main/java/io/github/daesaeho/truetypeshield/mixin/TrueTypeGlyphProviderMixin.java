// SPDX-License-Identifier: LicenseRef-All-Rights-Reserved
package io.github.daesaeho.truetypeshield.mixin;

import com.mojang.blaze3d.font.GlyphProvider;
import it.unimi.dsi.fastutil.ints.IntSets;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TrueTypeGlyphProviderDefinition.class)
abstract class TrueTypeGlyphProviderMixin {
    @Unique
    private static final AtomicBoolean truetypeShield$warned = new AtomicBoolean();

    @Inject(
        method = "load(Lnet/minecraft/server/packs/resources/ResourceManager;)Lcom/mojang/blaze3d/font/GlyphProvider;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void truetypeShield$skipFont(ResourceManager resources, CallbackInfoReturnable<GlyphProvider> result) {
        // An empty provider lets other providers in the same font definition supply glyphs.
        result.setReturnValue(IntSets::emptySet);
        if (truetypeShield$warned.compareAndSet(false, true)) {
            LoggerFactory.getLogger("TrueType Shield").warn(
                "Blocked a TrueType font from a resource pack. Custom text or icons that depend on TrueType may use fallback glyphs or appear missing."
            );
        }
    }
}
