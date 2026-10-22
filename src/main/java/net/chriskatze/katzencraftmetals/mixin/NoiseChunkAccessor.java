package net.chriskatze.katzencraftmetals.mixin;

import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NoiseChunk.class)
public interface NoiseChunkAccessor {
    @Invoker("getInterpolatedState") BlockState katzencraftmetals$interpolatedState();
}
