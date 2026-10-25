package net.chriskatze.katzencraftmetals.block.custom;

import net.chriskatze.katzencraftmetals.metal.MetalNames;
import com.mojang.serialization.MapCodec;
import net.chriskatze.katzencraftmetals.block.entity.FoundryBasinBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.ModBlockEntities;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class FoundryBasinBlock extends BaseEntityBlock {

    // Retain the saved water flag; old water-filled Basins default to a full level of three.
    public static final BooleanProperty CONTAINED_WATER = BooleanProperty.create("contained_water");
    public static final BooleanProperty CONTAINED_LAVA = BooleanProperty.create("contained_lava");
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty FLUID_LEVEL =
            net.minecraft.world.level.block.state.properties.IntegerProperty.create("fluid_level", 1, 3);

    protected boolean supportsContainedFluids() { return true; }

    public static net.minecraft.world.level.material.Fluid containedFluid(BlockState state) {
        if (!state.hasProperty(CONTAINED_WATER)) return Fluids.EMPTY;
        if (state.getValue(CONTAINED_LAVA)) return Fluids.LAVA;
        return state.getValue(CONTAINED_WATER) ? Fluids.WATER : Fluids.EMPTY;
    }

    public static boolean hasContainedFluid(BlockState state) {
        return containedFluid(state) != Fluids.EMPTY;
    }

    public static boolean canContainFluid(BlockState state) {
        return state.hasProperty(CONTAINED_WATER) && state.getValue(CAST_STATE) == CastState.EMPTY
                && !hasContainedFluid(state);
    }

    public static BlockState withFluid(BlockState state, net.minecraft.world.level.material.Fluid fluid, int level) {
        return state.setValue(CONTAINED_WATER, fluid == Fluids.WATER)
                .setValue(CONTAINED_LAVA, fluid == Fluids.LAVA).setValue(FLUID_LEVEL, level);
    }

    public static int lightEmission(BlockState state) {
        return containedFluid(state) == Fluids.LAVA ? 15 : state.getValue(CAST_STATE) == CastState.MOLTEN ? 12 : 0;
    }

    public enum CastState implements StringRepresentable {

        EMPTY("empty"),
        MOLTEN("molten"),
        COOLED("cooled");

        private final String serializedName;

        CastState(
                String serializedName
        ) {
            this.serializedName =
                    serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    public static final EnumProperty<CastState> CAST_STATE =
            EnumProperty.create(
                    "cast_state",
                    CastState.class
            );

    public static final MapCodec<FoundryBasinBlock> CODEC =
            simpleCodec(
                    FoundryBasinBlock::new
            );

    // Exact axis-aligned elements of the authored Basin model, including its thin windows.
    // Selection is independent of the intentionally solid collision volume below.
    private static final VoxelShape SHAPE =
            Shapes.or(
                    Block.box(0, 2, 0, 2, 16, 7),
                    Block.box(14, 2, 7, 16, 4, 9),
                    Block.box(14, 13, 7, 16, 16, 9),
                    Block.box(15, 4, 7, 16, 13, 9),
                    Block.box(0, 4, 7, 1, 13, 9),
                    Block.box(7, 4, 0, 9, 13, 1),
                    Block.box(7, 4, 15, 9, 13, 16),
                    Block.box(14, 2, 9, 16, 16, 16),
                    Block.box(2, 2, 14, 7, 16, 16),
                    Block.box(7, 2, 14, 9, 4, 16),
                    Block.box(7, 13, 14, 9, 16, 16),
                    Block.box(9, 2, 14, 14, 16, 16),
                    Block.box(7, 2, 0, 9, 4, 2),
                    Block.box(2, 2, 0, 7, 16, 2),
                    Block.box(7, 13, 0, 9, 16, 2),
                    Block.box(9, 2, 0, 14, 16, 2),
                    Block.box(14, 2, 0, 16, 16, 7),
                    Block.box(0, 2, 7, 2, 4, 9),
                    Block.box(0, 13, 7, 2, 16, 9),
                    Block.box(0, 2, 9, 2, 16, 16),
                    Block.box(2, 2, 2, 14, 3, 14),
                    Block.box(1, 0, 1, 15, 2, 15)
            ).optimize();

    public FoundryBasinBlock(
            Properties properties
    ) {
        super(properties);

        BlockState initial = stateDefinition.any()
                        .setValue(
                                CAST_STATE,
                                CastState.EMPTY
                        );
        registerDefaultState(supportsContainedFluids() ? withFluid(initial, Fluids.EMPTY, 3) : initial);
    }

    @Override
    protected void randomTick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos,
                              net.minecraft.util.RandomSource random) {
        net.chriskatze.katzencraftmetals.block.entity.FoundryBasinDripstone.schedule(state, level, pos, random.nextFloat());
    }

    @Override
    protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos,
                        net.minecraft.util.RandomSource random) {
        net.chriskatze.katzencraftmetals.block.entity.FoundryBasinDripstone.fill(state, level, pos);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return Shapes.block();
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<
                    Block,
                    BlockState
                    > builder
    ) {
        builder.add(
                CAST_STATE
        );
        if (supportsContainedFluids()) builder.add(CONTAINED_WATER, CONTAINED_LAVA, FLUID_LEVEL);
    }

    @Override
    public RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new FoundryBasinBlockEntity(
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
                ModBlockEntities.FOUNDRY_BASIN.get(),
                FoundryBasinBlockEntity::serverTick
        );
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (supportsContainedFluids() && (stack.is(Items.WATER_BUCKET) || stack.is(Items.LAVA_BUCKET) || stack.is(Items.BUCKET))) {
            if (player.isSpectator()) return ItemInteractionResult.FAIL;
            var fluid = containedFluid(state);
            boolean filling = !stack.is(Items.BUCKET);
            if (filling ? !canContainFluid(state) : fluid == Fluids.EMPTY
                    || (fluid == Fluids.WATER && state.getValue(FLUID_LEVEL) < 3)) {
                return ItemInteractionResult.FAIL;
            }
            var bucketFluid = stack.is(Items.LAVA_BUCKET) ? Fluids.LAVA : Fluids.WATER;
            var sound = filling ? (bucketFluid == Fluids.LAVA ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY)
                    : (fluid == Fluids.LAVA ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL);
            if (!level.isClientSide()) {
                level.setBlock(pos, withFluid(state, filling ? bucketFluid : Fluids.EMPTY, 3), Block.UPDATE_ALL);
                player.setItemInHand(hand, net.minecraft.world.item.ItemUtils.createFilledResult(stack, player,
                        new ItemStack(filling ? Items.BUCKET : fluid == Fluids.LAVA ? Items.LAVA_BUCKET : Items.WATER_BUCKET)));
                level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0f, 1.0f);
                level.gameEvent(player, filling ? net.minecraft.world.level.gameevent.GameEvent.FLUID_PLACE
                        : net.minecraft.world.level.gameevent.GameEvent.FLUID_PICKUP, pos);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (player.isSpectator()) return InteractionResult.PASS;

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(
                        pos
                );

        if (
                !(blockEntity
                        instanceof FoundryBasinBlockEntity basin)
        ) {
            return InteractionResult.PASS;
        }

        if (basin.isEmpty()) {
            player.displayClientMessage(
                    Component.literal(
                            "The foundry basin is empty."
                    ),
                    true
            );

            return InteractionResult.CONSUME;
        }

        if (!basin.isCooled()) {
            Component metalName =
                    ModMoltenMetals.get(
                                    basin.getStoredMetal()
                            )
                            .map(
                                    definition ->
                                            MetalNames.name(definition.id())
                            )
                            .orElse(
                                    Component.literal(
                                            "metal"
                                    )
                            );

            player.displayClientMessage(
                    Component.literal(
                                    "The molten "
                            )
                            .append(
                                    metalName
                            )
                            .append(
                                    " is still cooling."
                            ),
                    true
            );

            return InteractionResult.CONSUME;
        }

        ItemStack result =
                basin.takeResult();

        if (result.isEmpty()) {
            return InteractionResult.CONSUME;
        }

        int countBeforeInsertion = result.getCount();
        boolean inserted = player.getInventory().add(result);

        if (result.getCount() < countBeforeInsertion) {
            // Server broadcast includes the collecting player; no client replay.
            level.playSound(
                    null,
                    pos,
                    SoundEvents.ITEM_PICKUP,
                    SoundSource.PLAYERS,
                    0.2f,
                    1.6f + level.random.nextFloat() * 0.4f
            );
        }

        if (!inserted) {
            player.drop(
                    result,
                    false
            );
        }

        return InteractionResult.CONSUME;
    }

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
                    level.getBlockEntity(
                            pos
                    );

            if (
                    blockEntity
                            instanceof FoundryBasinBlockEntity basin
                            && basin.isCooled()
            ) {
                ItemStack result =
                        basin.getResultCopy();

                if (!result.isEmpty()) {
                    Containers.dropItemStack(
                            level,
                            pos.getX() + 0.5,
                            pos.getY() + 0.5,
                            pos.getZ() + 0.5,
                            result
                    );
                }
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
}
