package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.block.custom.FoundryBasinBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Basin-side discovery: vanilla's downward search recognizes AbstractCauldronBlock only.
 * Use its native upward clearance/source queries, drip chances and 50+distance delivery delay. */
public final class FoundryBasinDripstone {
    private FoundryBasinDripstone() {}

    private static boolean available(BlockState state) {
        return state.hasProperty(FoundryBasinBlock.CONTAINED_WATER)
                && state.getValue(FoundryBasinBlock.CAST_STATE) == FoundryBasinBlock.CastState.EMPTY
                && (FoundryBasinBlock.canContainFluid(state)
                    || (FoundryBasinBlock.containedFluid(state) == Fluids.WATER
                        && state.getValue(FoundryBasinBlock.FLUID_LEVEL) < 3));
    }

    private static Fluid source(ServerLevel level, BlockPos tip) {
        // Mud drips dehydrate mud into clay in vanilla; they do not fill cauldrons.
        // Check before the native query, which otherwise reports that moisture as water.
        var root = tip.above();
        for (int i=1; i<11; i++, root=root.above()) {
            if (!level.getBlockState(root).is(Blocks.POINTED_DRIPSTONE)) {
                if (level.getBlockState(root.above()).is(Blocks.MUD)) return Fluids.EMPTY;
                break;
            }
        }
        return PointedDripstoneBlock.getCauldronFillFluidType(level, tip);
    }

    private static boolean accepts(BlockState state, Fluid fluid) {
        return (fluid == Fluids.WATER || fluid == Fluids.LAVA)
                && (FoundryBasinBlock.canContainFluid(state)
                    || (fluid == Fluids.WATER && FoundryBasinBlock.containedFluid(state) == Fluids.WATER));
    }

    public static void schedule(BlockState state, ServerLevel level, BlockPos pos, float chance) {
        if (!available(state)) return;
        var tip = PointedDripstoneBlock.findStalactiteTipAboveCauldron(level, pos);
        if (tip == null) return;
        var fluid = source(level, tip);
        if (!accepts(state, fluid)) return;
        var drip = fluid.getFluidType().getDripInfo();
        if (drip != null && chance < drip.chance()) {
            level.levelEvent(1504, tip, 0);
            level.scheduleTick(pos, state.getBlock(), 50 + tip.getY() - pos.getY());
        }
    }

    public static void fill(BlockState state, ServerLevel level, BlockPos pos) {
        if (!available(state)) return;
        var tip = PointedDripstoneBlock.findStalactiteTipAboveCauldron(level, pos);
        if (tip == null) return;
        var fluid = source(level, tip);
        if (!accepts(state, fluid)) return;
        int amount = fluid == Fluids.LAVA ? 3 : FoundryBasinBlock.hasContainedFluid(state)
                ? state.getValue(FoundryBasinBlock.FLUID_LEVEL) + 1 : 1;
        level.setBlock(pos, FoundryBasinBlock.withFluid(state, fluid, amount), Block.UPDATE_ALL);
        level.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
        level.levelEvent(fluid == Fluids.LAVA ? 1046 : 1047, pos, 0);
    }
}
