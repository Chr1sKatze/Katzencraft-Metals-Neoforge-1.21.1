package net.chriskatze.katzencraftmetals.block.custom;

import com.mojang.serialization.MapCodec;
import net.chriskatze.katzencraftmetals.block.entity.FoundryControllerBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Dumb physical Foundry vessel block.
 *
 * Important invariant:
 * one face-connected physical Tank component is one vessel candidate and may
 * never exceed the hard 4 x 4 x 4 / 64-Tank limit. The Controller therefore
 * never has to claim an arbitrary subset of a larger visually-connected mass.
 *
 * Tanks remain ordinary blocks: no BlockEntity, no ticking, no molten storage,
 * and no persistent ownership state.
 */
public class FoundryTankBlock extends Block {

    public static final MapCodec<FoundryTankBlock> CODEC =
            simpleCodec(FoundryTankBlock::new);

    public static final BooleanProperty NORTH =
            BooleanProperty.create("north");
    public static final BooleanProperty EAST =
            BooleanProperty.create("east");
    public static final BooleanProperty SOUTH =
            BooleanProperty.create("south");
    public static final BooleanProperty WEST =
            BooleanProperty.create("west");
    public static final BooleanProperty UP =
            BooleanProperty.create("up");
    public static final BooleanProperty DOWN =
            BooleanProperty.create("down");
    public static final BooleanProperty HATCH_OPEN =
            BooleanProperty.create("hatch_open");

    public FoundryTankBlock(Properties properties) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(NORTH, false)
                        .setValue(EAST, false)
                        .setValue(SOUTH, false)
                        .setValue(WEST, false)
                        .setValue(UP, false)
                        .setValue(DOWN, false)
                        .setValue(HATCH_OPEN, false)
        );
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

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

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        /*
         * Do not allow the physical component to become larger than the same
         * maximum the Controller/storage understand. This is the key invariant
         * that prevents a 5-wide (or larger) visually-connected vessel while
         * only four rows/columns are actually owned by the Controller.
         */
        if (!fitsPhysicalVesselLimit(level, pos)) {
            return null;
        }

        /*
         * Never bridge two already-active Foundries together with one Tank.
         * Without this guard a single physical component would suddenly have
         * two storage owners. Separate Foundries may exist anywhere in the
         * world; just keep their Tank components physically separate.
         */
        if (bridgesDifferentActiveFoundries(level, pos)) {
            return null;
        }

        return defaultBlockState()
                .setValue(NORTH, isTank(level, pos.north()))
                .setValue(EAST, isTank(level, pos.east()))
                .setValue(SOUTH, isTank(level, pos.south()))
                .setValue(WEST, isTank(level, pos.west()))
                .setValue(UP, isTank(level, pos.above()))
                .setValue(DOWN, isTank(level, pos.below()))
                .setValue(HATCH_OPEN, false);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        BooleanProperty property = connectionProperty(direction);
        boolean connected = neighborState.getBlock() instanceof FoundryTankBlock;

        BlockState updated = state.setValue(property, connected);

        if (
                direction == Direction.UP
                        && connected
                        && updated.getValue(HATCH_OPEN)
        ) {
            updated = updated.setValue(HATCH_OPEN, false);
        }

        return updated;
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston
    ) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (
                !level.isClientSide()
                        && !oldState.is(state.getBlock())
        ) {
            FoundryTankNetwork.handleTankPlaced(level, pos, pos);
        }
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        boolean removedTank =
                !level.isClientSide()
                        && !state.is(newState.getBlock());

        /*
         * Let the vanilla Block removal callback finish first. Only after that
         * do we invalidate Controller caches. This leaves no Foundry callback
         * between "dirty" and returning from the Tank removal path.
         */
        super.onRemove(state, level, pos, newState, movedByPiston);

        if (removedTank) {
            /*
             * Never resolve the structure, rewrite Tanks, or send Tank states
             * here. Vanilla remains the sole owner of the actual block removal.
             */
            FoundryTankNetwork.handleTankPlaced(level, pos, pos);
        }
    }

    /**
     * Tests the component that would exist after placing a Tank at placedPos.
     * Only the hard physical envelope/count is checked here; more specialized
     * fluid-shape validation still belongs to the Controller.
     */
    private static boolean fitsPhysicalVesselLimit(
            LevelAccessor level,
            BlockPos placedPos
    ) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();

        BlockPos start = placedPos.immutable();
        visited.add(start);
        queue.addLast(start);

        int minX = start.getX();
        int maxX = start.getX();
        int minY = start.getY();
        int maxY = start.getY();
        int minZ = start.getZ();
        int maxZ = start.getZ();

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();

            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);

                if (next.equals(placedPos) || visited.contains(next)) {
                    continue;
                }

                if (!isTank(level, next)) {
                    continue;
                }

                BlockPos immutable = next.immutable();
                visited.add(immutable);
                queue.addLast(immutable);

                if (visited.size() > FoundryTankNetwork.MAX_TANK_COUNT) {
                    return false;
                }

                minX = Math.min(minX, immutable.getX());
                maxX = Math.max(maxX, immutable.getX());
                minY = Math.min(minY, immutable.getY());
                maxY = Math.max(maxY, immutable.getY());
                minZ = Math.min(minZ, immutable.getZ());
                maxZ = Math.max(maxZ, immutable.getZ());

                if (
                        maxX - minX + 1 > FoundryTankNetwork.MAX_LONG_SIDE
                                || maxZ - minZ + 1 > FoundryTankNetwork.MAX_LONG_SIDE
                                || maxY - minY + 1 > FoundryTankNetwork.MAX_HEIGHT
                ) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean bridgesDifferentActiveFoundries(
            Level level,
            BlockPos placedPos
    ) {
        Set<UUID> controllerIds = new HashSet<>();

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = placedPos.relative(direction);

            if (!isTank(level, neighborPos)) {
                continue;
            }

            FoundryTankNetwork network =
                    FoundryTankNetwork.find(level, neighborPos);

            if (network == null || !network.isActive()) {
                continue;
            }

            FoundryControllerBlockEntity controller =
                    network.getAttachedController();

            if (controller == null) {
                continue;
            }

            controllerIds.add(controller.getControllerId());

            if (controllerIds.size() > 1) {
                return true;
            }
        }

        return false;
    }

    private static boolean isTank(LevelAccessor level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof FoundryTankBlock;
    }

    private static BooleanProperty connectionProperty(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                NORTH,
                EAST,
                SOUTH,
                WEST,
                UP,
                DOWN,
                HATCH_OPEN
        );
    }
}
