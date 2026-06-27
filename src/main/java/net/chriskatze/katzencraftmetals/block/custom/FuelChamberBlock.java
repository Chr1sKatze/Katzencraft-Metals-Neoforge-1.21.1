package net.chriskatze.katzencraftmetals.block.custom;

import com.mojang.serialization.MapCodec;
import net.chriskatze.katzencraftmetals.block.entity.FuelChamberBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class FuelChamberBlock extends BaseEntityBlock {

    public static final MapCodec<FuelChamberBlock> CODEC =
            simpleCodec(FuelChamberBlock::new);

    private static final ThreadLocal<PlacementIntent> PENDING_PLACEMENT =
            new ThreadLocal<>();

    public FuelChamberBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new FuelChamberBlockEntity(
                pos,
                state
        );
    }

    // =========================
    // AUTOMATIC ASSIGNMENT
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

        BlockEntity blockEntity =
                level.getBlockEntity(pos);

        if (
                blockEntity
                        instanceof FuelChamberBlockEntity fuelChamber
        ) {
            fuelChamber.tryAutoAssign(
                    placementIntent != null
                            && placementIntent.placedPos()
                            .equals(pos)
                            ? placementIntent.clickedAgainstPos()
                            : null
            );
        }
    }

    // =========================
    // REMOVAL
    // =========================

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity =
                    level.getBlockEntity(pos);

            if (
                    blockEntity
                            instanceof FuelChamberBlockEntity fuelChamber
            ) {
                Containers.dropContents(
                        level,
                        pos,
                        fuelChamber.getFuelInventory()
                );
            }
        }

        super.onRemove(
                state,
                level,
                pos,
                newState,
                movedByPiston
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

        if (
                blockEntity
                        instanceof FuelChamberBlockEntity fuelChamber
                        && player
                        instanceof ServerPlayer serverPlayer
        ) {
            serverPlayer.openMenu(
                    fuelChamber,
                    buffer ->
                            buffer.writeBlockPos(pos)
            );

            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    private record PlacementIntent(
            BlockPos placedPos,
            BlockPos clickedAgainstPos
    ) {
    }
}
