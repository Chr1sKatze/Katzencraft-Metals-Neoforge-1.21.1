$ErrorActionPreference = "Stop"

$validationPath = "src\main\java\net\chriskatze\katzencraftmetals\block\entity\FoundryTankStructureValidation.java"
$placementPath = "src\main\java\net\chriskatze\katzencraftmetals\block\entity\FoundryTankPlacement.java"

if (-not (Test-Path $validationPath)) {
    throw "Could not find $validationPath. Run this script from the project root."
}

if (-not (Test-Path $placementPath)) {
    throw "Could not find $placementPath. Run this script from the project root."
}

$validationContent = @'
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

        Map<Integer, Set<ColumnKey>> columnsByLayer =
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

            columnsByLayer
                    .computeIfAbsent(
                            tankPos.getY(),
                            ignored -> new HashSet<>()
                    )
                    .add(
                            key
                    );
        }

        if (columnHeights.size() > FoundryTankNetwork.MAX_COLUMN_COUNT) {
            return ValidationResult.invalid();
        }

        /*
         * Every occupied vertical column must still be solid inside itself.
         *
         * Difference from the old rule:
         * A column no longer has to start at the global minY. This allows simple
         * overhangs like an upside-down T:
         *
         * Layer 2: [T][T][T]
         * Layer 1: [ ][T][ ]
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
         * The complete structure must be face-connected in 3D. This is normally
         * already true after discovery, but claim/merge candidates also pass
         * through this pure validator.
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
         * Every horizontal liquid level must be one connected footprint.
         *
         * This is the simple-fluid rule:
         * - upside-down T / simple overhangs are allowed
         * - upside-down U shapes are rejected
         *
         * Example allowed:
         * Layer 2: [T][T][T]
         * Layer 1: [ ][T][ ]
         *
         * Example rejected:
         * Layer 2: [T][T][T]
         * Layer 1: [T][ ][T]
         *
         * The rejected shape would create two lower liquid pockets that only
         * connect through an upper bridge, which would need spill-over physics.
         */
        for (Set<ColumnKey> layerColumns : columnsByLayer.values()) {
            if (layerColumns.isEmpty()) {
                continue;
            }

            Set<ColumnKey> connectedLayerColumns =
                    collectConnectedColumns(
                            layerColumns,
                            layerColumns.iterator()
                                    .next()
                    );

            if (connectedLayerColumns.size() != layerColumns.size()) {
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

'@

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText((Resolve-Path $validationPath), $validationContent, $utf8NoBom)

$placementContent = [System.IO.File]::ReadAllText((Resolve-Path $placementPath))

$oldMethod = @'
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
        boolean foundGap = false;

        for (int height = 0; height < FoundryTankNetwork.MAX_HEIGHT; height++) {
            BlockPos checkPos = new BlockPos(x, baseY + height, z);
            BlockEntity blockEntity = level.getBlockEntity(checkPos);

            boolean isUnassignedTank =
                    blockEntity instanceof FoundryTankBlockEntity tank
                            && tank.getNetworkId() == null;

            if (isUnassignedTank) {
                if (foundGap) {
                    return Set.of();
                }
                column.add(checkPos.immutable());
            } else {
                foundGap = true;
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
'@

$newMethod = @'
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
'@

if ($placementContent.Contains($oldMethod)) {
    $placementContent = $placementContent.Replace($oldMethod, $newMethod)
    [System.IO.File]::WriteAllText((Resolve-Path $placementPath), $placementContent, $utf8NoBom)
    Write-Host "Patched FoundryTankPlacement readValidUnassignedColumn."
} elseif ($placementContent.Contains("boolean foundGapAfterTank = false;")) {
    Write-Host "FoundryTankPlacement already appears to be patched."
} else {
    throw "Could not find the expected readValidUnassignedColumn method in FoundryTankPlacement.java."
}

Write-Host "Applied simple T-shape tank overhang support."
Write-Host "Now run: .\gradlew.bat build"
