package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.block.custom.FoundryControllerBlock;
import net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/**
 * One physically connected Foundry Tank section.
 *
 * Ownership rules:
 *
 * - ownerId == null:
 *   unassigned/orphan Tank section
 *
 * - ownerId != null:
 *   Tank section owned by the Controller with the same persistent UUID
 *
 * Two touching Tank sections with different owner IDs remain completely
 * separate. Unassigned sections can be absorbed automatically by a valid
 * Controller-owned network.
 */
public final class FoundryTankNetwork {

    public static final int MAX_SHORT_SIDE = 3;
    public static final int MAX_LONG_SIDE = 4;
    public static final int MAX_HEIGHT = 3;
    public static final int MAX_COLUMN_COUNT =
            MAX_SHORT_SIDE * MAX_LONG_SIDE;
    public static final int MAX_TANK_COUNT =
            MAX_COLUMN_COUNT * MAX_HEIGHT;

    private final Level level;

    @Nullable
    private final UUID ownerId;

    private final Set<BlockPos> tankPositions;
    private final int minY;

    private FoundryTankNetwork(
            Level level,
            @Nullable UUID ownerId,
            Set<BlockPos> tankPositions,
            int minY
    ) {
        this.level = level;
        this.ownerId = ownerId;
        this.tankPositions = Set.copyOf(tankPositions);
        this.minY = minY;
    }

    // =========================
    // DISCOVERY
    // =========================

    /**
     * Finds the complete face-connected Tank component containing startPos.
     *
     * Tanks are connected only when their owner IDs are equal. This includes
     * null owner IDs, allowing orphan sections to retain shared visuals.
     */
    @Nullable
    public static FoundryTankNetwork find(
            Level level,
            BlockPos startPos
    ) {
        BlockEntity startBlockEntity =
                level.getBlockEntity(startPos);

        if (!(startBlockEntity instanceof FoundryTankBlockEntity startTank)) {
            return null;
        }

        UUID ownerId =
                startTank.getNetworkId();

        Set<BlockPos> connected =
                collectConnectedTanks(
                        level,
                        startPos,
                        ownerId,
                        Set.of()
                );

        ValidationResult validation =
                validateStructure(connected);

        if (!validation.valid()) {
            return null;
        }

        return new FoundryTankNetwork(
                level,
                ownerId,
                connected,
                validation.minY()
        );
    }

    private static Set<BlockPos> collectConnectedTanks(
            Level level,
            BlockPos startPos,
            @Nullable UUID requiredOwnerId,
            Set<BlockPos> excludedPositions
    ) {
        Set<BlockPos> connected =
                new HashSet<>();

        if (excludedPositions.contains(startPos)) {
            return connected;
        }

        BlockEntity startBlockEntity =
                level.getBlockEntity(startPos);

        if (!(startBlockEntity instanceof FoundryTankBlockEntity startTank)) {
            return connected;
        }

        if (!Objects.equals(
                requiredOwnerId,
                startTank.getNetworkId()
        )) {
            return connected;
        }

        ArrayDeque<BlockPos> queue =
                new ArrayDeque<>();

        BlockPos immutableStart =
                startPos.immutable();

        connected.add(immutableStart);
        queue.addLast(immutableStart);

        while (!queue.isEmpty()) {
            BlockPos current =
                    queue.removeFirst();

            for (Direction direction : Direction.values()) {
                BlockPos next =
                        current.relative(direction);

                if (
                        excludedPositions.contains(next)
                                || connected.contains(next)
                ) {
                    continue;
                }

                BlockEntity blockEntity =
                        level.getBlockEntity(next);

                if (!(blockEntity instanceof FoundryTankBlockEntity tank)) {
                    continue;
                }

                if (!Objects.equals(
                        requiredOwnerId,
                        tank.getNetworkId()
                )) {
                    continue;
                }

                BlockPos immutableNext =
                        next.immutable();

                connected.add(immutableNext);
                queue.addLast(immutableNext);
            }
        }

        return connected;
    }

    // =========================
    // AUTOMATIC CONTROLLER CLAIMING
    // =========================

    /**
     * Claims the largest valid unassigned layout touching the Controller on
     * its back, left, or right side.
     */
    @Nullable
    public static FoundryTankNetwork claimLargestUnassignedLayoutForController(
            Level level,
            FoundryControllerBlockEntity controller
    ) {
        CandidateLayout best =
                CandidateLayout.empty();

        for (BlockPos attachmentPos : controller.getValidTankAttachmentPositions()) {
            BlockEntity blockEntity =
                    level.getBlockEntity(
                            attachmentPos
                    );

            if (!(blockEntity instanceof FoundryTankBlockEntity tank)) {
                continue;
            }

            FoundryTankNetwork existing =
                    tank.getNetwork();

            /*
             * An old UUID with no physically attached Controller is stale and
             * behaves exactly like an orphan section.
             */
            if (
                    existing != null
                            && existing.getOwnerId() != null
                            && existing.getAttachedController() == null
            ) {
                existing.releaseOwnership();
            }

            blockEntity =
                    level.getBlockEntity(
                            attachmentPos
                    );

            if (
                    !(blockEntity instanceof FoundryTankBlockEntity refreshedTank)
                            || refreshedTank.getNetworkId() != null
            ) {
                continue;
            }

            CandidateLayout candidate =
                    findLargestValidUnassignedLayout(
                            level,
                            attachmentPos,
                            controller
                    );

            if (candidate.isBetterThan(best)) {
                best =
                        candidate;
            }
        }

        if (best.positions().isEmpty()) {
            return null;
        }

        return claimExactLayout(
                level,
                best.positions(),
                controller.getControllerId()
        );
    }

    /**
     * Retained for compatibility with older callers.
     */
    @Nullable
    public static FoundryTankNetwork claimLargestUnassignedLayout(
            Level level,
            BlockPos startPos,
            UUID controllerId
    ) {
        CandidateLayout selected =
                findLargestValidUnassignedLayout(
                        level,
                        startPos,
                        null
                );

        if (selected.positions().isEmpty()) {
            return null;
        }

        return claimExactLayout(
                level,
                selected.positions(),
                controllerId
        );
    }

