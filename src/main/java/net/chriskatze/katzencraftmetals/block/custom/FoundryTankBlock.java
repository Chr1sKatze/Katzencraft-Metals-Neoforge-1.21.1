package net.chriskatze.katzencraftmetals.block.custom;

import com.mojang.serialization.MapCodec;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.BlockAndTintGetter;
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

/**
 * Dumb physical Foundry vessel block.
 *
 * Important invariant:
 * the six face properties are LOGICAL Tank connections, not merely a statement
 * that another Tank happens to occupy the neighboring block.
 *
 * This allows two independent Foundry vessels to sit directly beside each
 * other while keeping a real wall between them.
 *
 * Tanks remain ordinary blocks: no BlockEntity, no ticking, no molten storage,
 * and no persistent ownership ID.
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

    // Preserve the tank body and add one simple enclosing box for the raised custom hatch.
    private static final VoxelShape OPEN_HATCH_SHAPE = Shapes.or(
            Shapes.block(), Block.box(4, 16, 4, 12, 18, 12));

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return state.getValue(HATCH_OPEN) ? OPEN_HATCH_SHAPE : Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                           CollisionContext context) {
        return getShape(state, level, pos, context);
    }

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
    public boolean shouldDisplayFluidOverlay(BlockState state, BlockAndTintGetter level,
                                             BlockPos pos, FluidState fluid) {
        // Keep neighboring water visible through the glass. Like vanilla glass,
        // request its still-water overlay instead of the flowing-water side sprite.
        return fluid.is(FluidTags.WATER) || super.shouldDisplayFluidOverlay(state, level, pos, fluid);
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
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        if (context.getPlayer() != null && context.getPlayer().isSpectator()) return null;

        Level level =
                context.getLevel();

        BlockPos pos =
                context.getClickedPos();

        Direction clickedFace =
                context.getClickedFace();

        /*
         * BlockPlaceContext#getClickedPos is the new Tank position. The block
         * that the player actually aimed at is one step opposite the clicked
         * face.
         *
         * Example:
         * clicking the EAST face of a Tank places at tank.east(), so the block
         * actually targeted is newPos.west().
         */
        BlockPos clickedAgainstPos =
                pos.relative(
                        clickedFace.getOpposite()
                );

        boolean aimedAtTank =
                isTank(
                        level,
                        clickedAgainstPos
                );

        Player player =
                context.getPlayer();

        boolean startIndependentLayout =
                !aimedAtTank
                        && player != null
                        && player.isShiftKeyDown();

        Set<BlockPos> layoutToJoin;

        if (aimedAtTank) {
            /*
             * Explicitly aiming at an existing Tank is the strongest placement
             * signal: extend exactly that Tank's logical vessel.
             *
             * Other physically touching Tank layouts are ignored.
             */
            layoutToJoin =
                    collectLogicalComponent(
                            level,
                            clickedAgainstPos
                    );
        } else if (startIndependentLayout) {
            /*
             * Shift-placement while NOT aiming at a Tank intentionally starts a
             * brand-new logical vessel. It may physically touch any number of
             * existing vessels, but none of those faces are connected.
             */
            layoutToJoin =
                    Set.of();
        } else {
            /*
             * Preserve convenient ordinary placement:
             *
             * - no adjacent layout -> start a normal new layout
             * - exactly one adjacent layout -> extend it
             * - multiple independent adjacent layouts -> ambiguous, reject
             *
             * To choose one layout in the ambiguous case, aim directly at one
             * of its Tanks. To create a third independent layout, Shift-place
             * without aiming at a Tank.
             */
            layoutToJoin =
                    findSingleAdjacentLogicalComponent(
                            level,
                            pos
                    );

            if (layoutToJoin == null) {
                return null;
            }
        }

        Set<BlockPos> proposedLayout =
                new HashSet<>(
                        layoutToJoin
                );

        proposedLayout.add(
                pos.immutable()
        );

        /*
         * The size and simple-fluid checks apply to the LOGICAL vessel being
         * extended, not to every Tank block that happens to touch it.
         */
        if (!fitsLogicalVesselLimit(proposedLayout)) {
            return null;
        }

        BlockState placementState =
                defaultBlockState()
                        .setValue(HATCH_OPEN, false);

        /*
         * Connect only to neighboring Tanks that belong to the chosen logical
         * component.
         *
         * An independent Shift-placed Tank therefore starts with all six face
         * connections false even when another vessel directly touches it.
         */
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos =
                    pos.relative(direction);

            boolean connect =
                    layoutToJoin.contains(
                            neighborPos
                    );

            placementState =
                    placementState.setValue(
                            connectionProperty(direction),
                            connect
                    );
        }

        return placementState;
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
        BooleanProperty property =
                connectionProperty(direction);

        boolean tankNeighbor =
                neighborState.getBlock()
                        instanceof FoundryTankBlock;

        boolean connected =
                false;

        if (tankNeighbor) {
            /*
             * Connection hand-shake:
             *
             * During placement the NEW Tank already knows whether it wants this
             * edge, while the existing Tank still has false on its old air face.
             * Using OR lets that one true edge propagate to the opposite Tank.
             *
             * Independent neighboring vessels have false/false, so they remain
             * separate permanently.
             */
            BooleanProperty oppositeProperty =
                    connectionProperty(
                            direction.getOpposite()
                    );

            connected =
                    state.getValue(property)
                            || neighborState.getValue(
                            oppositeProperty
                    );
        }

        BlockState updated =
                state.setValue(
                        property,
                        connected
                );

        /*
         * A Tank physically occupying the block above obstructs the intake
         * hatch even if the two Tanks belong to separate logical vessels.
         */
        if (
                direction == Direction.UP
                        && tankNeighbor
                        && updated.getValue(HATCH_OPEN)
        ) {
            updated =
                    updated.setValue(
                            HATCH_OPEN,
                            false
                    );
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
        super.onPlace(
                state,
                level,
                pos,
                oldState,
                movedByPiston
        );

        if (
                !level.isClientSide()
                        && !oldState.is(
                        state.getBlock()
                )
        ) {
            FoundryTankNetwork.handleTankPlaced(
                    level,
                    pos,
                    pos
            );
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
                        && !state.is(
                        newState.getBlock()
                );

        /*
         * Let the vanilla Block removal callback finish first. Only after that
         * do we invalidate Controller caches.
         */
        super.onRemove(
                state,
                level,
                pos,
                newState,
                movedByPiston
        );

        if (removedTank) {
            /*
             * Faucets are physical attachments of this exact Tank and cannot
             * remain floating after their supporting Tank is destroyed.
             */
            destroyAttachedFaucets(
                    level,
                    pos
            );

            FoundryTankNetwork.handleTankPlaced(
                    level,
                    pos,
                    pos
            );
        }
    }

    /**
     * Returns the one logical Tank component directly neighboring placedPos.
     *
     * null means the placement is ambiguous because two or more independent
     * logical vessels touch the new position.
     *
     * An empty Set means no Tank vessel touches the position.
     */
    @Nullable
    private static Set<BlockPos> findSingleAdjacentLogicalComponent(
            LevelAccessor level,
            BlockPos placedPos
    ) {
        Set<BlockPos> checkedPositions =
                new HashSet<>();

        Set<BlockPos> foundComponent =
                null;

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos =
                    placedPos.relative(
                            direction
                    );

            if (
                    checkedPositions.contains(
                            neighborPos
                    )
                            || !isTank(
                            level,
                            neighborPos
                    )
            ) {
                continue;
            }

            Set<BlockPos> component =
                    collectLogicalComponent(
                            level,
                            neighborPos
                    );

            checkedPositions.addAll(
                    component
            );

            if (foundComponent != null) {
                /*
                 * This is a genuinely different component because positions of
                 * the first component were added to checkedPositions.
                 */
                return null;
            }

            foundComponent =
                    component;
        }

        return foundComponent != null
                ? foundComponent
                : Set.of();
    }

    /**
     * Collects exactly one logical Tank vessel.
     *
     * Physical adjacency alone is not enough. An edge is traversable only when
     * both Tank BlockStates agree that their shared face is connected.
     */
    private static Set<BlockPos> collectLogicalComponent(
            LevelAccessor level,
            BlockPos startPos
    ) {
        Set<BlockPos> connected =
                new HashSet<>();

        if (!isTank(level, startPos)) {
            return connected;
        }

        ArrayDeque<BlockPos> queue =
                new ArrayDeque<>();

        BlockPos start =
                startPos.immutable();

        connected.add(start);
        queue.addLast(start);

        while (!queue.isEmpty()) {
            BlockPos current =
                    queue.removeFirst();

            for (Direction direction : Direction.values()) {
                if (
                        !areTanksConnected(
                                level,
                                current,
                                direction
                        )
                ) {
                    continue;
                }

                BlockPos next =
                        current.relative(
                                direction
                        );

                if (!connected.add(
                        next.immutable()
                )) {
                    continue;
                }

                queue.addLast(
                        next.immutable()
                );
            }
        }

        return connected;
    }

    /**
     * Tests the prospective logical vessel after placement.
     *
     * Independent adjacent Tank layouts are deliberately absent from this Set,
     * so they do not count against one another's 4x4x4 / 64-Tank limits.
     */
    private static boolean fitsLogicalVesselLimit(
            Set<BlockPos> positions
    ) {
        if (
                positions == null
                        || positions.isEmpty()
        ) {
            return false;
        }

        if (
                positions.size()
                        > FoundryTankNetwork.MAX_TANK_COUNT
        ) {
            return false;
        }

        int minX =
                Integer.MAX_VALUE;
        int maxX =
                Integer.MIN_VALUE;
        int minY =
                Integer.MAX_VALUE;
        int maxY =
                Integer.MIN_VALUE;
        int minZ =
                Integer.MAX_VALUE;
        int maxZ =
                Integer.MIN_VALUE;

        for (BlockPos position : positions) {
            minX =
                    Math.min(
                            minX,
                            position.getX()
                    );

            maxX =
                    Math.max(
                            maxX,
                            position.getX()
                    );

            minY =
                    Math.min(
                            minY,
                            position.getY()
                    );

            maxY =
                    Math.max(
                            maxY,
                            position.getY()
                    );

            minZ =
                    Math.min(
                            minZ,
                            position.getZ()
                    );

            maxZ =
                    Math.max(
                            maxZ,
                            position.getZ()
                    );
        }

        if (
                maxX - minX + 1
                        > FoundryTankNetwork.MAX_LONG_SIDE
                        || maxZ - minZ + 1
                        > FoundryTankNetwork.MAX_LONG_SIDE
                        || maxY - minY + 1
                        > FoundryTankNetwork.MAX_HEIGHT
        ) {
            return false;
        }

        /*
         * Keep the existing simple-fluid invariant:
         * every bottom-up prefix of one logical vessel must remain connected.
         */
        return hasBottomUpConnectedFill(
                positions
        );
    }

    /**
     * Returns true when every bottom-up prefix of this one logical Tank vessel
     * is face-connected geometrically.
     *
     * The input Set has already been produced from logical connection edges, so
     * unrelated touching vessels are not present here.
     */
    private static boolean hasBottomUpConnectedFill(
            Set<BlockPos> positions
    ) {
        if (
                positions == null
                        || positions.isEmpty()
        ) {
            return false;
        }

        int minY =
                Integer.MAX_VALUE;

        int maxY =
                Integer.MIN_VALUE;

        for (BlockPos position : positions) {
            minY =
                    Math.min(
                            minY,
                            position.getY()
                    );

            maxY =
                    Math.max(
                            maxY,
                            position.getY()
                    );
        }

        for (int y = minY; y <= maxY; y++) {
            Set<BlockPos> prefix =
                    new HashSet<>();

            for (BlockPos position : positions) {
                if (position.getY() <= y) {
                    prefix.add(
                            position
                    );
                }
            }

            if (
                    !prefix.isEmpty()
                            && !isFaceConnected(
                            prefix
                    )
            ) {
                return false;
            }
        }

        return true;
    }

    private static boolean isFaceConnected(
            Set<BlockPos> positions
    ) {
        if (positions.isEmpty()) {
            return true;
        }

        Set<BlockPos> visited =
                new HashSet<>();

        ArrayDeque<BlockPos> queue =
                new ArrayDeque<>();

        BlockPos start =
                positions.iterator()
                        .next();

        visited.add(start);
        queue.addLast(start);

        while (!queue.isEmpty()) {
            BlockPos current =
                    queue.removeFirst();

            for (Direction direction : Direction.values()) {
                BlockPos next =
                        current.relative(
                                direction
                        );

                if (
                        positions.contains(next)
                                && visited.add(next)
                ) {
                    queue.addLast(next);
                }
            }
        }

        return visited.size()
                == positions.size();
    }

    /**
     * Shared logical-edge test used by both placement and the Controller's
     * structure resolver.
     *
     * Both sides must agree. This makes the saved BlockStates themselves the
     * persistent vessel boundary without introducing Tank BlockEntities or IDs.
     */
    public static boolean areTanksConnected(
            LevelAccessor level,
            BlockPos firstPos,
            Direction direction
    ) {
        if (
                level == null
                        || firstPos == null
                        || direction == null
        ) {
            return false;
        }

        BlockPos secondPos =
                firstPos.relative(
                        direction
                );

        BlockState firstState =
                level.getBlockState(
                        firstPos
                );

        BlockState secondState =
                level.getBlockState(
                        secondPos
                );

        if (
                !(firstState.getBlock()
                        instanceof FoundryTankBlock)
                        || !(secondState.getBlock()
                        instanceof FoundryTankBlock)
        ) {
            return false;
        }

        return firstState.getValue(
                connectionProperty(direction)
        )
                && secondState.getValue(
                connectionProperty(
                        direction.getOpposite()
                )
        );
    }

    private static void destroyAttachedFaucets(
            Level level,
            BlockPos tankPos
    ) {
        for (Direction direction : Direction.values()) {
            BlockPos faucetPos =
                    tankPos.relative(
                            direction
                    );

            BlockState faucetState =
                    level.getBlockState(
                            faucetPos
                    );

            if (
                    !(faucetState.getBlock()
                            instanceof FoundryFaucetBlock)
                            || !faucetState.hasProperty(
                            FoundryFaucetBlock.FACING
                    )
            ) {
                continue;
            }

            Direction faucetFacing =
                    faucetState.getValue(
                            FoundryFaucetBlock.FACING
                    );

            /*
             * The Faucet must point away from THIS Tank.
             */
            if (faucetFacing != direction) {
                continue;
            }

            level.destroyBlock(
                    faucetPos,
                    true
            );
        }
    }

    private static boolean isTank(
            LevelAccessor level,
            BlockPos pos
    ) {
        return level.getBlockState(pos)
                .getBlock()
                instanceof FoundryTankBlock;
    }

    private static BooleanProperty connectionProperty(
            Direction direction
    ) {
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
