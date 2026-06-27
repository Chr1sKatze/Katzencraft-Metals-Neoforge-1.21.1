package net.chriskatze.katzencraftmetals.block.custom;

import com.mojang.serialization.MapCodec;
import net.chriskatze.katzencraftmetals.block.entity.CastingCauldronBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CastingCauldronBlock extends BaseEntityBlock {

    public enum CastState implements StringRepresentable {

        EMPTY("empty"),
        MOLTEN("molten"),
        COOLED("cooled");

        private final String serializedName;

        CastState(String serializedName) {
            this.serializedName = serializedName;
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

    public static final MapCodec<CastingCauldronBlock> CODEC =
            simpleCodec(CastingCauldronBlock::new);

    public CastingCauldronBlock(Properties properties) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(
                                CAST_STATE,
                                CastState.EMPTY
                        )
        );
    }

    private static final VoxelShape SHAPE = Shapes.or(
            /*
             * Basin floor
             */
            Block.box(
                    0, 2, 0,
                    16, 4, 16
            ),

            /*
             * Walls
             */
            Block.box(
                    0, 4, 0,
                    16, 16, 2
            ),
            Block.box(
                    0, 4, 14,
                    16, 16, 16
            ),
            Block.box(
                    0, 4, 2,
                    2, 16, 14
            ),
            Block.box(
                    14, 4, 2,
                    16, 16, 14
            ),

            /*
             * Feet
             */
            Block.box(
                    0, 0, 0,
                    5, 2, 5
            ),
            Block.box(
                    11, 0, 0,
                    16, 2, 5
            ),
            Block.box(
                    0, 0, 11,
                    5, 2, 16
            ),
            Block.box(
                    11, 0, 11,
                    16, 2, 16
            )
    );

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
        return SHAPE;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(CAST_STATE);
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
        return new CastingCauldronBlockEntity(
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
                ModBlockEntities.CASTING_CAULDRON.get(),
                CastingCauldronBlockEntity::serverTick
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

        if (!(blockEntity instanceof CastingCauldronBlockEntity cauldron)) {
            return InteractionResult.PASS;
        }

        if (cauldron.isEmpty()) {
            player.displayClientMessage(
                    Component.literal(
                            "The iron block has finished cooling."
                    ),
                    true
            );

            return InteractionResult.CONSUME;
        }

        if (!cauldron.isCooled()) {
            player.displayClientMessage(
                    Component.literal(
                            "The molten metal is still cooling."
                    ),
                    true
            );

            return InteractionResult.CONSUME;
        }

        ItemStack result =
                cauldron.takeResult();

        if (result.isEmpty()) {
            return InteractionResult.CONSUME;
        }

        if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }

        player.displayClientMessage(
                Component.literal(
                        "The iron ingot has finished cooling."
                ),
                true
        );

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
                    level.getBlockEntity(pos);

            if (
                    blockEntity instanceof CastingCauldronBlockEntity cauldron
                            && cauldron.isCooled()
            ) {
                ItemStack result =
                        cauldron.getResultCopy();

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