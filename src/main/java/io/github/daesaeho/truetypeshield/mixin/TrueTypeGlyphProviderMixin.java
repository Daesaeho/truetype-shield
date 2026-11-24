// SPDX-License-Identifier: MIT
package io.github.daesaeho.truetypeshield.mixin;

import com.mojang.blaze3d.font.GlyphProvider;
import it.unimi.dsi.fastutil.ints.IntSets;
import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TrueTypeGlyphProviderDefinition.class)
abstract class TrueTypeGlyphProviderMixin {
    @Inject(
        method = "load(Lnet/minecraft/server/packs/resources/ResourceManager;)Lcom/mojang/blaze3d/font/GlyphProvider;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void truetypeShield$skipFont(ResourceManager resources, CallbackInfoReturnable<GlyphProvider> result) {
        // An empty provider lets other providers in the same font definition supply glyphs.
        result.setReturnValue(IntSets::emptySet);
    }
}