    /**
     * Claims one already-selected valid orphan layout.
     */
    @Nullable
    public static FoundryTankNetwork claimExactLayout(
            Level level,
            Set<BlockPos> selected,
            UUID controllerId
    ) {
        if (
                selected.isEmpty()
                        || !validateStructure(
                        selected
                ).valid()
        ) {
            return null;
        }

        StorageSnapshot storage =
                readStorage(
                        level,
                        selected
                );

        if (
                storage.mixedMetals()
                        || storage.amount()
                        > selected.size()
                        * FoundryTankBlockEntity.CAPACITY
        ) {
            return null;
        }

        for (BlockPos tankPos : selected) {
            BlockEntity blockEntity =
                    level.getBlockEntity(tankPos);

            if (
                    !(blockEntity instanceof FoundryTankBlockEntity tank)
                            || tank.getNetworkId() != null
            ) {
                return null;
            }
        }

        setOwnerId(
                level,
                selected,
                controllerId
        );

        invalidateTankCaches(
                level,
                selected
        );

        FoundryTankNetwork claimed =
                find(
                        level,
                        selected.iterator()
                                .next()
                );

        if (claimed == null) {
            setOwnerId(
                    level,
                    selected,
                    null
            );

            invalidateTankCaches(
                    level,
                    selected
            );

            return null;
        }

        claimed.writeDistributedStorage(
                selected,
                storage.metal(),
                storage.amount()
        );

        pulse(
                level,
                selected
        );

        return claimed;
    }

    /**
     * Finds the largest valid unassigned layout containing startPos.
     *
     * When controller is supplied, layouts containing a Tank in front of
     * that Controller are rejected.
     */
    private static CandidateLayout findLargestValidUnassignedLayout(
            Level level,
            BlockPos startPos,
            @Nullable FoundryControllerBlockEntity controller
    ) {
        BlockEntity startBlockEntity =
                level.getBlockEntity(startPos);

        if (
                !(startBlockEntity instanceof FoundryTankBlockEntity startTank)
                        || startTank.getNetworkId() != null
        ) {
            return CandidateLayout.empty();
        }

        int baseY =
                findUnassignedColumnBase(
                        level,
                        startPos
                );

        if (baseY == Integer.MIN_VALUE) {
            return CandidateLayout.empty();
        }

        ColumnKey startColumn =
                new ColumnKey(
                        startPos.getX(),
                        startPos.getZ()
                );

        CandidateLayout best =
                CandidateLayout.empty();

        for (int sizeX = 1; sizeX <= MAX_LONG_SIDE; sizeX++) {
            for (int sizeZ = 1; sizeZ <= MAX_LONG_SIDE; sizeZ++) {
                if (!isHorizontalSizeValid(
                        sizeX,
                        sizeZ
                )) {
                    continue;
                }

                for (
                        int minX =
                        startPos.getX() - sizeX + 1;
                        minX <= startPos.getX();
                        minX++
                ) {
                    int maxX =
                            minX + sizeX - 1;

                    for (
                            int minZ =
                            startPos.getZ() - sizeZ + 1;
                            minZ <= startPos.getZ();
                            minZ++
                    ) {
                        int maxZ =
                                minZ + sizeZ - 1;

                        Map<ColumnKey, Set<BlockPos>> columns =
                                new HashMap<>();

                        for (int x = minX; x <= maxX; x++) {
                            for (int z = minZ; z <= maxZ; z++) {
                                Set<BlockPos> column =
                                        readValidUnassignedColumn(
                                                level,
                                                x,
                                                z,
                                                baseY
                                        );

                                if (!column.isEmpty()) {
                                    columns.put(
                                            new ColumnKey(
                                                    x,
                                                    z
                                            ),
                                            column
                                    );
                                }
                            }
                        }

                        if (!columns.containsKey(startColumn)) {
                            continue;
                        }

                        Set<ColumnKey> connectedColumns =
                                collectConnectedColumns(
                                        columns.keySet(),
                                        startColumn
                                );

                        Set<BlockPos> positions =
                                new HashSet<>();

                        for (ColumnKey columnKey : connectedColumns) {
                            positions.addAll(
                                    columns.get(columnKey)
                            );
                        }

                        ValidationResult validation =
                                validateStructure(positions);

                        if (
                                !validation.valid()
                                        || controller != null
                                        && !controller.canOwnTankLayout(
                                        positions
                                )
                        ) {
                            continue;
                        }

                        StorageSnapshot candidateStorage =
                                readStorage(
                                        level,
                                        positions
                                );

                        if (
                                candidateStorage.mixedMetals()
                                        || candidateStorage.amount()
                                        > positions.size()
                                        * FoundryTankBlockEntity.CAPACITY
                        ) {
                            continue;
                        }

                        CandidateLayout candidate =
                                new CandidateLayout(
                                        positions,
                                        connectedColumns.size(),
                                        minX,
                                        minZ
                                );

                        if (candidate.isBetterThan(best)) {
                            best =
                                    candidate;
                        }
                    }
                }
            }
        }

        return best;
    }

    private static int findUnassignedColumnBase(
            Level level,
            BlockPos startPos
    ) {
        BlockPos current =
                startPos;

        for (int offset = 0; offset < MAX_HEIGHT; offset++) {
            BlockPos below =
                    current.below();

            BlockEntity blockEntity =
                    level.getBlockEntity(below);

            if (
                    blockEntity instanceof FoundryTankBlockEntity tank
                            && tank.getNetworkId() == null
            ) {
                current = below;
                continue;
            }

            return current.getY();
        }

        /*
         * A fourth directly connected Tank below would make this
         * vertical column too tall.
         */
        BlockEntity blockEntity =
                level.getBlockEntity(
                        current.below()
                );

        if (
                blockEntity instanceof FoundryTankBlockEntity tank
                        && tank.getNetworkId() == null
        ) {
            return Integer.MIN_VALUE;
        }

        return current.getY();
    }

