package net.chriskatze.katzencraftmetals.block.custom;

import net.chriskatze.katzencraftmetals.metal.MetalNames;
import com.mojang.serialization.MapCodec;
import net.chriskatze.katzencraftmetals.block.entity.FoundryBasinBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.ModBlockEntities;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
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

    private static final VoxelShape SHAPE =
            Shapes.or(
                    Block.box(
                            0, 3, 0,
                            16, 4, 16
                    ),
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
                    Block.box(
                            0, 0, 0,
                            4, 3, 2
                    ),
                    Block.box(
                            0, 0, 2,
                            2, 3, 4
                    ),
                    Block.box(
                            12, 0, 0,
                            16, 3, 2
                    ),
                    Block.box(
                            14, 0, 2,
                            16, 3, 4
                    ),
                    Block.box(
                            0, 0, 14,
                            4, 3, 16
                    ),
                    Block.box(
                            0, 0, 12,
                            2, 3, 14
                    ),
                    Block.box(
                            12, 0, 14,
                            16, 3, 16
                    ),
                    Block.box(
                            14, 0, 12,
                            16, 3, 14
                    )
            );

    public FoundryBasinBlock(
            Properties properties
    ) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(
                                CAST_STATE,
                                CastState.EMPTY
                        )
        );
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
