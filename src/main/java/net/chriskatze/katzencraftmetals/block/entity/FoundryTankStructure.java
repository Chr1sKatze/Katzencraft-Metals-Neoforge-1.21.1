package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
import java.util.UUID;

/** Discovery, validation, ownership, breaking, and component splitting. */
final class FoundryTankStructure {

    static final Comparator<BlockPos> POSITION_ORDER =
            Comparator
                    .comparingInt((BlockPos pos) -> pos.getY())
                    .thenComparingInt(pos -> pos.getX())
                    .thenComparingInt(pos -> pos.getZ());

    private FoundryTankStructure() {
    }

    @Nullable
    static FoundryTankNetwork find(
            Level level,
            BlockPos startPos
    ) {
        BlockEntity startBlockEntity = level.getBlockEntity(startPos);

        if (!(startBlockEntity instanceof FoundryTankBlockEntity startTank)) {
            return null;
        }

        UUID ownerId = startTank.getNetworkId();

        Set<BlockPos> connected = collectConnectedTanks(
                level,
                startPos,
                ownerId,
                Set.of()
        );

        ValidationResult validation = validateStructure(connected);

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

    static Set<BlockPos> collectConnectedTanks(
            Level level,
            BlockPos startPos,
            @Nullable UUID requiredOwnerId,
            Set<BlockPos> excludedPositions
    ) {
        Set<BlockPos> connected = new HashSet<>();

        if (excludedPositions.contains(startPos)) {
            return connected;
        }

        BlockEntity startBlockEntity = level.getBlockEntity(startPos);

        if (!(startBlockEntity instanceof FoundryTankBlockEntity startTank)) {
            return connected;
        }

        if (!Objects.equals(requiredOwnerId, startTank.getNetworkId())) {
            return connected;
        }

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        BlockPos immutableStart = startPos.immutable();

        connected.add(immutableStart);
        queue.addLast(immutableStart);

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();

            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);

                if (
                        excludedPositions.contains(next)
                                || connected.contains(next)
                ) {
                    continue;
                }

                BlockEntity blockEntity = level.getBlockEntity(next);

                if (!(blockEntity instanceof FoundryTankBlockEntity tank)) {
                    continue;
                }

                if (!Objects.equals(requiredOwnerId, tank.getNetworkId())) {
                    continue;
                }

                BlockPos immutableNext = next.immutable();
                connected.add(immutableNext);
                queue.addLast(immutableNext);
            }
        }

        return connected;
    }

    // =========================
    // CONTROLLER OWNERSHIP
    // =========================

    @Nullable
    static FoundryControllerBlockEntity findAttachedController(
            Level level,
            Set<BlockPos> positions,
            UUID controllerId
    ) {
        Set<BlockPos> checkedControllers = new HashSet<>();

        for (BlockPos tankPos : positions) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos controllerPos = tankPos.relative(direction);

                if (!checkedControllers.add(controllerPos)) {
                    continue;
                }

                BlockEntity blockEntity = level.getBlockEntity(controllerPos);

                if (
                        !(blockEntity instanceof FoundryControllerBlockEntity controller)
                                || !controllerId.equals(controller.getControllerId())
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

    static void releaseOwnership(
            FoundryTankNetwork network
    ) {
        Level level = network.level();
        Set<BlockPos> positions = network.getTankPositions();
        Map<ResourceLocation, Integer> contents =
                FoundryTankStorage.readContents(level, positions);

        setOwnerId(level, positions, null);
        invalidateTankCaches(level, positions);
        FoundryTankStorage.writeContents(level, positions, contents);
        pulse(level, positions);
    }

    // =========================
    // VALIDATION
    // =========================

    static ValidationResult validateStructure(
            Set<BlockPos> positions
    ) {
        if (
                positions == null
                        || positions.isEmpty()
                        || positions.size() > FoundryTankNetwork.MAX_TANK_COUNT
        ) {
            return ValidationResult.invalid();
        }

        int minX = positions.stream()
                .mapToInt(BlockPos::getX)
                .min()
                .orElse(0);
        int maxX = positions.stream()
                .mapToInt(BlockPos::getX)
                .max()
                .orElse(0);
        int minY = positions.stream()
                .mapToInt(BlockPos::getY)
                .min()
                .orElse(0);
        int minZ = positions.stream()
                .mapToInt(BlockPos::getZ)
                .min()
                .orElse(0);
        int maxZ = positions.stream()
                .mapToInt(BlockPos::getZ)
                .max()
                .orElse(0);

        int sizeX = maxX - minX + 1;
        int sizeZ = maxZ - minZ + 1;

        if (!isHorizontalSizeValid(sizeX, sizeZ)) {
            return ValidationResult.invalid();
        }

        Map<ColumnKey, Set<Integer>> columnHeights = new HashMap<>();

        for (BlockPos tankPos : positions) {
            ColumnKey key = new ColumnKey(
                    tankPos.getX(),
                    tankPos.getZ()
            );

            columnHeights
                    .computeIfAbsent(key, ignored -> new HashSet<>())
                    .add(tankPos.getY());
        }

        if (columnHeights.size() > FoundryTankNetwork.MAX_COLUMN_COUNT) {
            return ValidationResult.invalid();
        }

        for (Set<Integer> yValues : columnHeights.values()) {
            int columnMaxY = yValues.stream()
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(minY);

            int columnHeight = columnMaxY - minY + 1;

            if (
                    columnHeight < 1
                            || columnHeight > FoundryTankNetwork.MAX_HEIGHT
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

        Set<ColumnKey> connectedColumns = collectConnectedColumns(
                columnHeights.keySet(),
                columnHeights.keySet().iterator().next()
        );

        if (connectedColumns.size() != columnHeights.size()) {
            return ValidationResult.invalid();
        }

        return new ValidationResult(true, minY);
    }

    static boolean isHorizontalSizeValid(
            int sizeX,
            int sizeZ
    ) {
        return (
                sizeX >= 1
                        && sizeZ >= 1
                        && sizeX <= FoundryTankNetwork.MAX_SHORT_SIDE
                        && sizeZ <= FoundryTankNetwork.MAX_LONG_SIDE
        ) || (
                sizeX >= 1
                        && sizeZ >= 1
                        && sizeX <= FoundryTankNetwork.MAX_LONG_SIDE
                        && sizeZ <= FoundryTankNetwork.MAX_SHORT_SIDE
        );
    }

    private static Set<ColumnKey> collectConnectedColumns(
            Set<ColumnKey> availableColumns,
            ColumnKey startColumn
    ) {
        Set<ColumnKey> connected = new HashSet<>();

        if (!availableColumns.contains(startColumn)) {
            return connected;
        }

        ArrayDeque<ColumnKey> queue = new ArrayDeque<>();
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

    // =========================
    // BREAKING / SPLITTING
    // =========================

    static Set<BlockPos> findUpwardColumn(
            Level level,
            BlockPos startPos
    ) {
        BlockEntity startBlockEntity = level.getBlockEntity(startPos);

        if (!(startBlockEntity instanceof FoundryTankBlockEntity startTank)) {
            return Set.of();
        }

        UUID requiredOwnerId = startTank.getNetworkId();
        Set<BlockPos> removed = new LinkedHashSet<>();
        removed.add(startPos.immutable());

        for (int offset = 1; offset < FoundryTankNetwork.MAX_HEIGHT; offset++) {
            BlockPos checkPos = startPos.above(offset);
            BlockEntity blockEntity = level.getBlockEntity(checkPos);

            if (
                    !(blockEntity instanceof FoundryTankBlockEntity tank)
                            || !Objects.equals(
                            requiredOwnerId,
                            tank.getNetworkId()
                    )
            ) {
                break;
            }

            removed.add(checkPos.immutable());
        }

        return removed;
    }

    static void prepareUpwardRemoval(
            FoundryTankNetwork network,
            Set<BlockPos> removedPositions
    ) {
        if (removedPositions == null || removedPositions.isEmpty()) {
            return;
        }

        network.ensureMoltenContentsMigrated();

        Level level = network.level();
        FoundryControllerBlockEntity ownerController =
                network.getAttachedController();
        Map<ResourceLocation, Integer> originalContents =
                FoundryTankStorage.readContents(
                        level,
                        network.getTankPositions()
                );

        Set<BlockPos> remainingPositions =
                new HashSet<>(network.getTankPositions());
        remainingPositions.removeAll(removedPositions);

        FoundryTankStorage.clearContents(level, removedPositions);

        if (remainingPositions.isEmpty()) {
            return;
        }

        /*
         * Refill the surviving physical space before splitting. Any unavoidable
         * overflow leaves from the physical top/lightest metal first.
         */
        FoundryTankStorage.writeContents(
                level,
                remainingPositions,
                originalContents
        );

        List<Set<BlockPos>> components =
                splitIntoComponents(remainingPositions);
        Set<BlockPos> controllerComponent =
                chooseControllerComponent(components, ownerController);

        for (Set<BlockPos> component : components) {
            Map<ResourceLocation, Integer> componentContents =
                    FoundryTankStorage.readContents(level, component);

            UUID resultingOwnerId =
                    network.getOwnerId() != null
                            && component == controllerComponent
                            ? network.getOwnerId()
                            : null;

            setOwnerId(level, component, resultingOwnerId);
            invalidateTankCaches(level, component);
            FoundryTankStorage.writeContents(
                    level,
                    component,
                    componentContents
            );
            pulse(level, component);
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

        Set<BlockPos> best = null;
        long bestDistance = Long.MAX_VALUE;

        for (Set<BlockPos> component : components) {
            if (!controller.canOwnTankLayout(component)) {
                continue;
            }

            long closestDistance = Long.MAX_VALUE;

            for (BlockPos tankPos : component) {
                long deltaX =
                        tankPos.getX() - controller.getBlockPos().getX();
                long deltaY =
                        tankPos.getY() - controller.getBlockPos().getY();
                long deltaZ =
                        tankPos.getZ() - controller.getBlockPos().getZ();

                long distance =
                        deltaX * deltaX
                                + deltaY * deltaY
                                + deltaZ * deltaZ;

                closestDistance = Math.min(closestDistance, distance);
            }

            if (
                    best == null
                            || component.size() > best.size()
                            || component.size() == best.size()
                            && closestDistance < bestDistance
            ) {
                best = component;
                bestDistance = closestDistance;
            }
        }

        return best;
    }

    private static List<Set<BlockPos>> splitIntoComponents(
            Set<BlockPos> positions
    ) {
        List<Set<BlockPos>> components = new ArrayList<>();
        Set<BlockPos> remaining = new HashSet<>(positions);

        while (!remaining.isEmpty()) {
            BlockPos start = remaining.iterator().next();
            Set<BlockPos> component = new HashSet<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();

            component.add(start);
            queue.addLast(start);
            remaining.remove(start);

            while (!queue.isEmpty()) {
                BlockPos current = queue.removeFirst();

                for (Direction direction : Direction.values()) {
                    BlockPos next = current.relative(direction);

                    if (!remaining.remove(next)) {
                        continue;
                    }

                    BlockPos immutableNext = next.immutable();
                    component.add(immutableNext);
                    queue.addLast(immutableNext);
                }
            }

            components.add(component);
        }

        return components;
    }

    static List<BlockPos> findAttachedFaucets(
            Level level,
            Set<BlockPos> tankSubset
    ) {
        Set<BlockPos> faucets = new HashSet<>();

        for (BlockPos tankPos : tankSubset) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos faucetPos = tankPos.relative(direction);
                BlockState faucetState = level.getBlockState(faucetPos);

                if (!faucetState.hasProperty(FoundryFaucetBlock.FACING)) {
                    continue;
                }

                if (
                        faucetState.getValue(FoundryFaucetBlock.FACING)
                                != direction
                ) {
                    continue;
                }

                if (
                        level.getBlockEntity(faucetPos)
                                instanceof FoundryFaucetBlockEntity
                ) {
                    faucets.add(faucetPos.immutable());
                }
            }
        }

        return new ArrayList<>(faucets);
    }

    // =========================
    // SHARED MUTATION HELPERS
    // =========================

    static void setOwnerId(
            Level level,
            Set<BlockPos> positions,
            @Nullable UUID ownerId
    ) {
        for (BlockPos tankPos : positions) {
            BlockEntity blockEntity = level.getBlockEntity(tankPos);

            if (blockEntity instanceof FoundryTankBlockEntity tank) {
                tank.setNetworkId(ownerId);
            }
        }
    }

    static void invalidateTankCaches(
            Level level,
            Set<BlockPos> positions
    ) {
        for (BlockPos tankPos : positions) {
            BlockEntity blockEntity = level.getBlockEntity(tankPos);

            if (blockEntity instanceof FoundryTankBlockEntity tank) {
                tank.invalidateNetworkCache();
            }
        }
    }

    static void pulse(
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

    private record ColumnKey(
            int x,
            int z
    ) {
    }

    record ValidationResult(
            boolean valid,
            int minY
    ) {
        static ValidationResult invalid() {
            return new ValidationResult(false, 0);
        }
    }
}
