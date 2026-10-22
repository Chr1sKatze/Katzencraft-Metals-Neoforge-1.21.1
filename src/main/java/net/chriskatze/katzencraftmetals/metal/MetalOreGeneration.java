package net.chriskatze.katzencraftmetals.metal;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import java.util.*;

/** Immutable catalog configuration; ore blocks are declared separately through form(). */
public record MetalOreGeneration(MetalDefinition.Form form, ResourceLocation dimension, List<String> biomes,
                                 List<String> replacements, int veinSize, int attemptsPerChunk,
                                 int minY, int maxY, Distribution distribution, Mode mode, boolean enabled) {
    public enum Mode { ADDITIVE, REPLACE }
    public enum Distribution {
        UNIFORM, TRIANGLE, BOTTOM_HEAVY, TOP_HEAVY;
        public static final Codec<Distribution> CODEC = Codec.STRING.xmap(Distribution::valueOf, Enum::name);
        /** Inclusive discrete distributions, using only the chunk's seeded RandomSource. */
        public int sample(RandomSource random, int min, int max) {
            int range = max - min;
            return min + switch (this) {
                case UNIFORM -> random.nextInt(range + 1);
                // Equivalent to native TrapezoidHeight with a zero-width plateau.
                case TRIANGLE -> random.nextInt(range - range / 2 + 1) + random.nextInt(range / 2 + 1);
                case BOTTOM_HEAVY -> Math.min(random.nextInt(range + 1), random.nextInt(range + 1));
                case TOP_HEAVY -> Math.max(random.nextInt(range + 1), random.nextInt(range + 1));
            };
        }
    }
    public MetalOreGeneration {
        Objects.requireNonNull(form); Objects.requireNonNull(dimension);
        Objects.requireNonNull(distribution); Objects.requireNonNull(mode);
        if (!MetalDefinition.isOre(form)) throw new IllegalArgumentException("Generation requires an ore form: " + form);
        if (veinSize < 1 || veinSize > 64) throw new IllegalArgumentException("Ore veinSize must be 1..64");
        if (attemptsPerChunk < 1 || attemptsPerChunk > 256) throw new IllegalArgumentException("Ore attemptsPerChunk must be 1..256");
        if (minY < -2032 || maxY > 2031 || minY > maxY) throw new IllegalArgumentException("Invalid ore height range: " + minY + ".." + maxY);
        biomes = selectors(biomes); replacements = selectors(replacements);
        if (replacements.isEmpty()) throw new IllegalArgumentException("Specify ore replacement blocks/tags explicitly");
    }
    private static List<String> selectors(List<String> values) {
        var copy = List.copyOf(values);
        for (String s : copy) ResourceLocation.parse(s.startsWith("#") ? s.substring(1) : s);
        if (new HashSet<>(copy).size() != copy.size()) throw new IllegalArgumentException("Duplicate ore selector: " + copy);
        return copy;
    }
    public static final class Builder {
        private final MetalDefinition.Form form;
        private ResourceLocation dimension;
        private List<String> biomes = List.of(), replacements = List.of();
        private int veinSize = 8, attempts = 3, min = -64, max = 64;
        private Distribution distribution = Distribution.UNIFORM;
        private Mode mode;
        private boolean enabled = true;
        public Builder(MetalDefinition.Form form) { this.form = form; }
        public Builder dimension(String id) { dimension = ResourceLocation.parse(id); return this; }
        /** OR of biome IDs and #tags; no selectors means unrestricted within the dimension. */
        public Builder biomes(String... selectors) { biomes = List.of(selectors); return this; }
        /** OR of block IDs and #tags, all producing this entry's declared ore variant. */
        public Builder replace(String... selectors) { replacements = List.of(selectors); return this; }
        public Builder veinSize(int value) { veinSize = value; return this; }
        public Builder attemptsPerChunk(int value) { attempts = value; return this; }
        public Builder height(int minY, int maxY) { min = minY; max = maxY; return this; }
        public Builder distribution(Distribution value) { distribution = value; return this; }
        public Builder mode(Mode value) { mode = Objects.requireNonNull(value); return this; }
        public Builder enabled(boolean value) { enabled = value; return this; }
        MetalOreGeneration build(Mode defaultMode) {
            return new MetalOreGeneration(form, Objects.requireNonNull(dimension, "Ore generation requires a dimension"),
                    biomes, replacements, veinSize, attempts, min, max, distribution, mode == null ? defaultMode : mode, enabled);
        }
    }
}
