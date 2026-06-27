package net.chriskatze.katzencraftmetals.block.custom;

import com.mojang.serialization.MapCodec;
import net.chriskatze.katzencraftmetals.block.entity.FuelChamberBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

public class FuelChamberBlock extends BaseEntityBlock {

    public static final MapCodec<FuelChamberBlock> CODEC =
            simpleCodec(FuelChamberBlock::new);

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
        return new FuelChamberBlockEntity(pos, state);
    }

    /*
     * Drop the stored coal when the block is destroyed.
     */
    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity instanceof FuelChamberBlockEntity fuelChamber) {
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

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof FuelChamberBlockEntity fuelChamber
                && player instanceof ServerPlayer serverPlayer) {

            serverPlayer.openMenu(
                    fuelChamber,
                    buffer -> buffer.writeBlockPos(pos)
            );

            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }
}