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
        int maxY = positions.stream()
                .mapToInt(BlockPos::getY)
                .max()
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
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;

        if (
                sizeY < 1
                        || sizeY > FoundryTankNetwork.MAX_HEIGHT
                        || !isHorizontalSizeValid(sizeX, sizeZ)
        ) {
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
                    .add(
                            tankPos.getY()
                    );
        }

        if (columnHeights.size() > FoundryTankNetwork.MAX_COLUMN_COUNT) {
            return ValidationResult.invalid();
        }

        /*
         * Every occupied vertical column must still be solid inside itself.
         *
         * A column no longer has to start at the global minY. This allows
         * simple overhangs and normal U-shapes.
         *
         * But a column may not contain an internal gap.
         */
        for (Set<Integer> yValues : columnHeights.values()) {
            int columnMinY = yValues.stream()
                    .mapToInt(Integer::intValue)
                    .min()
                    .orElse(minY);

            int columnMaxY = yValues.stream()
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(minY);

            int columnHeight =
                    columnMaxY
                            - columnMinY
                            + 1;

            if (
                    columnHeight < 1
                            || columnHeight > FoundryTankNetwork.MAX_HEIGHT
                            || yValues.size() != columnHeight
            ) {
                return ValidationResult.invalid();
            }

            for (int y = columnMinY; y <= columnMaxY; y++) {
                if (!yValues.contains(y)) {
                    return ValidationResult.invalid();
                }
            }
        }

        /*
         * The complete structure must be face-connected in 3D.
         */
        Set<BlockPos> connectedTankBlocks =
                collectConnectedTankBlocks(
                        positions,
                        positions.iterator()
                                .next()
                );

        if (connectedTankBlocks.size() != positions.size()) {
            return ValidationResult.invalid();
        }

        /*
         * Simple-fluid rule:
         *
         * For every height, the filled volume from the bottom up to that height
         * must be connected.
         *
         * This allows a normal U:
         *
         * Layer 2: [T][ ][ ][T]
         * Layer 1: [T][T][T][T]
         *
         * At layer 1 the bottom is connected. At layer 2 the full bottom plus
         * both raised sides are still one connected volume.
         *
         * But it rejects an upside-down U:
         *
         * Layer 2: [T][T][T][T]
         * Layer 1: [T][ ][ ][T]
         *
         * At layer 1 the lower liquid pockets are already split, so they would
         * only equalize later by spilling over the upper bridge.
         */
        for (int y = minY; y <= maxY; y++) {
            Set<BlockPos> bottomAccessiblePrefix =
                    collectPositionsAtOrBelowY(
                            positions,
                            y
                    );

            if (bottomAccessiblePrefix.isEmpty()) {
                continue;
            }

            Set<BlockPos> connectedPrefix =
                    collectConnectedTankBlocks(
                            bottomAccessiblePrefix,
                            bottomAccessiblePrefix.iterator()
                                    .next()
                    );

            if (connectedPrefix.size() != bottomAccessiblePrefix.size()) {
                return ValidationResult.invalid();
            }
        }

        return new ValidationResult(
                true,
                minY
        );
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

    private static Set<BlockPos> collectPositionsAtOrBelowY(
            Set<BlockPos> positions,
            int y
    ) {
        Set<BlockPos> collected =
                new HashSet<>();

        for (BlockPos position : positions) {
            if (position.getY() <= y) {
                collected.add(
                        position
                );
            }
        }

        return collected;
    }

    private static Set<BlockPos> collectConnectedTankBlocks(
            Set<BlockPos> availablePositions,
            BlockPos startPos
    ) {
        Set<BlockPos> connected =
                new HashSet<>();

        if (!availablePositions.contains(startPos)) {
            return connected;
        }

        ArrayDeque<BlockPos> queue =
                new ArrayDeque<>();

        connected.add(
                startPos
        );

        queue.addLast(
                startPos
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
                        availablePositions.contains(next)
                                && connected.add(next)
                ) {
                    queue.addLast(
                            next
                    );
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
