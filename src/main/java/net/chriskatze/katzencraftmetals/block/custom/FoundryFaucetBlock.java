package net.chriskatze.katzencraftmetals.block.custom;

import com.mojang.serialization.MapCodec;
import net.chriskatze.katzencraftmetals.block.entity.CastingCauldronBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryFaucetBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
                    10, 6, 12,
                    12, 10, 16
            ),
            Block.box(
                    10, 6, 10,
                    12, 8, 12
            ),
            Block.box(
                    4, 6, 10,
                    6, 8, 12
            ),
            Block.box(
                    4, 6, 12,
                    6, 10, 16
            ),
            Block.box(
                    4, 10, 15,
                    6, 11, 16
            ),
            Block.box(
                    5, 12, 15,
                    11, 13, 16
            ),
            Block.box(
                    6, 13, 15,
                    10, 14, 16
            ),
            Block.box(
                    9, 11, 15,
                    12, 12, 16
            ),
            Block.box(
                    4, 11, 15,
                    7, 12, 16
            ),
            Block.box(
                    10, 10, 15,
                    12, 11, 16
            )
    );

    private static final VoxelShape SHAPE_SOUTH = Shapes.or(
            Block.box(
                    4, 4, 0,
                    12, 6, 6
            ),
            Block.box(
                    10, 6, 0,
                    12, 10, 4
            ),
            Block.box(
                    10, 6, 4,
                    12, 8, 6
            ),
            Block.box(
                    4, 6, 4,
                    6, 8, 6
            ),
            Block.box(
                    4, 6, 0,
                    6, 10, 4
            ),
            Block.box(
                    4, 10, 0,
                    6, 11, 1
            ),
            Block.box(
                    5, 12, 0,
                    11, 13, 1
            ),
            Block.box(
                    6, 13, 0,
                    10, 14, 1
            ),
            Block.box(
                    9, 11, 0,
                    12, 12, 1
            ),
            Block.box(
                    4, 11, 0,
                    7, 12, 1
            ),
            Block.box(
                    10, 10, 0,
                    12, 11, 1
            )
    );

    private static final VoxelShape SHAPE_EAST = Shapes.or(
            Block.box(
                    0, 4, 4,
                    6, 6, 12
            ),
            Block.box(
                    0, 6, 10,
                    4, 10, 12
            ),
            Block.box(
                    4, 6, 10,
                    6, 8, 12
            ),
            Block.box(
                    4, 6, 4,
                    6, 8, 6
            ),
            Block.box(
                    0, 6, 4,
                    4, 10, 6
            ),
            Block.box(
                    0, 10, 4,
                    1, 11, 6
            ),
            Block.box(
                    0, 12, 5,
                    1, 13, 11
            ),
            Block.box(
                    0, 13, 6,
                    1, 14, 10
            ),
            Block.box(
                    0, 11, 9,
                    1, 12, 12
            ),
            Block.box(
                    0, 11, 4,
                    1, 12, 7
            ),
            Block.box(
                    0, 10, 10,
                    1, 11, 12
            )
    );

    private static final VoxelShape SHAPE_WEST = Shapes.or(
            Block.box(
                    10, 4, 4,
                    16, 6, 12
            ),
            Block.box(
                    12, 6, 4,
                    16, 10, 6
            ),
            Block.box(
                    10, 6, 4,
                    12, 8, 6
            ),
            Block.box(
                    10, 6, 10,
                    12, 8, 12
            ),
            Block.box(
                    12, 6, 10,
                    16, 10, 12
            ),
            Block.box(
                    15, 10, 10,
                    16, 11, 12
            ),
            Block.box(
                    15, 12, 5,
                    16, 13, 11
            ),
            Block.box(
                    15, 13, 6,
                    16, 14, 10
            ),
            Block.box(
                    15, 11, 4,
                    16, 12, 7
            ),
            Block.box(
                    15, 11, 9,
                    16, 12, 12
            ),
            Block.box(
                    15, 10, 4,
                    16, 11, 6
            )
    );

    /*
     * Larger targeting shapes make the complete visible Faucet area clickable,
     * including the empty space inside its arch.
     *
     * Physical collision continues to use the detailed SHAPE_* definitions.
     */
    private static final VoxelShape SELECTION_SHAPE_NORTH =
            Block.box(
                    4, 4, 10,
                    12, 14, 16
            );

    private static final VoxelShape SELECTION_SHAPE_SOUTH =
            Block.box(
                    4, 4, 0,
                    12, 14, 6
            );

    private static final VoxelShape SELECTION_SHAPE_EAST =
            Block.box(
                    0, 4, 4,
                    6, 14, 12
            );

    private static final VoxelShape SELECTION_SHAPE_WEST =
            Block.box(
                    10, 4, 4,
                    16, 14, 12
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
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        /*
         * Shift-right-click must open the lock menu even when the player holds
         * a block or item. Returning SUCCESS consumes the item interaction before
         * block placement/use can steal the click.
         */
        if (
                player.isShiftKeyDown()
                        && openFaucetLockMenu(
                        level,
                        pos,
                        player
                )
        ) {
            return ItemInteractionResult.SUCCESS;
        }

        return super.useItemOn(
                stack,
                state,
                level,
                pos,
                player,
                hand,
                hitResult
        );
    }

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
         * Shift-right-click opens the per-Faucet output selection menu.
         * This is checked before the normal active-Faucet stop behavior so the
         * player can inspect or change the selection intentionally.
         */
        if (player.isShiftKeyDown()) {
            return openFaucetLockMenu(
                    level,
                    pos,
                    player
            )
                    ? InteractionResult.CONSUME
                    : InteractionResult.PASS;
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

        if (!tank.hasActiveController()) {
            player.displayClientMessage(
                    Component.literal(
                            "This Tank section is not connected to a Foundry Controller."
                    ),
                    true
            );

            return InteractionResult.CONSUME;
        }

        ResourceLocation outputMetal =
                faucet.resolveOutputMetal(
                        tank
                ).orElse(null);

        if (outputMetal == null) {
            player.displayClientMessage(
                    Component.literal(
                            faucet.getSelectedOutputMetalId().isPresent()
                                    ? "The selected metal is not available at this faucet."
                                    : "There is no molten metal available for this faucet."
                    ),
                    true
            );

            return InteractionResult.CONSUME;
        }

        FoundryFaucetBlockEntity.CauldronTarget cauldronTarget =
                FoundryFaucetBlockEntity.findCauldronTarget(
                        level,
                        pos
                );

        if (cauldronTarget == null) {
            player.displayClientMessage(
                    Component.literal(
                            "Place a Casting Cauldron within 3 clear blocks below the faucet."
                    ),
                    true
            );

            return InteractionResult.CONSUME;
        }

        CastingCauldronBlockEntity cauldron =
                cauldronTarget.cauldron();

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
                outputMetal,
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

        faucet.startPouring(outputMetal);

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

    private static boolean openFaucetLockMenu(
            Level level,
            BlockPos pos,
            Player player
    ) {
        if (level.isClientSide()) {
            return true;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(pos);

        if (!(blockEntity instanceof FoundryFaucetBlockEntity faucet)) {
            return false;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(
                    faucet,
                    buffer -> buffer.writeBlockPos(pos)
            );
        }

        return true;
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
            case NORTH -> SELECTION_SHAPE_NORTH;
            case SOUTH -> SELECTION_SHAPE_SOUTH;
            case EAST -> SELECTION_SHAPE_EAST;
            case WEST -> SELECTION_SHAPE_WEST;
            default -> SELECTION_SHAPE_NORTH;
        };
    }

    @Override
    protected VoxelShape getCollisionShape(
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
}
