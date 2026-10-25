package net.chriskatze.katzencraftmetals.block.custom;

import com.mojang.serialization.MapCodec;
import net.chriskatze.katzencraftmetals.block.entity.FoundryBasinBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTableBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class FoundryTableBlock extends FoundryBasinBlock {
    @Override protected boolean supportsContainedFluids() { return false; }

    public static final MapCodec<FoundryTableBlock> CODEC = simpleCodec(FoundryTableBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape[] SHAPES = makeShapes();

    private static VoxelShape[] makeShapes() {
        VoxelShape north = Shapes.or(box(0,0,0,16,10,16),
                box(0,10,0,16,11,1), box(0,10,15,16,11,16),
                box(0,10,1,1,11,15), box(15,10,1,16,11,15),
                box(1,10,1,6,13,15), box(10,10,1,15,13,15),
                box(6,10,1,10,13,4), box(6,10,12,10,13,15),
                box(6,10,4,10,10.5,12));
        VoxelShape[] shapes = new VoxelShape[]{north, Shapes.empty(), Shapes.empty(), Shapes.empty()};
        for (int i=1;i<4;i++) {
            final VoxelShape[] result = {Shapes.empty()};
            shapes[i-1].forAllBoxes((x1,y1,z1,x2,y2,z2) ->
                    result[0] = Shapes.or(result[0], Shapes.box(1-z2,y1,x1,1-z1,y2,x2)));
            shapes[i] = result[0];
        }
        return shapes;
    }

    public static int modelRotation(Direction facing) {
        return switch (facing) { case EAST -> 90; case SOUTH -> 180; case WEST -> 270; default -> 0; };
    }

    public FoundryTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite().getClockWise());
    }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPES[modelRotation(state.getValue(FACING)) / 90]; }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPES[modelRotation(state.getValue(FACING)) / 90]; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new FoundryTableBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.FOUNDRY_TABLE.get(), FoundryBasinBlockEntity::serverTick);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isSpectator()) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof FoundryTableBlockEntity table)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ItemStack result = table.getResultCopy();
        if (!result.isEmpty() && player.getInventory().add(result)) {
            table.takeResult();
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, .2f, 1.6f + level.random.nextFloat() * .4f);
        }
        return InteractionResult.CONSUME;
    }
}
