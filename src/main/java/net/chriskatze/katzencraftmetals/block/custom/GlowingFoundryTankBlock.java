package net.chriskatze.katzencraftmetals.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Registry-compatible Tank subclass.
 *
 * LIT is retained only because the existing ModBlocks registration still reads
 * that property for its light-level lambda. Controller/storage code is no
 * longer allowed to mutate it. Keeping setLit as a deliberate no-op also makes
 * any stale legacy call harmless instead of rewriting a Tank BlockState.
 */
public class GlowingFoundryTankBlock extends FoundryTankBlock {

    public static final MapCodec<GlowingFoundryTankBlock> CODEC =
            simpleCodec(GlowingFoundryTankBlock::new);

    public static final BooleanProperty LIT =
            BlockStateProperties.LIT;

    public static final int MOLTEN_LIGHT_LEVEL = 6;

    public GlowingFoundryTankBlock(
            Properties properties
    ) {
        super(properties);

        registerDefaultState(
                defaultBlockState()
                        .setValue(LIT, false)
        );
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    /**
     * Compatibility no-op. Molten light is rendered by the Controller; Tank
     * BlockStates are never rewritten for liquid/storage state.
     */
    public static void setLit(
            Level level,
            BlockPos pos,
            boolean lit
    ) {
        // Intentionally empty.
    }
}
