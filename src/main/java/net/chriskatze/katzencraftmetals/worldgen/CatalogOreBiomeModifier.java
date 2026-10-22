package net.chriskatze.katzencraftmetals.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.*;
import java.util.List;

/** Appends catalog deposits once; swaps selected vanilla holders in place, preserving step/order. */
public record CatalogOreBiomeModifier(List<Addition> additions, List<Replacement> replacements) implements BiomeModifier {
    public record Addition(OreGenerationScope scope, Holder<PlacedFeature> feature) {
        static final Codec<Addition> CODEC = RecordCodecBuilder.create(i -> i.group(
                OreGenerationScope.CODEC.fieldOf("scope").forGetter(Addition::scope),
                PlacedFeature.CODEC.fieldOf("feature").forGetter(Addition::feature)).apply(i, Addition::new));
    }
    /** Native feature values are unresolved during bootstrap. Build one shared guarded copy
     * only after registry loading, when biome modification runs. The cache is reload-local. */
    public static final class Replacement {
        private final Holder<PlacedFeature> original;
        private final List<OreGenerationScope> scopes;
        private Holder<PlacedFeature> guarded;
        public Replacement(Holder<PlacedFeature> original, List<OreGenerationScope> scopes) {
            this.original = original; this.scopes = List.copyOf(scopes);
        }
        public Holder<PlacedFeature> original() { return original; }
        public List<OreGenerationScope> scopes() { return scopes; }
        public synchronized Holder<PlacedFeature> guarded() {
            if (guarded == null) {
                var nativeFeature = original.value();
                var modifiers = new java.util.ArrayList<>(nativeFeature.placement());
                modifiers.add(new OreReplacementFilter(scopes));
                guarded = Holder.direct(new PlacedFeature(nativeFeature.feature(), List.copyOf(modifiers)));
            }
            return guarded;
        }
        static final Codec<Replacement> CODEC = RecordCodecBuilder.create(i -> i.group(
                PlacedFeature.CODEC.fieldOf("original").forGetter(Replacement::original),
                OreGenerationScope.CODEC.listOf().fieldOf("excluded_scopes").forGetter(Replacement::scopes)).apply(i, Replacement::new));
    }
    public static final MapCodec<CatalogOreBiomeModifier> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Addition.CODEC.listOf().fieldOf("additions").forGetter(CatalogOreBiomeModifier::additions),
            Replacement.CODEC.listOf().fieldOf("replacements").forGetter(CatalogOreBiomeModifier::replacements)
    ).apply(i, CatalogOreBiomeModifier::new));
    public CatalogOreBiomeModifier { additions = List.copyOf(additions); replacements = List.copyOf(replacements); }
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        var settings = builder.getGenerationSettings();
        if (phase == Phase.ADD) for (var entry : additions) {
            if (entry.scope.matchesBiome(biome) && !settings.getFeatures(GenerationStep.Decoration.UNDERGROUND_ORES).contains(entry.feature))
                settings.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, entry.feature);
        }
        if (phase == Phase.MODIFY) for (var step : GenerationStep.Decoration.values()) {
            var features = settings.getFeatures(step);
            for (var replacement : replacements) {
                for (int index = 0; index < features.size(); index++) {
                    if (features.get(index).equals(replacement.original())) features.set(index, replacement.guarded());
                }
            }
        }
    }
    public MapCodec<? extends BiomeModifier> codec() { return ModOreGeneration.BIOME.get(); }
}