    private static Set<BlockPos> readValidUnassignedColumn(
            Level level,
            int x,
            int z,
            int baseY
    ) {
        BlockPos belowBase =
                new BlockPos(
                        x,
                        baseY - 1,
                        z
                );

        BlockEntity belowBaseBlockEntity =
                level.getBlockEntity(belowBase);

        if (
                belowBaseBlockEntity instanceof FoundryTankBlockEntity tank
                        && tank.getNetworkId() == null
        ) {
            return Set.of();
        }

        Set<BlockPos> column =
                new LinkedHashSet<>();

        boolean foundGap =
                false;

        for (int height = 0; height < MAX_HEIGHT; height++) {
            BlockPos checkPos =
                    new BlockPos(
                            x,
                            baseY + height,
                            z
                    );

            BlockEntity blockEntity =
                    level.getBlockEntity(checkPos);

            boolean isUnassignedTank =
                    blockEntity instanceof FoundryTankBlockEntity tank
                            && tank.getNetworkId() == null;

            if (isUnassignedTank) {
                if (foundGap) {
                    return Set.of();
                }

                column.add(
                        checkPos.immutable()
                );
            } else {
                foundGap = true;
            }
        }

        if (column.isEmpty()) {
            return Set.of();
        }

        BlockPos aboveMaximum =
                new BlockPos(
                        x,
                        baseY + MAX_HEIGHT,
                        z
                );

        BlockEntity aboveMaximumBlockEntity =
                level.getBlockEntity(aboveMaximum);

        if (
                aboveMaximumBlockEntity instanceof FoundryTankBlockEntity tank
                        && tank.getNetworkId() == null
        ) {
            return Set.of();
        }

        return column;
    }

    // =========================
    // AUTOMATIC TANK PLACEMENT
    // =========================

    /**
     * Handles ownership after a Tank is placed.
     *
     * The placed Tank scans every neighboring block, so clicking the ground
     * or another decorative block does not prevent a logical connection.
     *
     * Priority:
     *
     * 1. the deliberately clicked Controller or owned Tank
     * 2. one unique valid neighboring Controller network
     * 3. otherwise remain unassigned rather than guessing between foundries
     */
    public static boolean handleTankPlaced(
            Level level,
            BlockPos placedPos,
            BlockPos clickedAgainstPos
    ) {
        Map<UUID, FoundryControllerBlockEntity> candidateControllers =
                new HashMap<>();

        normalizeAdjacentStaleNetworks(
                level,
                placedPos
        );

        FoundryControllerBlockEntity preferredController =
                findControllerFromClickedBlock(
                        level,
                        placedPos,
                        clickedAgainstPos
                );

        if (preferredController != null) {
            candidateControllers.put(
                    preferredController.getControllerId(),
                    preferredController
            );
        }

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos =
                    placedPos.relative(direction);

            BlockEntity blockEntity =
                    level.getBlockEntity(neighborPos);

            if (
                    blockEntity
                            instanceof FoundryControllerBlockEntity controller
                            && controller.isValidTankAttachmentPosition(
                            placedPos
                    )
            ) {
                candidateControllers.putIfAbsent(
                        controller.getControllerId(),
                        controller
                );

                continue;
            }

            if (
                    blockEntity
                            instanceof FoundryTankBlockEntity tank
            ) {
                FoundryTankNetwork network =
                        tank.getNetwork();

                FoundryControllerBlockEntity controller =
                        network != null
                                ? network.getAttachedController()
                                : null;

                if (controller != null) {
                    candidateControllers.putIfAbsent(
                            controller.getControllerId(),
                            controller
                    );
                }
            }
        }

        if (candidateControllers.isEmpty()) {
            return false;
        }

        List<Set<BlockPos>> adjacentOrphanComponents =
                collectAdjacentOrphanComponents(
                        level,
                        placedPos
                );

        Map<UUID, PlacementOption> validOptions =
                new HashMap<>();

        for (FoundryControllerBlockEntity controller : candidateControllers.values()) {
            FoundryTankNetwork existingNetwork =
                    controller.getOwnedTankNetwork();

            Set<BlockPos> mandatoryPositions =
                    new HashSet<>();

            if (existingNetwork != null) {
                mandatoryPositions.addAll(
                        existingNetwork.tankPositions
                );
            }

            mandatoryPositions.add(
                    placedPos.immutable()
            );

            ValidationResult validation =
                    validateStructure(
                            mandatoryPositions
                    );

            if (
                    !validation.valid()
                            || !controller.canOwnTankLayout(
                            mandatoryPositions
                    )
            ) {
                continue;
            }

            MergeCandidate mergeCandidate =
                    findBestMergeCandidate(
                            level,
                            mandatoryPositions,
                            adjacentOrphanComponents,
                            controller
                    );

            if (mergeCandidate == null) {
                continue;
            }

            validOptions.put(
                    controller.getControllerId(),
                    new PlacementOption(
                            controller,
                            mergeCandidate
                    )
            );
        }

        PlacementOption selectedOption =
                null;

        if (preferredController != null) {
            selectedOption =
                    validOptions.get(
                            preferredController.getControllerId()
                    );
        }

        if (
                selectedOption == null
                        && validOptions.size() == 1
        ) {
            selectedOption =
                    validOptions.values()
                            .iterator()
                            .next();
        }

        if (selectedOption == null) {
            return false;
        }

        FoundryControllerBlockEntity targetController =
                selectedOption.controller();

        MergeCandidate best =
                selectedOption.mergeCandidate();

        setOwnerId(
                level,
                best.positions(),
                targetController.getControllerId()
        );

        invalidateTankCaches(
                level,
                best.positions()
        );

        FoundryTankNetwork merged =
                find(
                        level,
                        placedPos
                );

        if (merged == null) {
            for (BlockPos position : best.newlyClaimedPositions()) {
                BlockEntity blockEntity =
                        level.getBlockEntity(position);

                if (blockEntity instanceof FoundryTankBlockEntity tank) {
                    tank.setNetworkId(null);
                }
            }

            return false;
        }

        merged.writeDistributedStorage(
                best.positions(),
                best.metal(),
                best.amount()
        );

        pulse(
                level,
                best.positions()
        );

