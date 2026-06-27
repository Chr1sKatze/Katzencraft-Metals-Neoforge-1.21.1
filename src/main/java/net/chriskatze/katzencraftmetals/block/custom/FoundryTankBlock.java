package net.chriskatze.katzencraftmetals.block.custom;

import com.mojang.serialization.MapCodec;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class FoundryTankBlock extends BaseEntityBlock {

    public static final MapCodec<FoundryTankBlock> CODEC =
            simpleCodec(FoundryTankBlock::new);

    /*
     * getStateForPlacement contains the clicked face information, while
     * setPlacedBy is where the Tank BlockEntity already exists.
     *
     * Minecraft performs both calls synchronously on the same server thread,
     * so this small ThreadLocal safely carries the deliberate clicked block
     * position from one method to the other without needing a custom BlockItem.
     */
    private static final ThreadLocal<PlacementIntent> PENDING_PLACEMENT =
            new ThreadLocal<>();

    private static boolean dismantlingUpwardColumn;

    public FoundryTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    /*
     * The Tank is visually transparent even though it keeps its normal
     * full-block collision shape.
     *
     * These match the light/visual behavior used by vanilla transparent
     * blocks, preventing the invisible block volume from darkening opaque
     * blocks seen through the Tank.
     */
    @Override
    protected VoxelShape getVisualShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getOcclusionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        return Shapes.empty();
    }

    @Override
    protected int getLightBlock(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        return 0;
    }

    @Override
    protected float getShadeBrightness(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        return 1.0f;
    }

    @Override
    protected boolean propagatesSkylightDown(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        return true;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new FoundryTankBlockEntity(
                pos,
                state
        );
    }

    // =========================
    // AUTOMATIC PLACEMENT LINKING
    // =========================

    @Nullable
    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        if (!context.getLevel().isClientSide()) {
            BlockPos placedPos =
                    context.getClickedPos();

            BlockPos clickedAgainstPos =
                    placedPos.relative(
                            context.getClickedFace()
                                    .getOpposite()
                    );

            PENDING_PLACEMENT.set(
                    new PlacementIntent(
                            placedPos.immutable(),
                            clickedAgainstPos.immutable()
                    )
            );
        }

        return defaultBlockState();
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        super.setPlacedBy(
                level,
                pos,
                state,
                placer,
                stack
        );

        if (level.isClientSide()) {
            return;
        }

        PlacementIntent placementIntent =
                PENDING_PLACEMENT.get();

        PENDING_PLACEMENT.remove();

        if (
                placementIntent == null
                        || !placementIntent.placedPos()
                        .equals(pos)
        ) {
            return;
        }

        boolean joinedActiveNetwork =
                FoundryTankNetwork.handleTankPlaced(
                        level,
                        pos,
                        placementIntent.clickedAgainstPos()
                );

        /*
         * An invalid addition remains a normal unassigned Tank instead of
         * consuming the item and then unexpectedly deleting the block.
         */
        if (
                !joinedActiveNetwork
                        && placer instanceof Player player
                        && FoundryTankNetwork.hasNearbyFoundryCandidate(
                        level,
                        pos
                )
        ) {
            player.displayClientMessage(
                    Component.literal(
                            "This Tank was placed, but it could not join that Foundry layout."
                    ),
                    true
            );
        }
    }

    // =========================
    // UPWARD-ONLY BREAKING
    // =========================

    @Override
    public BlockState playerWillDestroy(
            Level level,
            BlockPos pos,
            BlockState state,
            Player player
    ) {
        if (
                !level.isClientSide()
                        && !dismantlingUpwardColumn
        ) {
            BlockEntity blockEntity =
                    level.getBlockEntity(pos);

            FoundryTankNetwork network =
                    blockEntity instanceof FoundryTankBlockEntity tank
                            ? tank.getNetwork()
                            : null;

            Set<BlockPos> removedPositions =
                    FoundryTankNetwork.findUpwardColumn(
                            level,
                            pos
                    );

            if (!removedPositions.isEmpty()) {
                dismantlingUpwardColumn = true;

                try {
                    boolean dropItems =
                            !player.isCreative();

                    if (network != null) {
                        network.prepareUpwardRemoval(
                                removedPositions
                        );
                    }

                    for (
                            BlockPos faucetPos :
                            FoundryTankNetwork.findAttachedFaucets(
                                    level,
                                    removedPositions
                            )
                    ) {
                        level.destroyBlock(
                                faucetPos,
                                dropItems,
                                player
                        );
                    }

                    /*
                     * Vanilla removes the selected Tank after this method
                     * returns. We only destroy the Tanks above it here.
                     */
                    for (BlockPos tankPos : removedPositions) {
                        if (tankPos.equals(pos)) {
                            continue;
                        }

                        level.destroyBlock(
                                tankPos,
                                dropItems,
                                player
                        );
                    }
                } finally {
                    dismantlingUpwardColumn = false;
                }
            }
        }

        return super.playerWillDestroy(
                level,
                pos,
                state,
                player
        );
    }

    private record PlacementIntent(
            BlockPos placedPos,
            BlockPos clickedAgainstPos
    ) {
    }
}
