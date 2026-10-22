package net.chriskatze.katzencraftmetals.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import java.util.List;

/** Dimension is checked during placement, never inferred from biome tags. */
public record OreGenerationScope(ResourceLocation dimension, List<String> biomes) {
    public static final Codec<OreGenerationScope> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("dimension").forGetter(OreGenerationScope::dimension),
            Codec.STRING.listOf().fieldOf("biomes").forGetter(OreGenerationScope::biomes)
    ).apply(i, OreGenerationScope::new));
    public OreGenerationScope { java.util.Objects.requireNonNull(dimension); biomes = List.copyOf(biomes);
        biomes.forEach(s -> ResourceLocation.parse(s.startsWith("#") ? s.substring(1) : s)); }
    public boolean matchesBiome(Holder<Biome> biome) {
        return biomes.isEmpty() || biomes.stream().anyMatch(s -> s.startsWith("#")
                ? biome.is(TagKey.create(Registries.BIOME, ResourceLocation.parse(s.substring(1))))
                : biome.is(ResourceLocation.parse(s)));
    }
    public boolean matches(ResourceLocation actualDimension, Holder<Biome> biome) {
        return dimension.equals(actualDimension) && matchesBiome(biome);
    }
}
