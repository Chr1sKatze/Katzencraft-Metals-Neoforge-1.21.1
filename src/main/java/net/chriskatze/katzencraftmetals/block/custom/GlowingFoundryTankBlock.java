package net.chriskatze.katzencraftmetals.block.custom;

import com.mojang.serialization.MapCodec;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Foundry Tank variant that emits faint vanilla block light while its local
 * Tank section visibly contains molten metal.
 *
 * The existing closed-Tank structure, storage, renderer, and network logic
 * remain unchanged.
 */
public class GlowingFoundryTankBlock extends FoundryTankBlock {

    public static final MapCodec<GlowingFoundryTankBlock> CODEC =
            simpleCodec(GlowingFoundryTankBlock::new);

    public static final BooleanProperty LIT =
            BlockStateProperties.LIT;

    public static final int MOLTEN_LIGHT_LEVEL = 6;

    /*
     * Checking every four ticks avoids doing a network lookup for every
     * Tank every server tick. Tank positions are offset across the interval
     * so a large Foundry does not update every Tank in the same tick.
     */
    private static final int LIGHT_CHECK_INTERVAL = 4;

    private static final float MOLTEN_EPSILON =
            0.0001f;

    public GlowingFoundryTankBlock(
            Properties properties
    ) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(
                                LIT,
                                false
                        )
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        super.createBlockStateDefinition(builder);

        builder.add(LIT);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType
    ) {
        if (level.isClientSide()) {
            return null;
        }

        return createTickerHelper(
                blockEntityType,
                ModBlockEntities.FOUNDRY_TANK.get(),
                GlowingFoundryTankBlock::serverTick
        );
    }

    private static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            FoundryTankBlockEntity tank
    ) {
        long positionOffset =
                Math.floorMod(
                        pos.asLong(),
                        LIGHT_CHECK_INTERVAL
                );

        if (
                Math.floorMod(
                        level.getGameTime()
                                + positionOffset,
                        LIGHT_CHECK_INTERVAL
                ) != 0
        ) {
            return;
        }

        boolean shouldBeLit =
                tank.getLocalVisualMoltenAmount()
                        > MOLTEN_EPSILON;

        boolean currentlyLit =
                state.getValue(LIT);

        if (currentlyLit == shouldBeLit) {
            return;
        }

        level.setBlock(
                pos,
                state.setValue(
                        LIT,
                        shouldBeLit
                ),
                Block.UPDATE_CLIENTS
        );

        /*
         * Explicitly ask the vanilla light engine to recalculate this
         * position. This makes both the appearance and disappearance of
         * the level-6 light propagate promptly.
         */
        level.getLightEngine()
                .checkBlock(pos);
    }
}
