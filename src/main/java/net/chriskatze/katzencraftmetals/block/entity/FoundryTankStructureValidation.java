package net.chriskatze.katzencraftmetals.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Shape validation for Foundry Tank networks.
 *
 * This is separate from FoundryTankStructure because validation is pure shape
 * logic, while FoundryTankStructure also handles ownership and splitting.
 */
final class FoundryTankStructureValidation {

    private FoundryTankStructureValidation() {
    }

    static ValidationResult validate(
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
