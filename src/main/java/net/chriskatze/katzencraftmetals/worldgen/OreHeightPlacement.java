package net.chriskatze.katzencraftmetals.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.chriskatze.katzencraftmetals.metal.MetalOreGeneration.Distribution;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.*;
import java.util.stream.Stream;

/** One seeded height sample per attempt, clipped BEFORE sampling to this world's generation limits. */
public final class OreHeightPlacement extends PlacementModifier {
    public static final MapCodec<OreHeightPlacement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            OreGenerationScope.CODEC.fieldOf("scope").forGetter(p -> p.scope),
            Codec.intRange(-2032, 2031).fieldOf("min_y").forGetter(p -> p.min),
            Codec.intRange(-2032, 2031).fieldOf("max_y").forGetter(p -> p.max),
            Distribution.CODEC.fieldOf("distribution").forGetter(p -> p.distribution)
    ).apply(i, OreHeightPlacement::new));
    private final OreGenerationScope scope;
    private final int min, max;
    private final Distribution distribution;
    public OreHeightPlacement(OreGenerationScope scope, int min, int max, Distribution distribution) {
        if (min > max) throw new IllegalArgumentException("Inverted ore height range");
        this.scope = scope; this.min = min; this.max = max; this.distribution = distribution;
    }
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        if (!scope.dimension().equals(context.getLevel().getLevel().dimension().location())) return Stream.empty();
        int low = Math.max(min, context.getMinGenY());
        int high = Math.min(max, Math.min(context.getLevel().getMaxBuildHeight() - 1,
                context.generator().getMinY() + context.generator().getGenDepth() - 1));
        if (low > high) return Stream.empty();
        int y = switch (distribution) {
            case UNIFORM -> net.minecraft.world.level.levelgen.heightproviders.UniformHeight.of(
                    net.minecraft.world.level.levelgen.VerticalAnchor.absolute(low), net.minecraft.world.level.levelgen.VerticalAnchor.absolute(high)).sample(random, context);
            case TRIANGLE -> net.minecraft.world.level.levelgen.heightproviders.TrapezoidHeight.of(
                    net.minecraft.world.level.levelgen.VerticalAnchor.absolute(low), net.minecraft.world.level.levelgen.VerticalAnchor.absolute(high)).sample(random, context);
            default -> distribution.sample(random, low, high);
        };
        var at = new BlockPos(pos.getX(), y, pos.getZ());
        return scope.matchesBiome(context.getLevel().getBiome(at)) ? Stream.of(at) : Stream.empty();
    }
    public PlacementModifierType<?> type() { return ModOreGeneration.HEIGHT.get(); }
}
