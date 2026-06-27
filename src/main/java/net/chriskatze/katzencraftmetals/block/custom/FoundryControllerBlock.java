package net.chriskatze.katzencraftmetals.block.custom;

import com.mojang.serialization.MapCodec;
import net.chriskatze.katzencraftmetals.block.entity.FoundryControllerBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class FoundryControllerBlock extends BaseEntityBlock {

    public static final MapCodec<FoundryControllerBlock> CODEC =
            simpleCodec(FoundryControllerBlock::new);

    public FoundryControllerBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
        );
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
        return new FoundryControllerBlockEntity(pos, state);
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
                ModBlockEntities.FOUNDRY_CONTROLLER.get(),
                FoundryControllerBlockEntity::serverTick
        );
    }

    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;

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

            if (blockEntity instanceof FoundryControllerBlockEntity controller) {
                Containers.dropContents(
                        level,
                        pos,
                        controller.getInputInventory()
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

        BlockEntity blockEntity =
                level.getBlockEntity(pos);

        if (
                blockEntity instanceof FoundryControllerBlockEntity controller
                        && player instanceof ServerPlayer serverPlayer
        ) {
            serverPlayer.openMenu(
                    controller,
                    buffer -> buffer.writeBlockPos(pos)
            );

            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        return this.defaultBlockState()
                .setValue(
                        FACING,
                        context.getHorizontalDirection().getOpposite()
                );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<
                    net.minecraft.world.level.block.Block,
                    BlockState
                    > builder
    ) {
        builder.add(FACING);
    }
}