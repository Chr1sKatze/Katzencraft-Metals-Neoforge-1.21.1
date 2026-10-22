package net.chriskatze.katzencraftmetals.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.*;
import java.util.List;
import java.util.stream.Stream;

/** Suppresses a vanilla placement ONLY in configured replacement scopes; all native modifiers remain. */
public final class OreReplacementFilter extends PlacementModifier {
    public static final MapCodec<OreReplacementFilter> CODEC = OreGenerationScope.CODEC.listOf()
            .fieldOf("excluded_scopes").xmap(OreReplacementFilter::new, p -> p.exclusions);
    private final List<OreGenerationScope> exclusions;
    public OreReplacementFilter(List<OreGenerationScope> exclusions) { this.exclusions = List.copyOf(exclusions); }
    public boolean allows(net.minecraft.resources.ResourceLocation dimension, net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome) {
        return exclusions.stream().noneMatch(s -> s.matches(dimension, biome));
    }
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        return allows(context.getLevel().getLevel().dimension().location(), context.getLevel().getBiome(pos))
                ? Stream.of(pos) : Stream.empty();
    }
    public PlacementModifierType<?> type() { return ModOreGeneration.REPLACEMENT.get(); }
}
