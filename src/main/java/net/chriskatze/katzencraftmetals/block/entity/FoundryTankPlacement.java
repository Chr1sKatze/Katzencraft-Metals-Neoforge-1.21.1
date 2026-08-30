package net.chriskatze.katzencraftmetals.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Controller claiming and automatic Tank-placement/merge rules. */
final class FoundryTankPlacement {

    private FoundryTankPlacement() {
    }

    @Nullable
    static FoundryTankNetwork claimLargestUnassignedLayoutForController(
            Level level,
            FoundryControllerBlockEntity controller
    ) {
        CandidateLayout best = CandidateLayout.empty();

        for (BlockPos attachmentPos : controller.getValidTankAttachmentPositions()) {
            BlockEntity blockEntity = level.getBlockEntity(attachmentPos);

            if (!(blockEntity instanceof FoundryTankBlockEntity tank)) {
                continue;
            }

            FoundryTankNetwork existing = tank.getNetwork();

            if (
                    existing != null
                            && existing.getOwnerId() != null
                            && existing.getAttachedController() == null
            ) {
                existing.releaseOwnership();
            }

            blockEntity = level.getBlockEntity(attachmentPos);

            if (
                    !(blockEntity instanceof FoundryTankBlockEntity refreshedTank)
                            || refreshedTank.getNetworkId() != null
            ) {
                continue;
            }

            CandidateLayout candidate = findLargestValidUnassignedLayout(
                    level,
                    attachmentPos,
                    controller
            );

            if (candidate.isBetterThan(best)) {
                best = candidate;
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

    @Nullable
    static FoundryTankNetwork claimLargestUnassignedLayout(
            Level level,
            BlockPos startPos,
            UUID controllerId
    ) {
        CandidateLayout selected = findLargestValidUnassignedLayout(
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

    @Nullable
    static FoundryTankNetwork claimExactLayout(
            Level level,
            Set<BlockPos> selected,
            UUID controllerId
    ) {
        if (
                selected == null
                        || selected.isEmpty()
                        || !FoundryTankStructure.validateStructure(selected).valid()
        ) {
            return null;
        }

        Map<ResourceLocation, Integer> contents =
                FoundryTankStorage.snapshotContents(level, selected);

        if (
                FoundryTankStorage.total(contents)
                        > selected.size() * FoundryTankBlockEntity.CAPACITY
        ) {
            return null;
        }

        for (BlockPos tankPos : selected) {
            BlockEntity blockEntity = level.getBlockEntity(tankPos);

            if (
                    !(blockEntity instanceof FoundryTankBlockEntity tank)
                            || tank.getNetworkId() != null
            ) {
                return null;
            }
        }

        FoundryTankStructure.setOwnerId(level, selected, controllerId);
        FoundryTankStructure.invalidateTankCaches(level, selected);

        FoundryTankNetwork claimed = FoundryTankStructure.find(
                level,
                selected.iterator().next()
        );

        if (claimed == null) {
            FoundryTankStructure.setOwnerId(level, selected, null);
            FoundryTankStructure.invalidateTankCaches(level, selected);
            return null;
        }

        FoundryTankStorage.writeContents(level, selected, contents);
        FoundryTankStructure.pulse(level, selected);
        return claimed;
    }

    /**
     * Finds the largest valid unassigned layout containing startPos. When a
     * controller is supplied, layouts containing a Tank in front are rejected.
     */
    private static CandidateLayout findLargestValidUnassignedLayout(
            Level level,
            BlockPos startPos,
            @Nullable FoundryControllerBlockEntity controller
    ) {
        BlockEntity startBlockEntity = level.getBlockEntity(startPos);

        if (
                !(startBlockEntity instanceof FoundryTankBlockEntity startTank)
                        || startTank.getNetworkId() != null
        ) {
            return CandidateLayout.empty();
        }

        int baseY = findUnassignedColumnBase(level, startPos);

        if (baseY == Integer.MIN_VALUE) {
            return CandidateLayout.empty();
        }

        ColumnKey startColumn = new ColumnKey(
                startPos.getX(),
                startPos.getZ()
        );

        CandidateLayout best = CandidateLayout.empty();

        for (int sizeX = 1; sizeX <= FoundryTankNetwork.MAX_LONG_SIDE; sizeX++) {
            for (int sizeZ = 1; sizeZ <= FoundryTankNetwork.MAX_LONG_SIDE; sizeZ++) {
                if (!FoundryTankStructure.isHorizontalSizeValid(sizeX, sizeZ)) {
                    continue;
                }

                for (
                        int minX = startPos.getX() - sizeX + 1;
                        minX <= startPos.getX();
                        minX++
                ) {
                    int maxX = minX + sizeX - 1;

                    for (
                            int minZ = startPos.getZ() - sizeZ + 1;
                            minZ <= startPos.getZ();
                            minZ++
                    ) {
                        int maxZ = minZ + sizeZ - 1;
                        Map<ColumnKey, Set<BlockPos>> columns = new HashMap<>();

                        for (int x = minX; x <= maxX; x++) {
                            for (int z = minZ; z <= maxZ; z++) {
                                Set<BlockPos> column = readValidUnassignedColumn(
                                        level,
                                        x,
                                        z,
                                        baseY
                                );

                                if (!column.isEmpty()) {
                                    columns.put(new ColumnKey(x, z), column);
                                }
                            }
                        }

                        if (!columns.containsKey(startColumn)) {
                            continue;
                        }

                        Set<ColumnKey> connectedColumns = collectConnectedColumns(
                                columns.keySet(),
                                startColumn
                        );

                        Set<BlockPos> positions = new HashSet<>();

                        for (ColumnKey columnKey : connectedColumns) {
                            positions.addAll(columns.get(columnKey));
                        }

                        FoundryTankStructureValidation.ValidationResult validation =
                                FoundryTankStructure.validateStructure(positions);

                        if (
                                !validation.valid()
                                        || controller != null
                                        && !controller.canOwnTankLayout(positions)
                        ) {
                            continue;
                        }

                        Map<ResourceLocation, Integer> contents =
                                FoundryTankStorage.snapshotContents(level, positions);

                        if (
                                FoundryTankStorage.total(contents)
                                        > positions.size()
                                        * FoundryTankBlockEntity.CAPACITY
                        ) {
                            continue;
                        }

                        CandidateLayout candidate = new CandidateLayout(
                                positions,
                                connectedColumns.size(),
                                minX,
                                minZ
                        );

                        if (candidate.isBetterThan(best)) {
                            best = candidate;
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
        BlockPos current = startPos;

        for (int offset = 0; offset < FoundryTankNetwork.MAX_HEIGHT; offset++) {
            BlockPos below = current.below();
            BlockEntity blockEntity = level.getBlockEntity(below);

            if (
                    blockEntity instanceof FoundryTankBlockEntity tank
                            && tank.getNetworkId() == null
            ) {
                current = below;
                continue;
            }

            return current.getY();
        }

        BlockEntity blockEntity = level.getBlockEntity(current.below());

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
        BlockEntity belowBaseBlockEntity = level.getBlockEntity(
                new BlockPos(x, baseY - 1, z)
        );

        if (
                belowBaseBlockEntity instanceof FoundryTankBlockEntity tank
                        && tank.getNetworkId() == null
        ) {
            return Set.of();
        }

        Set<BlockPos> column = new LinkedHashSet<>();
        boolean foundTank = false;
        boolean foundGapAfterTank = false;

        for (int height = 0; height < FoundryTankNetwork.MAX_HEIGHT; height++) {
            BlockPos checkPos = new BlockPos(x, baseY + height, z);
            BlockEntity blockEntity = level.getBlockEntity(checkPos);

            boolean isUnassignedTank =
                    blockEntity instanceof FoundryTankBlockEntity tank
                            && tank.getNetworkId() == null;

            if (isUnassignedTank) {
                if (foundGapAfterTank) {
                    return Set.of();
                }

                foundTank = true;

                column.add(checkPos.immutable());
            } else if (foundTank) {
                foundGapAfterTank = true;
            }
        }

        if (column.isEmpty()) {
            return Set.of();
        }

        BlockEntity aboveMaximumBlockEntity = level.getBlockEntity(
                new BlockPos(
                        x,
                        baseY + FoundryTankNetwork.MAX_HEIGHT,
                        z
                )
        );

        if (
                aboveMaximumBlockEntity instanceof FoundryTankBlockEntity tank
                        && tank.getNetworkId() == null
        ) {
            return Set.of();
        }

        return column;
    }

    static boolean handleTankPlaced(
            Level level,
            BlockPos placedPos,
            BlockPos clickedAgainstPos
    ) {
        Map<UUID, FoundryControllerBlockEntity> candidateControllers =
                new HashMap<>();

        normalizeAdjacentStaleNetworks(level, placedPos);

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
            BlockPos neighborPos = placedPos.relative(direction);
            BlockEntity blockEntity = level.getBlockEntity(neighborPos);

            if (
                    blockEntity instanceof FoundryControllerBlockEntity controller
                            && controller.isValidTankAttachmentPosition(placedPos)
            ) {
                candidateControllers.putIfAbsent(
                        controller.getControllerId(),
                        controller
                );
                continue;
            }

            if (blockEntity instanceof FoundryTankBlockEntity tank) {
                FoundryTankNetwork network = tank.getNetwork();
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

        Map<UUID, PlacementOption> validOptions = new HashMap<>();

        for (FoundryControllerBlockEntity controller : candidateControllers.values()) {
            FoundryTankNetwork existingNetwork = controller.getOwnedTankNetwork();
            Set<BlockPos> mandatoryPositions = new HashSet<>();

            if (existingNetwork != null) {
                mandatoryPositions.addAll(existingNetwork.getTankPositions());
            }

            mandatoryPositions.add(placedPos.immutable());

            if (!controller.canOwnTankLayout(mandatoryPositions)) {
                continue;
            }

            /*
             * Important robustness rule:
             *
             * Do not reject the active network + newly placed Tank before
             * considering nearby orphan pieces. A previously failed/invalid
             * placement can leave unowned Tank pieces beside the network. The
             * newly placed Tank may be the missing block that makes the full
             * combined shape valid again.
             */
            List<Set<BlockPos>> adjacentOrphanComponents =
                    collectAdjacentOrphanComponents(
                            level,
                            mandatoryPositions
                    );

            MergeCandidate mergeCandidate = findBestMergeCandidate(
                    level,
                    mandatoryPositions,
                    adjacentOrphanComponents,
                    controller
            );

            if (mergeCandidate != null) {
                validOptions.put(
                        controller.getControllerId(),
                        new PlacementOption(controller, mergeCandidate)
                );
            }
        }

        PlacementOption selectedOption = null;

        if (preferredController != null) {
            selectedOption = validOptions.get(
                    preferredController.getControllerId()
            );
        }

        if (selectedOption == null && validOptions.size() == 1) {
            selectedOption = validOptions.values().iterator().next();
        }

        if (selectedOption == null) {
            return false;
        }

        FoundryControllerBlockEntity targetController =
                selectedOption.controller();
        MergeCandidate best = selectedOption.mergeCandidate();

        FoundryTankStructure.setOwnerId(
                level,
                best.positions(),
                targetController.getControllerId()
        );
        FoundryTankStructure.invalidateTankCaches(level, best.positions());

        FoundryTankNetwork merged = FoundryTankStructure.find(level, placedPos);

        if (merged == null) {
            for (BlockPos position : best.newlyClaimedPositions()) {
                BlockEntity blockEntity = level.getBlockEntity(position);
                if (blockEntity instanceof FoundryTankBlockEntity tank) {
                    tank.setNetworkId(null);
                }
            }
            return false;
        }

        FoundryTankStorage.writeContents(level, best.positions(), best.contents());
        FoundryTankStructure.pulse(level, best.positions());
        return true;
    }

    private static void normalizeAdjacentStaleNetworks(
            Level level,
            BlockPos placedPos
    ) {
        Set<BlockPos> checkedNetworks = new HashSet<>();

        for (Direction direction : Direction.values()) {
            BlockEntity blockEntity = level.getBlockEntity(
                    placedPos.relative(direction)
            );

            if (!(blockEntity instanceof FoundryTankBlockEntity tank)) {
                continue;
            }

            FoundryTankNetwork network = tank.getNetwork();

            if (network == null || network.getOwnerId() == null) {
                continue;
            }

            BlockPos key = network.getTankPositions()
                    .stream()
                    .min(FoundryTankStructure.POSITION_ORDER)
                    .orElse(tank.getBlockPos());

            if (
                    checkedNetworks.add(key)
                            && network.getAttachedController() == null
            ) {
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
        BlockEntity clickedBlockEntity = level.getBlockEntity(clickedAgainstPos);

        if (
                clickedBlockEntity instanceof FoundryControllerBlockEntity controller
                        && controller.isValidTankAttachmentPosition(placedPos)
        ) {
            return controller;
        }

        if (clickedBlockEntity instanceof FoundryTankBlockEntity clickedTank) {
            FoundryTankNetwork clickedNetwork = clickedTank.getNetwork();
            if (clickedNetwork != null) {
                return clickedNetwork.getAttachedController();
            }
        }

        return null;
    }

    static boolean hasNearbyFoundryCandidate(
            Level level,
            BlockPos placedPos
    ) {
        for (Direction direction : Direction.values()) {
            BlockEntity blockEntity = level.getBlockEntity(
                    placedPos.relative(direction)
            );

            if (
                    blockEntity instanceof FoundryControllerBlockEntity controller
                            && controller.isValidTankAttachmentPosition(placedPos)
            ) {
                return true;
            }

            if (blockEntity instanceof FoundryTankBlockEntity tank) {
                FoundryTankNetwork network = tank.getNetwork();
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
            Set<BlockPos> anchorPositions
    ) {
        List<Set<BlockPos>> components =
                new ArrayList<>();

        if (
                anchorPositions == null
                        || anchorPositions.isEmpty()
        ) {
            return components;
        }

        Set<BlockPos> excludedPositions =
                new HashSet<>();

        for (BlockPos anchorPosition : anchorPositions) {
            excludedPositions.add(
                    anchorPosition.immutable()
            );
        }

        Set<BlockPos> alreadyCollected =
                new HashSet<>();

        Set<UUID> alreadyCollectedLayoutIds =
                new HashSet<>();

        for (BlockPos anchorPosition : anchorPositions) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbor =
                        anchorPosition.relative(
                                direction
                        );

                if (
                        excludedPositions.contains(neighbor)
                                || alreadyCollected.contains(neighbor)
                ) {
                    continue;
                }

                BlockEntity blockEntity =
                        level.getBlockEntity(
                                neighbor
                        );

                if (
                        !(blockEntity instanceof FoundryTankBlockEntity tank)
                                || tank.getNetworkId() != null
                ) {
                    continue;
                }

                UUID orphanLayoutId =
                        tank.getOrphanLayoutId();

                if (
                        orphanLayoutId != null
                                && !alreadyCollectedLayoutIds.add(orphanLayoutId)
                ) {
                    continue;
                }

                Set<BlockPos> component =
                        orphanLayoutId != null
                                ? collectMatchingOrphanLayout(
                                level,
                                neighbor,
                                orphanLayoutId,
                                excludedPositions
                        )
                                : collectLegacyNullOrphanLayout(
                                level,
                                neighbor,
                                excludedPositions
                        );

                if (!component.isEmpty()) {
                    components.add(
                            component
                    );

                    alreadyCollected.addAll(
                            component
                    );
                }
            }
        }

        return components;
    }

    private static Set<BlockPos> collectMatchingOrphanLayout(
            Level level,
            BlockPos startPos,
            UUID requiredLayoutId,
            Set<BlockPos> excludedPositions
    ) {
        Set<BlockPos> connected =
                new HashSet<>();

        if (excludedPositions.contains(startPos)) {
            return connected;
        }

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

                if (
                        excludedPositions.contains(next)
                                || connected.contains(next)
                ) {
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

    private static Set<BlockPos> collectLegacyNullOrphanLayout(
            Level level,
            BlockPos startPos,
            Set<BlockPos> excludedPositions
    ) {
        Set<BlockPos> connected =
                new HashSet<>();

        if (excludedPositions.contains(startPos)) {
            return connected;
        }

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

                if (
                        excludedPositions.contains(next)
                                || connected.contains(next)
                ) {
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

                queue.addLast(
                        immutableNext
                );
            }
        }

        return connected;
    }

    @Nullable
    private static MergeCandidate findBestMergeCandidate(
            Level level,
            Set<BlockPos> mandatoryPositions,
            List<Set<BlockPos>> optionalOrphanComponents,
            FoundryControllerBlockEntity controller
    ) {
        int componentCount = Math.min(optionalOrphanComponents.size(), 20);
        MergeCandidate best = null;
        int combinationCount = 1 << componentCount;

        for (int mask = 0; mask < combinationCount; mask++) {
            Set<BlockPos> combined = new HashSet<>(mandatoryPositions);
            Set<BlockPos> newlyClaimed = new HashSet<>();

            for (int index = 0; index < componentCount; index++) {
                if ((mask & (1 << index)) == 0) {
                    continue;
                }

                Set<BlockPos> component = optionalOrphanComponents.get(index);
                combined.addAll(component);
                newlyClaimed.addAll(component);
            }

            FoundryTankStructureValidation.ValidationResult validation =
                    FoundryTankStructure.validateStructure(combined);

            if (
                    !validation.valid()
                            || !controller.canOwnTankLayout(combined)
            ) {
                continue;
            }

            Map<ResourceLocation, Integer> contents =
                    FoundryTankStorage.snapshotContents(level, combined);

            if (
                    FoundryTankStorage.total(contents)
                            > combined.size() * FoundryTankBlockEntity.CAPACITY
            ) {
                continue;
            }

            Set<BlockPos> allNewlyClaimed = new HashSet<>(newlyClaimed);

            for (BlockPos position : combined) {
                BlockEntity blockEntity = level.getBlockEntity(position);
                if (
                        blockEntity instanceof FoundryTankBlockEntity tank
                                && tank.getNetworkId() == null
                ) {
                    allNewlyClaimed.add(position);
                }
            }

            MergeCandidate candidate = new MergeCandidate(
                    combined,
                    allNewlyClaimed,
                    contents
            );

            if (
                    best == null
                            || candidate.positions().size() > best.positions().size()
            ) {
                best = candidate;
            }
        }

        return best;
    }

    private static Set<ColumnKey> collectConnectedColumns(
            Set<ColumnKey> availableColumns,
            ColumnKey startColumn
    ) {
        Set<ColumnKey> connected = new HashSet<>();

        if (!availableColumns.contains(startColumn)) {
            return connected;
        }

        java.util.ArrayDeque<ColumnKey> queue = new java.util.ArrayDeque<>();
        connected.add(startColumn);
        queue.addLast(startColumn);

        while (!queue.isEmpty()) {
            ColumnKey current = queue.removeFirst();

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                ColumnKey next = new ColumnKey(
                        current.x() + direction.getStepX(),
                        current.z() + direction.getStepZ()
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

    private record ColumnKey(
            int x,
            int z
    ) {
    }

    private record CandidateLayout(
            Set<BlockPos> positions,
            int columnCount,
            int minX,
            int minZ
    ) {
        private CandidateLayout {
            positions = Set.copyOf(positions);
        }

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
            if (positions.size() != other.positions.size()) {
                return positions.size() > other.positions.size();
            }
            if (columnCount != other.columnCount) {
                return columnCount > other.columnCount;
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
            Map<ResourceLocation, Integer> contents
    ) {
        private MergeCandidate {
            positions = Set.copyOf(positions);
            newlyClaimedPositions = Set.copyOf(newlyClaimedPositions);
            contents = contents == null
                    ? Map.of()
                    : Map.copyOf(contents);
        }
    }
}