        return true;
    }

    private static void normalizeAdjacentStaleNetworks(
            Level level,
            BlockPos placedPos
    ) {
        Set<BlockPos> checkedNetworks =
                new HashSet<>();

        for (Direction direction : Direction.values()) {
            BlockEntity blockEntity =
                    level.getBlockEntity(
                            placedPos.relative(direction)
                    );

            if (
                    !(blockEntity
                            instanceof FoundryTankBlockEntity tank)
            ) {
                continue;
            }

            FoundryTankNetwork network =
                    tank.getNetwork();

            if (
                    network == null
                            || network.getOwnerId() == null
            ) {
                continue;
            }

            BlockPos key =
                    network.getTankPositions()
                            .stream()
                            .min(
                                    Comparator
                                            .comparingInt(
                                                    (BlockPos pos) ->
                                                            pos.getY()
                                            )
                                            .thenComparingInt(
                                                    pos ->
                                                            pos.getX()
                                            )
                                            .thenComparingInt(
                                                    pos ->
                                                            pos.getZ()
                                            )
                            )
                            .orElse(
                                    tank.getBlockPos()
                            );

            if (!checkedNetworks.add(key)) {
                continue;
            }

            if (network.getAttachedController() == null) {
                network.releaseOwnership();
            }
        }
    }

    @Nullable
    private static FoundryControllerBlockEntity findControllerFromClickedBlock(
            Level level,
            BlockPos placedPos,
            BlockPos clickedAgainstPos
    ) {
        BlockEntity clickedBlockEntity =
                level.getBlockEntity(
                        clickedAgainstPos
                );

        if (
                clickedBlockEntity
                        instanceof FoundryControllerBlockEntity controller
                        && controller.isValidTankAttachmentPosition(
                        placedPos
                )
        ) {
            return controller;
        }

        if (
                clickedBlockEntity
                        instanceof FoundryTankBlockEntity clickedTank
        ) {
            FoundryTankNetwork clickedNetwork =
                    clickedTank.getNetwork();

            if (clickedNetwork != null) {
                return clickedNetwork.getAttachedController();
            }
        }

        return null;
    }

    public static boolean hasNearbyFoundryCandidate(
            Level level,
            BlockPos placedPos
    ) {
        for (Direction direction : Direction.values()) {
            BlockEntity blockEntity =
                    level.getBlockEntity(
                            placedPos.relative(direction)
                    );

            if (
                    blockEntity
                            instanceof FoundryControllerBlockEntity controller
                            && controller.isValidTankAttachmentPosition(
                            placedPos
                    )
            ) {
                return true;
            }

            if (
                    blockEntity
                            instanceof FoundryTankBlockEntity tank
            ) {
                FoundryTankNetwork network =
                        tank.getNetwork();

                if (
                        network != null
                                && network.getAttachedController() != null
                ) {
                    return true;
                }
            }
        }

        return false;
    }

    private static List<Set<BlockPos>> collectAdjacentOrphanComponents(
            Level level,
            BlockPos placedPos
    ) {
        List<Set<BlockPos>> components =
                new ArrayList<>();

        Set<BlockPos> alreadyCollected =
                new HashSet<>();

        Set<BlockPos> excluded =
                Set.of(
                        placedPos.immutable()
                );

        for (Direction direction : Direction.values()) {
            BlockPos neighbor =
                    placedPos.relative(direction);

            if (alreadyCollected.contains(neighbor)) {
                continue;
            }

            BlockEntity blockEntity =
                    level.getBlockEntity(neighbor);

            if (
                    !(blockEntity instanceof FoundryTankBlockEntity tank)
                            || tank.getNetworkId() != null
            ) {
                continue;
            }

            Set<BlockPos> component =
                    collectConnectedTanks(
                            level,
                            neighbor,
                            null,
                            excluded
                    );

            if (component.isEmpty()) {
                continue;
            }

            components.add(component);
            alreadyCollected.addAll(component);
        }

        return components;
    }

    @Nullable
    private static MergeCandidate findBestMergeCandidate(
            Level level,
            Set<BlockPos> mandatoryPositions,
            List<Set<BlockPos>> optionalOrphanComponents,
            FoundryControllerBlockEntity controller
    ) {
        int componentCount =
                optionalOrphanComponents.size();

        if (componentCount > 20) {
            componentCount = 20;
        }

        MergeCandidate best =
                null;

        int combinationCount =
                1 << componentCount;

        for (
                int mask = 0;
                mask < combinationCount;
                mask++
        ) {
            Set<BlockPos> combined =
                    new HashSet<>(
                            mandatoryPositions
                    );

            Set<BlockPos> newlyClaimed =
                    new HashSet<>();

            for (int index = 0; index < componentCount; index++) {
                if ((mask & (1 << index)) == 0) {
                    continue;
                }

                Set<BlockPos> component =
                        optionalOrphanComponents.get(index);

                combined.addAll(component);
                newlyClaimed.addAll(component);
            }

            ValidationResult validation =
                    validateStructure(combined);

            if (
                    !validation.valid()
                            || !controller.canOwnTankLayout(
                            combined
                    )
            ) {
                continue;
            }

            StorageSnapshot storage =
                    readStorage(
                            level,
                            combined
                    );

            if (
                    storage.mixedMetals()
                            || storage.amount()
                            > combined.size()
                            * FoundryTankBlockEntity.CAPACITY
            ) {
                continue;
            }

            Set<BlockPos> allNewlyClaimed =
                    new HashSet<>(
                            newlyClaimed
                    );

            for (BlockPos position : combined) {
                BlockEntity blockEntity =
                        level.getBlockEntity(position);

                if (
                        blockEntity instanceof FoundryTankBlockEntity tank
                                && tank.getNetworkId() == null
                ) {
                    allNewlyClaimed.add(position);
                }
            }

            MergeCandidate candidate =
                    new MergeCandidate(
                            combined,
                            allNewlyClaimed,
                            storage.metal(),
                            storage.amount()
                    );

            if (
                    best == null
                            || candidate.positions().size()
                            > best.positions().size()
            ) {
                best =
                        candidate;
            }
        }

        return best;
    }

    // =========================
    // OWNERSHIP / CONTROLLER STATE
    // =========================

    public boolean isActive() {
        return ownerId != null
                && getAttachedController() != null;
    }

    @Nullable
    public FoundryControllerBlockEntity getAttachedController() {
        if (ownerId == null) {
            return null;
        }

        return findAttachedController(
                level,
                tankPositions,
                ownerId
        );
    }

    @Nullable
    private static FoundryControllerBlockEntity findAttachedController(
            Level level,
            Set<BlockPos> positions,
            UUID controllerId
    ) {
        Set<BlockPos> checkedControllers =
                new HashSet<>();

        for (BlockPos tankPos : positions) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos controllerPos =
                        tankPos.relative(direction);

                if (!checkedControllers.add(controllerPos)) {
                    continue;
                }

                BlockEntity blockEntity =
                        level.getBlockEntity(controllerPos);

                if (
                        !(blockEntity
                                instanceof FoundryControllerBlockEntity controller)
                                || !controllerId.equals(
                                controller.getControllerId()
                        )
                ) {
                    continue;
                }

                if (controller.canOwnTankLayout(positions)) {
                    return controller;
                }
            }
        }

        return null;
    }

    /**
     * Turns this complete section into an orphan while preserving liquid.
     */
    public void releaseOwnership() {
        StorageSnapshot storage =
                readStorage(
                        level,
                        tankPositions
                );

        setOwnerId(
                level,
                tankPositions,
                null
        );

        invalidateTankCaches(
                level,
                tankPositions
        );

        writeDistributedStorage(
                tankPositions,
                storage.metal(),
                storage.amount()
        );

        pulse(
                level,
                tankPositions
        );
    }

    // =========================
    // STRUCTURE VALIDATION
    // =========================

    public static boolean isValidStructure(
            Set<BlockPos> positions
    ) {
        return validateStructure(
                positions
        ).valid();
    }

    private static ValidationResult validateStructure(
            Set<BlockPos> positions
    ) {
        if (
                positions.isEmpty()
                        || positions.size() > MAX_TANK_COUNT
        ) {
            return ValidationResult.invalid();
        }

        int minX =
                positions.stream()
                        .mapToInt(
                                tankPos ->
                                        tankPos.getX()
                        )
                        .min()
                        .orElse(0);

        int maxX =
                positions.stream()
                        .mapToInt(
                                tankPos ->
                                        tankPos.getX()
                        )
                        .max()
                        .orElse(0);

        int minY =
                positions.stream()
                        .mapToInt(
                                tankPos ->
                                        tankPos.getY()
                        )
                        .min()
                        .orElse(0);

        int minZ =
                positions.stream()
                        .mapToInt(
                                tankPos ->
                                        tankPos.getZ()
                        )
                        .min()
                        .orElse(0);

        int maxZ =
                positions.stream()
                        .mapToInt(
                                tankPos ->
                                        tankPos.getZ()
                        )
                        .max()
                        .orElse(0);

        int sizeX =
                maxX - minX + 1;

        int sizeZ =
                maxZ - minZ + 1;

        if (!isHorizontalSizeValid(
                sizeX,
                sizeZ
        )) {
            return ValidationResult.invalid();
        }

        Map<ColumnKey, Set<Integer>> columnHeights =
                new HashMap<>();

        for (BlockPos tankPos : positions) {
            ColumnKey key =
                    new ColumnKey(
                            tankPos.getX(),
                            tankPos.getZ()
                    );

            columnHeights
                    .computeIfAbsent(
                            key,
                            ignored -> new HashSet<>()
                    )
                    .add(tankPos.getY());
        }

        if (columnHeights.size() > MAX_COLUMN_COUNT) {
            return ValidationResult.invalid();
        }

        for (Set<Integer> yValues : columnHeights.values()) {
            int columnMaxY =
                    yValues.stream()
                            .mapToInt(
                                    Integer::intValue
                            )
                            .max()
                            .orElse(minY);

            int columnHeight =
                    columnMaxY
                            - minY
                            + 1;

            if (
                    columnHeight < 1
                            || columnHeight > MAX_HEIGHT
                            || yValues.size() != columnHeight
            ) {
                return ValidationResult.invalid();
            }

            for (int y = minY; y <= columnMaxY; y++) {
                if (!yValues.contains(y)) {
                    return ValidationResult.invalid();
                }
            }
        }

        Set<ColumnKey> connectedColumns =
                collectConnectedColumns(
                        columnHeights.keySet(),
                        columnHeights.keySet()
                                .iterator()
                                .next()
                );

        if (
                connectedColumns.size()
                        != columnHeights.size()
        ) {
            return ValidationResult.invalid();
        }

        return new ValidationResult(
                true,
                minY
        );
    }

    private static boolean isHorizontalSizeValid(
            int sizeX,
            int sizeZ
    ) {
        return (
                sizeX >= 1
                        && sizeZ >= 1
                        && sizeX <= MAX_SHORT_SIDE
                        && sizeZ <= MAX_LONG_SIDE
        )
                || (
                sizeX >= 1
                        && sizeZ >= 1
                        && sizeX <= MAX_LONG_SIDE
                        && sizeZ <= MAX_SHORT_SIDE
        );
    }

    private static Set<ColumnKey> collectConnectedColumns(
            Set<ColumnKey> availableColumns,
            ColumnKey startColumn
    ) {
        Set<ColumnKey> connected =
                new HashSet<>();

        if (!availableColumns.contains(startColumn)) {
            return connected;
        }

        ArrayDeque<ColumnKey> queue =
                new ArrayDeque<>();

        connected.add(startColumn);
        queue.addLast(startColumn);

        while (!queue.isEmpty()) {
            ColumnKey current =
                    queue.removeFirst();

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                ColumnKey next =
                        new ColumnKey(
                                current.x()
                                        + direction.getStepX(),
                                current.z()
                                        + direction.getStepZ()
                        );

                if (
                        availableColumns.contains(next)
                                && connected.add(next)
                ) {
                    queue.addLast(next);
                }
            }
        }

        return connected;
    }

    // =========================
    // STORAGE
    // =========================

    /**
     * Completes one-time migration from the former single-metal Tank fields
     * into the real density-sorted multi-metal layer storage.
     *
     * Step 8A deliberately keeps FoundryMultiMetalStorage as the internal
     * implementation. The important architectural change is that every
     * machine now talks only to FoundryTankNetwork.
     */
    public void ensureMoltenContentsMigrated() {
        FoundryMultiMetalStorage.ensureMigrated(
                level,
                this
        );
    }

    /**
     * Returns every molten metal currently stored by this complete Tank
     * network. The returned map is immutable.
     */
    public Map<ResourceLocation, Integer> getMoltenContents() {
        ensureMoltenContentsMigrated();

        return FoundryMultiMetalStorage.getContents(
                level,
                this
        );
    }

    /**
     * Returns the amount of one exact molten metal in the complete network.
     */
    public int getMoltenAmount(
            ResourceLocation metal
    ) {
        if (metal == null) {
            return 0;
        }

        ensureMoltenContentsMigrated();

        return FoundryMultiMetalStorage.getAmount(
                level,
                this,
                metal
        );
    }

    /**
     * Returns the combined amount of every molten metal in the network.
     */
    public int getTotalMoltenAmount() {
        ensureMoltenContentsMigrated();

        return FoundryMultiMetalStorage.getTotalAmount(
                level,
                this
        );
    }

    public boolean canAccept(
            ResourceLocation metal,
            int amount
    ) {
        return FoundryMultiMetalStorage.canAccept(
                level,
                this,
                metal,
                amount
        );
    }

    public int insert(
            ResourceLocation metal,
            int amount
    ) {
        return FoundryMultiMetalStorage.insert(
                level,
                this,
                metal,
                amount
        );
    }

    /**
     * Extracts only the requested metal. It can never silently fall through
     * to another stored liquid.
     */
    public int extract(
            ResourceLocation metal,
            int requestedAmount
    ) {
        return FoundryMultiMetalStorage.extract(
                level,
                this,
                metal,
                requestedAmount
        );
    }

    /**
     * Compatibility overload for older callers.
     *
     * Active Foundries use the Controller's selected output. An orphan with
     * exactly one stored metal can still expose that one unambiguous metal.
     */
    public int extract(
            int requestedAmount
    ) {
        ResourceLocation metal =
                getStoredMetal();

        return metal != null
                ? extract(
                metal,
                requestedAmount
        )
                : 0;
    }

    /**
     * Returns true when one metal physically occupies the requested horizontal
     * Tank level. Faucets use this to preserve their existing height rules.
     */
    public boolean hasMetalAtHeight(
            int tankY,
            ResourceLocation metal
    ) {
        ensureMoltenContentsMigrated();

        return FoundryMultiMetalStorage.hasMetalAtHeight(
                level,
                this,
                tankY,
                metal
        );
    }

    /**
     * Moves real multi-metal contents into the surviving Tanks before the old
     * structural splitting code processes an upward Tank-column removal.
     */
    public void prepareMoltenRemoval(
            Set<BlockPos> removedPositions
    ) {
        FoundryMultiMetalStorage.prepareRemoval(
                level,
                this,
                removedPositions
        );
    }

    /**
     * Compatibility name retained for existing callers and renderer helpers.
     * It now means the total of every stored metal.
     */
    public int getMoltenAmount() {
        return getTotalMoltenAmount();
    }

    /**
     * Compatibility view of one output metal.
     *
     * An active Foundry reports the Controller's selected output. Without a
     * Controller, only a network containing exactly one metal is unambiguous.
     */
    @Nullable
    public ResourceLocation getStoredMetal() {
        FoundryControllerBlockEntity controller =
                getAttachedController();

        if (controller != null) {
            ResourceLocation selected =
                    controller.getSelectedOutputMetalOrDefault(
                            this
                    );

            if (selected != null) {
                return selected;
            }
        }

        Map<ResourceLocation, Integer> contents =
                getMoltenContents();

        return contents.size() == 1
                ? contents.keySet()
                .iterator()
                .next()
                : null;
    }

    public boolean isEmpty() {
        return getTotalMoltenAmount() <= 0;
    }

    public boolean isFull() {
        return getTotalMoltenAmount()
                >= getCapacity();
    }


    private static StorageSnapshot readStorage(
            Level level,
            Set<BlockPos> positions
    ) {
        ResourceLocation metal =
                null;

        int amount =
                0;

        boolean mixedMetals =
                false;

        for (BlockPos tankPos : positions) {
            BlockEntity blockEntity =
                    level.getBlockEntity(tankPos);

            if (!(blockEntity instanceof FoundryTankBlockEntity tank)) {
                continue;
            }

            int localAmount =
                    tank.getLocalMoltenAmount();

            ResourceLocation localMetal =
                    tank.getLocalStoredMetal();

            if (localAmount <= 0) {
                continue;
            }

            if (localMetal == null) {
                mixedMetals = true;
                continue;
            }

            if (metal == null) {
                metal = localMetal;
            } else if (!metal.equals(localMetal)) {
                mixedMetals = true;
            }

            amount += localAmount;
        }

        if (amount <= 0) {
            metal = null;
        }

        return new StorageSnapshot(
                metal,
                amount,
                mixedMetals
        );
    }

    /**
     * Redistributes integer storage from the lowest horizontal layer upward.
     *
     * Tanks on one horizontal layer differ by at most one molten unit.
     */
    private void writeDistributedStorage(
            Set<BlockPos> positions,
            @Nullable ResourceLocation metal,
            int amount
    ) {
        writeDistributedStorage(
                level,
                positions,
                metal,
                amount
        );
    }

    private static void writeDistributedStorage(
            Level level,
            Set<BlockPos> positions,
            @Nullable ResourceLocation metal,
            int amount
    ) {
        int capacity =
                positions.size()
                        * FoundryTankBlockEntity.CAPACITY;

        int remaining =
                Mth.clamp(
                        amount,
                        0,
                        capacity
                );

        ResourceLocation normalizedMetal =
                remaining > 0
                        ? metal
                        : null;

        Map<Integer, List<BlockPos>> positionsByY =
                new TreeMap<>();

        for (BlockPos tankPos : positions) {
            positionsByY
                    .computeIfAbsent(
                            tankPos.getY(),
                            ignored -> new ArrayList<>()
                    )
                    .add(tankPos);
        }

        for (List<BlockPos> layer : positionsByY.values()) {
            layer.sort(
                    Comparator
                            .comparingInt(
                                    (BlockPos tankPos) ->
                                            tankPos.getX()
                            )
                            .thenComparingInt(
                                    tankPos ->
                                            tankPos.getZ()
                            )
            );

            int layerCapacity =
                    layer.size()
                            * FoundryTankBlockEntity.CAPACITY;

            int amountInLayer =
                    Math.min(
                            remaining,
                            layerCapacity
                    );

            int amountPerTank =
                    amountInLayer
                            / layer.size();

            int remainder =
                    amountInLayer
                            % layer.size();

            for (int index = 0; index < layer.size(); index++) {
                BlockPos tankPos =
                        layer.get(index);

                int localAmount =
                        amountPerTank
                                + (
                                index < remainder
                                        ? 1
                                        : 0
                        );

                BlockEntity blockEntity =
                        level.getBlockEntity(tankPos);

                if (blockEntity instanceof FoundryTankBlockEntity tank) {
                    tank.setLocalStorage(
                            localAmount > 0
                                    ? normalizedMetal
                                    : null,
                            localAmount
                    );
                }
            }

            remaining -=
                    amountInLayer;
        }
    }

    // =========================
    // RENDERING
    // =========================

    public float getLocalVisualMoltenAmount(
            BlockPos tankPos
    ) {
        if (!tankPositions.contains(tankPos)) {
            return 0.0f;
        }

        Map<Integer, Integer> tanksPerLayer =
                new TreeMap<>();

        for (BlockPos position : tankPositions) {
            tanksPerLayer.merge(
                    position.getY(),
                    1,
                    Integer::sum
            );
        }

        int remaining =
                getMoltenAmount();

        for (
                Map.Entry<Integer, Integer> layer :
                tanksPerLayer.entrySet()
        ) {
            int layerCapacity =
                    layer.getValue()
                            * FoundryTankBlockEntity.CAPACITY;

            int amountInLayer =
                    Math.min(
                            remaining,
                            layerCapacity
                    );

            if (layer.getKey() == tankPos.getY()) {
                return Mth.clamp(
                        (float) amountInLayer
                                / layer.getValue(),
                        0.0f,
                        FoundryTankBlockEntity.CAPACITY
                );
            }

            remaining -=
                    amountInLayer;
        }

        return 0.0f;
    }

    // =========================
    // BREAKING / SPLITTING
    // =========================

    /**
     * Returns the selected Tank plus every directly contiguous Tank above it
     * in the same vertical column and with the same owner ID.
     */
    public static Set<BlockPos> findUpwardColumn(
            Level level,
            BlockPos startPos
    ) {
        BlockEntity startBlockEntity =
                level.getBlockEntity(startPos);

        if (!(startBlockEntity instanceof FoundryTankBlockEntity startTank)) {
            return Set.of();
        }

        UUID requiredOwnerId =
                startTank.getNetworkId();

        Set<BlockPos> removed =
                new LinkedHashSet<>();

        removed.add(
                startPos.immutable()
        );

        for (int offset = 1; offset < MAX_HEIGHT; offset++) {
            BlockPos checkPos =
                    startPos.above(offset);

            BlockEntity blockEntity =
                    level.getBlockEntity(checkPos);

            if (
                    !(blockEntity instanceof FoundryTankBlockEntity tank)
                            || !Objects.equals(
                            requiredOwnerId,
                            tank.getNetworkId()
                    )
            ) {
                break;
            }

            removed.add(
                    checkPos.immutable()
            );
        }

        return removed;
    }

    /**
     * Prepares a network before the selected Tank and every Tank above it
     * are destroyed.
     *
     * Surviving components are recalculated immediately:
     *
     * - the component still attached to the matching Controller keeps ownership
     * - all cut-off components become orphan sections
     * - each component keeps the molten units physically redistributed into it
     * - only overflow beyond the remaining total capacity is destroyed
     */
    public void prepareUpwardRemoval(
            Set<BlockPos> removedPositions
    ) {
        FoundryControllerBlockEntity ownerController =
                getAttachedController();

        StorageSnapshot originalStorage =
                readStorage(
                        level,
                        tankPositions
                );

        Set<BlockPos> remainingPositions =
                new HashSet<>(
                        tankPositions
                );

        remainingPositions.removeAll(
                removedPositions
        );

        for (BlockPos removedPos : removedPositions) {
            BlockEntity blockEntity =
                    level.getBlockEntity(removedPos);

            if (blockEntity instanceof FoundryTankBlockEntity tank) {
                tank.setLocalStorage(
                        null,
                        0
                );
            }
        }

        if (
                remainingPositions.isEmpty()
                        || originalStorage.mixedMetals()
        ) {
            return;
        }

        int preservedAmount =
                Math.min(
                        originalStorage.amount(),
                        remainingPositions.size()
                                * FoundryTankBlockEntity.CAPACITY
                );

        writeDistributedStorage(
                level,
                remainingPositions,
                preservedAmount > 0
                        ? originalStorage.metal()
                        : null,
                preservedAmount
        );

        List<Set<BlockPos>> components =
                splitIntoComponents(
                        remainingPositions
                );

        Set<BlockPos> controllerComponent =
                chooseControllerComponent(
                        components,
                        ownerController
                );

        for (Set<BlockPos> component : components) {
            StorageSnapshot componentStorage =
                    readStorage(
                            level,
                            component
                    );

            UUID resultingOwnerId =
                    ownerId != null
                            && component == controllerComponent
                            ? ownerId
                            : null;

            setOwnerId(
                    level,
                    component,
                    resultingOwnerId
            );

            invalidateTankCaches(
                    level,
                    component
            );

            writeDistributedStorage(
                    level,
                    component,
                    componentStorage.metal(),
                    componentStorage.amount()
            );

            pulse(
                    level,
                    component
            );
        }
    }

    @Nullable
    private static Set<BlockPos> chooseControllerComponent(
            List<Set<BlockPos>> components,
            @Nullable FoundryControllerBlockEntity controller
    ) {
        if (controller == null) {
            return null;
        }

        Set<BlockPos> best =
                null;

        long bestDistance =
                Long.MAX_VALUE;

        for (Set<BlockPos> component : components) {
            if (!controller.canOwnTankLayout(component)) {
                continue;
            }

            long closestDistance =
                    Long.MAX_VALUE;

            for (BlockPos tankPos : component) {
                long deltaX =
                        tankPos.getX()
                                - controller.getBlockPos().getX();

                long deltaY =
                        tankPos.getY()
                                - controller.getBlockPos().getY();

                long deltaZ =
                        tankPos.getZ()
                                - controller.getBlockPos().getZ();

                long distance =
                        deltaX * deltaX
                                + deltaY * deltaY
                                + deltaZ * deltaZ;

                closestDistance =
                        Math.min(
                                closestDistance,
                                distance
                        );
            }

            if (
                    best == null
                            || component.size() > best.size()
                            || component.size() == best.size()
                            && closestDistance < bestDistance
            ) {
                best =
                        component;

                bestDistance =
                        closestDistance;
            }
        }

        return best;
    }

    private static List<Set<BlockPos>> splitIntoComponents(
            Set<BlockPos> positions
    ) {
        List<Set<BlockPos>> components =
                new ArrayList<>();

        Set<BlockPos> remaining =
                new HashSet<>(positions);

        while (!remaining.isEmpty()) {
            BlockPos start =
                    remaining.iterator()
                            .next();

            Set<BlockPos> component =
                    new HashSet<>();

            ArrayDeque<BlockPos> queue =
                    new ArrayDeque<>();

            component.add(start);
            queue.addLast(start);
            remaining.remove(start);

            while (!queue.isEmpty()) {
                BlockPos current =
                        queue.removeFirst();

                for (Direction direction : Direction.values()) {
                    BlockPos next =
                            current.relative(direction);

                    if (!remaining.remove(next)) {
                        continue;
                    }

                    BlockPos immutableNext =
                            next.immutable();

                    component.add(immutableNext);
                    queue.addLast(immutableNext);
                }
            }

            components.add(component);
        }

        return components;
    }

    public static List<BlockPos> findAttachedFaucets(
            Level level,
            Set<BlockPos> tankSubset
    ) {
        Set<BlockPos> faucets =
                new HashSet<>();

        for (BlockPos tankPos : tankSubset) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos faucetPos =
                        tankPos.relative(direction);

                BlockState faucetState =
                        level.getBlockState(faucetPos);

                if (
                        !faucetState.hasProperty(
                                FoundryFaucetBlock.FACING
                        )
                ) {
                    continue;
                }

                if (
                        faucetState.getValue(
                                FoundryFaucetBlock.FACING
                        ) != direction
                ) {
                    continue;
                }

                if (
                        level.getBlockEntity(faucetPos)
                                instanceof FoundryFaucetBlockEntity
                ) {
                    faucets.add(
                            faucetPos.immutable()
                    );
                }
            }
        }

        return new ArrayList<>(faucets);
    }

    // =========================
    // INTERNAL HELPERS
    // =========================

    private static void setOwnerId(
            Level level,
            Set<BlockPos> positions,
            @Nullable UUID ownerId
    ) {
        for (BlockPos tankPos : positions) {
            BlockEntity blockEntity =
                    level.getBlockEntity(tankPos);

            if (blockEntity instanceof FoundryTankBlockEntity tank) {
                tank.setNetworkId(ownerId);
            }
        }
    }

    private static void invalidateTankCaches(
            Level level,
            Set<BlockPos> positions
    ) {
        for (BlockPos tankPos : positions) {
            BlockEntity blockEntity =
                    level.getBlockEntity(tankPos);

            if (blockEntity instanceof FoundryTankBlockEntity tank) {
                tank.invalidateNetworkCache();
            }
        }
    }

    private static void pulse(
            Level level,
            Set<BlockPos> positions
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        for (BlockPos tankPos : positions) {
            serverLevel.sendParticles(
                    ParticleTypes.WAX_ON,
                    tankPos.getX() + 0.5,
                    tankPos.getY() + 0.65,
                    tankPos.getZ() + 0.5,
                    1,
                    0.18,
                    0.18,
                    0.18,
                    0.0
            );
        }
    }

    // =========================
    // GETTERS
    // =========================

    public int getCapacity() {
        return tankPositions.size()
                * FoundryTankBlockEntity.CAPACITY;
    }

    public int getTankCount() {
        return tankPositions.size();
    }

    @Nullable
    public UUID getOwnerId() {
        return ownerId;
    }

    public Set<BlockPos> getTankPositions() {
        return tankPositions;
    }

    public int getMinY() {
        return minY;
    }

    private record ColumnKey(
            int x,
            int z
    ) {
    }

    private record StorageSnapshot(
            @Nullable ResourceLocation metal,
            int amount,
            boolean mixedMetals
    ) {
    }

    private record ValidationResult(
            boolean valid,
            int minY
    ) {
        private static ValidationResult invalid() {
            return new ValidationResult(
                    false,
                    0
            );
        }
    }

    private record CandidateLayout(
            Set<BlockPos> positions,
            int columnCount,
            int minX,
            int minZ
    ) {
        private static CandidateLayout empty() {
            return new CandidateLayout(
                    Set.of(),
                    0,
                    Integer.MAX_VALUE,
                    Integer.MAX_VALUE
            );
        }

        private boolean isBetterThan(
                CandidateLayout other
        ) {
            if (
                    positions.size()
                            != other.positions.size()
            ) {
                return positions.size()
                        > other.positions.size();
            }

            if (columnCount != other.columnCount) {
                return columnCount
                        > other.columnCount;
            }

            if (minX != other.minX) {
                return minX < other.minX;
            }

            return minZ < other.minZ;
        }
    }

    private record PlacementOption(
            FoundryControllerBlockEntity controller,
            MergeCandidate mergeCandidate
    ) {
    }

    private record MergeCandidate(
            Set<BlockPos> positions,
            Set<BlockPos> newlyClaimedPositions,
            @Nullable ResourceLocation metal,
            int amount
    ) {
    }
}
