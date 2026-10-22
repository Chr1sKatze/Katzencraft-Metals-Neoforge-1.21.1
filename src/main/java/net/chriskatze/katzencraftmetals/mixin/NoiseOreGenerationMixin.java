package net.chriskatze.katzencraftmetals.mixin;

import net.chriskatze.katzencraftmetals.worldgen.NoiseOreOverrides;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** No active overrides: one empty-map check; all native noise/seed behavior stays intact. */
@Mixin(NoiseBasedChunkGenerator.class)
public class NoiseOreGenerationMixin {
    @Redirect(method = "doFill", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/NoiseChunk;getInterpolatedState()Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState katzencraftmetals$filterNoiseOre(NoiseChunk noise, Blender blender, StructureManager structures,
                                                     RandomState random, ChunkAccess chunk, int minCellY, int cellCountY) {
        var state = ((NoiseChunkAccessor) noise).katzencraftmetals$interpolatedState();
        if (state == null || NoiseOreOverrides.SCOPES.isEmpty()) return state;
        var level = ((StructureManagerAccessor) structures).katzencraftmetals$level();
        if (!(level instanceof WorldGenLevel world)) return state;
        var biome = chunk.getNoiseBiome(QuartPos.fromBlock(noise.blockX()), QuartPos.fromBlock(noise.blockY()), QuartPos.fromBlock(noise.blockZ()));
        return NoiseOreOverrides.filter(state, world.getLevel().dimension().location(), biome, NoiseOreOverrides.SCOPES);
    }
}
