package net.chriskatze.katzencraftmetals.block.custom;

import com.mojang.serialization.MapCodec;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

            boolean forceSeparateLayout =
                    context.getPlayer() != null
                            && context.getPlayer().isShiftKeyDown()
                            && !context.getLevel()
                            .getBlockState(
                                    clickedAgainstPos
                            )
                            .is(this);

            PENDING_PLACEMENT.set(
                    new PlacementIntent(
                            placedPos.immutable(),
                            clickedAgainstPos.immutable(),
                            forceSeparateLayout
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

        /*
         * A deliberate shift-placement against a non-Tank always creates a new
         * one-Tank layout, even when other Tanks are touching the placed position.
         */
        if (
                placementIntent.forceSeparateLayout()
                        && level.getBlockEntity(pos)
                        instanceof FoundryTankBlockEntity placedTank
        ) {
            placedTank.setOrphanLayoutId(
                    UUID.randomUUID()
            );

            return;
        }

        boolean joinedActiveNetwork =
                FoundryTankNetwork.handleTankPlaced(
                        level,
                        pos,
                        placementIntent.clickedAgainstPos()
                );

        if (!joinedActiveNetwork) {
            boolean startedSeparateLayout =
                    assignOrphanLayoutAfterPlacement(
                            level,
                            pos,
                            placementIntent.clickedAgainstPos()
                    );

            if (
                    startedSeparateLayout
                            && placer instanceof Player player
            ) {
                player.displayClientMessage(
                        Component.literal(
                                "This Tank could not join that layout and started a new Tank layout."
                        ),
                        true
                );
            } else if (
                    placer instanceof Player player
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
    }

    /**
     * Gives every free-standing Tank section its own persistent layout UUID.
     *
     * When the newly placed Tank touches existing orphan layouts:
     *
     * 1. merge all touching layouts when the complete result is valid
     * 2. otherwise prefer the deliberately clicked layout when it is valid
     * 3. otherwise join one unambiguous valid neighboring layout
     * 4. otherwise remain placed as a new one-Tank layout
     *
     * The returned value is true only when the Tank had an orphan neighbor but
     * had to start a separate layout.
     */
    private static boolean assignOrphanLayoutAfterPlacement(
            Level level,
            BlockPos placedPos,
            BlockPos clickedAgainstPos
    ) {
        BlockEntity placedBlockEntity =
                level.getBlockEntity(
                        placedPos
                );

        if (
                !(placedBlockEntity instanceof FoundryTankBlockEntity placedTank)
                        || placedTank.getNetworkId() != null
        ) {
            return false;
        }

        AdjacentOrphanLayouts adjacentLayouts =
                collectAdjacentOrphanLayouts(
                        level,
                        placedPos
                );

        Map<UUID, OrphanLayoutGroup> groups =
                adjacentLayouts.groups();

        if (groups.isEmpty()) {
            placedTank.setOrphanLayoutId(
                    UUID.randomUUID()
            );

            return adjacentLayouts.hadOrphanNeighbor();
        }

        UUID preferredLayoutId =
                getPreferredOrphanLayoutId(
                        level,
                        clickedAgainstPos
                );

        /*
         * First try the natural result: the new Tank bridges every touching
         * orphan layout into one valid section.
         */
        Set<BlockPos> completeMerge =
                new HashSet<>();

        completeMerge.add(
                placedPos.immutable()
        );

        for (OrphanLayoutGroup group : groups.values()) {
            completeMerge.addAll(
                    group.positions()
            );
        }

        if (FoundryTankNetwork.isValidStructure(
                completeMerge
        )) {
            UUID selectedLayoutId =
                    preferredLayoutId != null
                            && groups.containsKey(
                            preferredLayoutId
                    )
                            ? preferredLayoutId
                            : groups.keySet()
                            .iterator()
                            .next();

            applyOrphanLayoutId(
                    level,
                    completeMerge,
                    selectedLayoutId
            );

            return false;
        }

        /*
         * If all touching layouts cannot merge, honor the block the player
         * deliberately clicked whenever that individual connection is valid.
         */
        if (
                preferredLayoutId != null
                        && groups.containsKey(
                        preferredLayoutId
                )
        ) {
            OrphanLayoutGroup preferredGroup =
                    groups.get(
                            preferredLayoutId
                    );

            Set<BlockPos> preferredConnection =
                    withPlacedTank(
                            preferredGroup.positions(),
                            placedPos
                    );

            if (FoundryTankNetwork.isValidStructure(
                    preferredConnection
            )) {
                applyOrphanLayoutId(
                        level,
                        preferredConnection,
                        preferredLayoutId
                );

                return false;
            }
        }

        /*
         * Without a preferred layout, join only when exactly one neighboring
         * layout can accept the Tank. This avoids arbitrary connections when
         * the Tank touches several otherwise valid layouts.
         */
        List<OrphanLayoutGroup> individuallyValid =
                new ArrayList<>();

        for (OrphanLayoutGroup group : groups.values()) {
            if (FoundryTankNetwork.isValidStructure(
                    withPlacedTank(
                            group.positions(),
                            placedPos
                    )
            )) {
                individuallyValid.add(group);
            }
        }

        if (individuallyValid.size() == 1) {
            OrphanLayoutGroup selected =
                    individuallyValid.getFirst();

            applyOrphanLayoutId(
                    level,
                    withPlacedTank(
                            selected.positions(),
                            placedPos
                    ),
                    selected.layoutId()
            );

            return false;
        }

        /*
         * The placement itself is always allowed. It simply starts a new
         * one-Tank layout and therefore renders a frame against every touching
         * incompatible layout.
         */
        placedTank.setOrphanLayoutId(
                UUID.randomUUID()
        );

        return true;
    }

    private static AdjacentOrphanLayouts collectAdjacentOrphanLayouts(
            Level level,
            BlockPos placedPos
    ) {
        Map<UUID, OrphanLayoutGroup> groups =
                new LinkedHashMap<>();

        boolean hadOrphanNeighbor =
                false;

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos =
                    placedPos.relative(
                            direction
                    );

            BlockEntity neighborBlockEntity =
                    level.getBlockEntity(
                            neighborPos
                    );

            if (
                    !(neighborBlockEntity instanceof FoundryTankBlockEntity neighborTank)
                            || neighborTank.getNetworkId() != null
            ) {
                continue;
            }

            hadOrphanNeighbor =
                    true;

            OrphanLayoutGroup group =
                    ensureOrphanLayoutGroup(
                            level,
                            neighborPos
                    );

            if (group == null) {
                /*
                 * An oversized legacy null-ID component is deliberately not
                 * migrated. The newly placed Tank will remain a separate
                 * layout instead of expanding it further.
                 */
                continue;
            }

            groups.putIfAbsent(
                    group.layoutId(),
                    group
            );
        }

        return new AdjacentOrphanLayouts(
                groups,
                hadOrphanNeighbor
        );
    }

    @Nullable
    private static OrphanLayoutGroup ensureOrphanLayoutGroup(
            Level level,
            BlockPos startPos
    ) {
        BlockEntity startBlockEntity =
                level.getBlockEntity(
                        startPos
                );

        if (
                !(startBlockEntity instanceof FoundryTankBlockEntity startTank)
                        || startTank.getNetworkId() != null
        ) {
            return null;
        }

        UUID existingLayoutId =
                startTank.getOrphanLayoutId();

        if (existingLayoutId != null) {
            return new OrphanLayoutGroup(
                    existingLayoutId,
                    collectMatchingOrphanLayout(
                            level,
                            startPos,
                            existingLayoutId
                    )
            );
        }

        /*
         * Migrate one old null-ID orphan component the first time a new Tank
         * is placed beside it.
         */
        Set<BlockPos> legacyComponent =
                collectLegacyNullLayout(
                        level,
                        startPos
                );

        if (
                legacyComponent.isEmpty()
                        || legacyComponent.size()
                        > FoundryTankNetwork.MAX_TANK_COUNT
        ) {
            return null;
        }

        UUID migratedLayoutId =
                UUID.randomUUID();

        applyOrphanLayoutId(
                level,
                legacyComponent,
                migratedLayoutId
        );

        return new OrphanLayoutGroup(
                migratedLayoutId,
                legacyComponent
        );
    }

    private static Set<BlockPos> collectMatchingOrphanLayout(
            Level level,
            BlockPos startPos,
            UUID requiredLayoutId
    ) {
        Set<BlockPos> connected =
                new HashSet<>();

        ArrayDeque<BlockPos> queue =
                new ArrayDeque<>();

        BlockPos immutableStart =
                startPos.immutable();

        connected.add(
                immutableStart
        );

        queue.addLast(
                immutableStart
        );

        while (!queue.isEmpty()) {
            BlockPos current =
                    queue.removeFirst();

            for (Direction direction : Direction.values()) {
                BlockPos next =
                        current.relative(
                                direction
                        );

                if (connected.contains(next)) {
                    continue;
                }

                BlockEntity blockEntity =
                        level.getBlockEntity(
                                next
                        );

                if (
                        !(blockEntity instanceof FoundryTankBlockEntity tank)
                                || tank.getNetworkId() != null
                                || !requiredLayoutId.equals(
                                tank.getOrphanLayoutId()
                        )
                ) {
                    continue;
                }

                BlockPos immutableNext =
                        next.immutable();

                connected.add(
                        immutableNext
                );

                queue.addLast(
                        immutableNext
                );
            }
        }

        return connected;
    }

    private static Set<BlockPos> collectLegacyNullLayout(
            Level level,
            BlockPos startPos
    ) {
        Set<BlockPos> connected =
                new HashSet<>();

        ArrayDeque<BlockPos> queue =
                new ArrayDeque<>();

        BlockPos immutableStart =
                startPos.immutable();

        connected.add(
                immutableStart
        );

        queue.addLast(
                immutableStart
        );

        while (!queue.isEmpty()) {
            BlockPos current =
                    queue.removeFirst();

            for (Direction direction : Direction.values()) {
                BlockPos next =
                        current.relative(
                                direction
                        );

                if (connected.contains(next)) {
                    continue;
                }

                BlockEntity blockEntity =
                        level.getBlockEntity(
                                next
                        );

                if (
                        !(blockEntity instanceof FoundryTankBlockEntity tank)
                                || tank.getNetworkId() != null
                                || tank.getOrphanLayoutId() != null
                ) {
                    continue;
                }

                BlockPos immutableNext =
                        next.immutable();

                connected.add(
                        immutableNext
                );

                if (
                        connected.size()
                                > FoundryTankNetwork.MAX_TANK_COUNT
                ) {
                    return connected;
                }

                queue.addLast(
                        immutableNext
                );
            }
        }

        return connected;
    }

    @Nullable
    private static UUID getPreferredOrphanLayoutId(
            Level level,
            BlockPos clickedAgainstPos
    ) {
        BlockEntity clickedBlockEntity =
                level.getBlockEntity(
                        clickedAgainstPos
                );

        if (
                clickedBlockEntity instanceof FoundryTankBlockEntity clickedTank
                        && clickedTank.getNetworkId() == null
        ) {
            return clickedTank.getOrphanLayoutId();
        }

        return null;
    }

    private static Set<BlockPos> withPlacedTank(
            Set<BlockPos> positions,
            BlockPos placedPos
    ) {
        Set<BlockPos> combined =
                new HashSet<>(
                        positions
                );

        combined.add(
                placedPos.immutable()
        );

        return combined;
    }

    private static void applyOrphanLayoutId(
            Level level,
            Set<BlockPos> positions,
            UUID layoutId
    ) {
        for (BlockPos tankPos : positions) {
            BlockEntity blockEntity =
                    level.getBlockEntity(
                            tankPos
                    );

            if (
                    blockEntity instanceof FoundryTankBlockEntity tank
                            && tank.getNetworkId() == null
            ) {
                tank.setOrphanLayoutId(
                        layoutId
                );
            }
        }
    }

    private static Set<BlockPos> findUpwardColumnForLayout(
            Level level,
            BlockPos startPos,
            FoundryTankBlockEntity startTank
    ) {
        Set<BlockPos> layoutPositions;

        if (startTank.getNetworkId() != null) {
            FoundryTankNetwork network =
                    startTank.getNetwork();

            layoutPositions =
                    network != null
                            ? network.getTankPositions()
                            : Set.of(
                            startPos.immutable()
                    );
        } else {
            UUID requiredLayoutId =
                    startTank.getOrphanLayoutId();

            layoutPositions =
                    requiredLayoutId != null
                            ? collectMatchingOrphanLayout(
                            level,
                            startPos,
                            requiredLayoutId
                    )
                            : Set.of(
                            startPos.immutable()
                    );
        }

        return chooseTankRemovalForLayout(
                layoutPositions,
                startPos
        );
    }

    private static Set<BlockPos> chooseTankRemovalForLayout(
            Set<BlockPos> layoutPositions,
            BlockPos startPos
    ) {
        if (layoutPositions == null || layoutPositions.isEmpty()) {
            return Set.of(
                    startPos.immutable()
            );
        }

        Set<BlockPos> selectedOnly =
                Set.of(
                        startPos.immutable()
                );

        if (isValidAfterRemoving(layoutPositions, selectedOnly)) {
            return selectedOnly;
        }

        Set<BlockPos> upwardColumn =
                collectUpwardColumnFromLayout(
                        layoutPositions,
                        startPos
                );

        if (upwardColumn.isEmpty()) {
            return selectedOnly;
        }

        /*
         * If removing only the clicked Tank would leave an invalid tank shape,
         * fall back to the old column behavior. This keeps unsupported/problem
         * removals safe while allowing harmless single-block edits.
         */
        if (isValidAfterRemoving(layoutPositions, upwardColumn)) {
            return upwardColumn;
        }

        return upwardColumn;
    }

    private static boolean isValidAfterRemoving(
            Set<BlockPos> layoutPositions,
            Set<BlockPos> removedPositions
    ) {
        Set<BlockPos> remaining =
                new HashSet<>(
                        layoutPositions
                );

        remaining.removeAll(
                removedPositions
        );

        return remaining.isEmpty()
                || FoundryTankNetwork.isValidStructure(
                remaining
        );
    }

    private static Set<BlockPos> collectUpwardColumnFromLayout(
            Set<BlockPos> layoutPositions,
            BlockPos startPos
    ) {
        Set<BlockPos> removed =
                new HashSet<>();

        for (BlockPos tankPos : layoutPositions) {
            if (
                    tankPos.getX() == startPos.getX()
                            && tankPos.getZ() == startPos.getZ()
                            && tankPos.getY() >= startPos.getY()
                            && tankPos.getY()
                            < startPos.getY()
                            + FoundryTankNetwork.MAX_HEIGHT
            ) {
                removed.add(
                        tankPos.immutable()
                );
            }
        }

        return removed;
    }

    private record OrphanLayoutGroup(
            UUID layoutId,
            Set<BlockPos> positions
    ) {
    }

    private record AdjacentOrphanLayouts(
            Map<UUID, OrphanLayoutGroup> groups,
            boolean hadOrphanNeighbor
    ) {
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
                    blockEntity instanceof FoundryTankBlockEntity tank
                            ? findUpwardColumnForLayout(
                            level,
                            pos,
                            tank
                    )
                            : Set.of();

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
                     * returns. We only destroy the extra Tanks here.
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
            BlockPos clickedAgainstPos,
            boolean forceSeparateLayout
    ) {
    }
}
