package net.chriskatze.katzencraftmetals.worldgen;

import net.chriskatze.katzencraftmetals.metal.MetalDefinition.Form;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Native Iron/Copper large noise veins bypass placed features. Apply the same REPLACE scopes there. */
public final class NoiseOreOverrides {
    private NoiseOreOverrides() {}
    public static final Map<ResourceLocation, List<OreGenerationScope>> SCOPES = scopes(ModOreGeneration.entries());
    public static Map<ResourceLocation, List<OreGenerationScope>> scopes(List<ModOreGeneration.Entry> entries) {
        var result = new LinkedHashMap<ResourceLocation, List<OreGenerationScope>>();
        for (var entry : entries) if (entry.config().mode() == net.chriskatze.katzencraftmetals.metal.MetalOreGeneration.Mode.REPLACE
                && (entry.config().form() == Form.ORE || entry.config().form() == Form.DEEPSLATE_ORE)
                && (entry.metal().id().equals(ResourceLocation.withDefaultNamespace("iron")) || entry.metal().id().equals(ResourceLocation.withDefaultNamespace("copper"))))
            result.computeIfAbsent(entry.metal().id(), ignored -> new ArrayList<>()).add(entry.scope());
        result.replaceAll((id, scopes) -> List.copyOf(scopes));
        return Collections.unmodifiableMap(result);
    }
    public static BlockState filter(BlockState state, ResourceLocation dimension, Holder<Biome> biome,
                                    Map<ResourceLocation, List<OreGenerationScope>> scopes) {
        if (state == null || scopes.isEmpty()) return state;
        String metal = state.is(Blocks.DEEPSLATE_IRON_ORE) || state.is(Blocks.RAW_IRON_BLOCK) ? "iron"
                : state.is(Blocks.COPPER_ORE) || state.is(Blocks.RAW_COPPER_BLOCK) ? "copper" : null;
        if (metal == null) return state;
        return scopes.getOrDefault(ResourceLocation.withDefaultNamespace(metal), List.of()).stream().anyMatch(s -> s.matches(dimension, biome))
                ? null : state; // Null requests the generator's normal host block, not air.
    }
}
