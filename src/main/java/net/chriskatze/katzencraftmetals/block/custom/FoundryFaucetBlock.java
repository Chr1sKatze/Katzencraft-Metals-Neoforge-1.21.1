package net.chriskatze.katzencraftmetals.block.custom;

import com.mojang.serialization.MapCodec;
import net.chriskatze.katzencraftmetals.block.entity.CastingCauldronBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryFaucetBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class FoundryFaucetBlock extends BaseEntityBlock {

    public static final MapCodec<FoundryFaucetBlock> CODEC =
            simpleCodec(FoundryFaucetBlock::new);

    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    // =========================
    // SHAPES
    // =========================

    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            Block.box(
                    4, 4, 10,
                    12, 6, 16
            ),
            Block.box(
                    4, 6, 10,
                    6, 10, 16
            ),
            Block.box(
                    10, 6, 10,
                    12, 10, 16
            )
    );

    private static final VoxelShape SHAPE_SOUTH = Shapes.or(
            Block.box(
                    4, 4, 0,
                    12, 6, 6
            ),
            Block.box(
                    4, 6, 0,
                    6, 10, 6
            ),
            Block.box(
                    10, 6, 0,
                    12, 10, 6
            )
    );

    private static final VoxelShape SHAPE_EAST = Shapes.or(
            Block.box(
                    0, 4, 4,
                    6, 6, 12
            ),
            Block.box(
                    0, 6, 4,
                    6, 10, 6
            ),
            Block.box(
                    0, 6, 10,
                    6, 10, 12
            )
    );

    private static final VoxelShape SHAPE_WEST = Shapes.or(
            Block.box(
                    10, 4, 4,
                    16, 6, 12
            ),
            Block.box(
                    10, 6, 4,
                    16, 10, 6
            ),
            Block.box(
                    10, 6, 10,
                    16, 10, 12
            )
    );

    public FoundryFaucetBlock(Properties properties) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(
                                FACING,
                                Direction.NORTH
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
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        Direction clickedFace =
                context.getClickedFace();

        Direction facing =
                clickedFace.getAxis().isHorizontal()
                        ? clickedFace
                        : context.getHorizontalDirection().getOpposite();

        return defaultBlockState()
                .setValue(FACING, facing);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new FoundryFaucetBlockEntity(
                pos,
                state
        );
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
                ModBlockEntities.FOUNDRY_FAUCET.get(),
                FoundryFaucetBlockEntity::serverTick
        );
    }

    // =========================
    // INTERACTION
    // =========================

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(pos);

        if (!(blockEntity instanceof FoundryFaucetBlockEntity faucet)) {
            return InteractionResult.PASS;
        }

        /*
         * Right-clicking an active Faucet stops it.
         */
        if (faucet.isPouring()) {
            faucet.stopPouring();

            level.playSound(
                    null,
                    pos,
                    SoundEvents.LEVER_CLICK,
                    SoundSource.BLOCKS,
                    0.5f,
                    0.8f
            );

            player.displayClientMessage(
                    Component.literal(
                            "The faucet has stopped pouring."
                    ),
                    true
            );

            return InteractionResult.CONSUME;
        }

        Direction facing =
                state.getValue(FACING);

        /*
         * Tank behind the Faucet.
         */
        BlockPos tankPosition =
                pos.relative(facing.getOpposite());

        BlockEntity tankBlockEntity =
                level.getBlockEntity(tankPosition);

        if (!(tankBlockEntity instanceof FoundryTankBlockEntity tank)) {
            player.displayClientMessage(
                    Component.literal(
                            "The faucet is not connected to a Foundry Tank."
                    ),
                    true
            );

            return InteractionResult.CONSUME;
        }

        /*
         * Cauldron directly below the Faucet.
         */
        BlockEntity cauldronBlockEntity =
                level.getBlockEntity(pos.below());

        if (!(cauldronBlockEntity instanceof CastingCauldronBlockEntity cauldron)) {
            player.displayClientMessage(
                    Component.literal(
                            "Place a Casting Cauldron below the faucet."
                    ),
                    true
            );

            return InteractionResult.CONSUME;
        }

        if (tank.isEmpty() || tank.getStoredMetal() == null) {
            player.displayClientMessage(
                    Component.literal(
                            "The Foundry Tank is empty."
                    ),
                    true
            );

            return InteractionResult.CONSUME;
        }

        if (cauldron.isFull()) {
            player.displayClientMessage(
                    Component.literal(
                            "The Casting Cauldron is already full."
                    ),
                    true
            );

            return InteractionResult.CONSUME;
        }

        if (!cauldron.canAccept(
                tank.getStoredMetal(),
                FoundryFaucetBlockEntity.TRANSFER_AMOUNT
        )) {
            player.displayClientMessage(
                    Component.literal(
                            "The Casting Cauldron cannot accept this metal."
                    ),
                    true
            );

            return InteractionResult.CONSUME;
        }

        faucet.startPouring();

        level.playSound(
                null,
                pos,
                SoundEvents.LEVER_CLICK,
                SoundSource.BLOCKS,
                0.5f,
                1.2f
        );

        player.displayClientMessage(
                Component.literal(
                        "The faucet has started pouring."
                ),
                true
        );

        return InteractionResult.CONSUME;
    }

    // =========================
    // SHAPE
    // =========================

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return getShape(
                state,
                level,
                pos,
                context
        );
    }
}