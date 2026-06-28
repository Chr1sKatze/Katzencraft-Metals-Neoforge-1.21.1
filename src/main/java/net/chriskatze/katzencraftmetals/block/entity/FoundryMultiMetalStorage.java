package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.metal.FoundryMetalLayer;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Multi-metal storage facade for one existing FoundryTankNetwork.
 *
 * The structural network remains responsible for ownership, shape, capacity,
 * placement, and splitting. This class owns the actual molten-metal contents
 * and writes density-sorted local layers into each Tank BlockEntity.
 *
 * A compatibility shadow is also written into the old single-metal Tank
 * fields. That keeps the mature structure-management code working while all
 * gameplay and rendering use the real multi-metal layers.
 */
public final class FoundryMultiMetalStorage {

    private FoundryMultiMetalStorage() {
    }

    public static void ensureMigrated(
            Level level,
            FoundryTankNetwork network
    ) {
        if (network == null) {
            return;
        }

        if (
                level == null
                        || level.isClientSide()
        ) {
            return;
        }

        boolean changed =
                false;

        for (BlockPos tankPos : network.getTankPositions()) {
            BlockEntity blockEntity =
                    level.getBlockEntity(tankPos);

            if (
                    blockEntity instanceof FoundryTankBlockEntity tank
                            && !tank.isMultiMetalStorageInitialized()
            ) {
                changed = true;
                break;
            }
        }

        if (!changed) {
            return;
        }

        /*
         * Capture every already-initialized real layer plus genuine legacy
         * storage before changing any Tank. This preserves old saves where one
         * former master Tank may still contain more than one local Tank's
         * capacity.
         *
         * Newly placed Tanks can temporarily receive compatibility-shadow
         * units from the old structural code. They are deliberately ignored
         * unless hasLegacyStorageCandidate() proves those units came from an
         * old save.
         */
        Map<ResourceLocation, Integer> contents =
                new LinkedHashMap<>();

        for (BlockPos tankPos : network.getTankPositions()) {
            BlockEntity blockEntity =
                    level.getBlockEntity(tankPos);

            if (!(blockEntity instanceof FoundryTankBlockEntity tank)) {
                continue;
            }

            if (tank.isMultiMetalStorageInitialized()) {
                for (FoundryMetalLayer layer : tank.getLocalMetalLayers()) {
                    if (ModMoltenMetals.contains(layer.metal())) {
                        contents.merge(
                                layer.metal(),
                                layer.amount(),
                                Integer::sum
                        );
                    }
                }

                continue;
            }

            if (
                    tank.hasLegacyStorageCandidate()
                            && tank.getLocalStoredMetal() != null
                            && tank.getLocalMoltenAmount() > 0
                            && ModMoltenMetals.contains(
                            tank.getLocalStoredMetal()
                    )
            ) {
                contents.merge(
                        tank.getLocalStoredMetal(),
                        tank.getLocalMoltenAmount(),
                        Integer::sum
                );
            }

            /*
             * Initialize empty here. The captured aggregate is redistributed
             * once after every Tank has been converted.
             */
            tank.initializeMultiMetalStorage(
                    false
            );
        }

        redistribute(
                level,
                network,
                contents
        );
    }

    public static boolean canAccept(
            Level level,
            FoundryTankNetwork network,
            ResourceLocation metal,
            int amount
    ) {
        if (
                network == null
                        || !network.isActive()
                        || amount <= 0
                        || !ModMoltenMetals.contains(metal)
        ) {
            return false;
        }

        ensureMigrated(
                level,
                network
        );

        return getTotalAmount(level, network) + amount
                <= network.getCapacity();
    }

    public static int insert(
            Level level,
            FoundryTankNetwork network,
            ResourceLocation metal,
            int requestedAmount
    ) {
        if (
                network == null
                        || !network.isActive()
                        || requestedAmount <= 0
                        || !ModMoltenMetals.contains(metal)
        ) {
            return 0;
        }

        ensureMigrated(level, network);

        Map<ResourceLocation, Integer> contents =
                new LinkedHashMap<>(
                        getContents(level, network)
                );

        int accepted =
                Math.min(
                        requestedAmount,
                        network.getCapacity()
                                - sum(contents)
                );

        if (accepted <= 0) {
            return 0;
        }

        contents.merge(
                metal,
                accepted,
                Integer::sum
        );

        redistribute(
                level,
                network,
                contents
        );

        return accepted;
    }

    public static int extract(
            Level level,
            FoundryTankNetwork network,
            ResourceLocation metal,
            int requestedAmount
    ) {
        if (
                network == null
                        || !network.isActive()
                        || requestedAmount <= 0
                        || metal == null
        ) {
            return 0;
        }

        ensureMigrated(level, network);

        Map<ResourceLocation, Integer> contents =
                new LinkedHashMap<>(
                        getContents(level, network)
                );

        int stored =
                contents.getOrDefault(
                        metal,
                        0
                );

        int extracted =
                Math.min(
                        requestedAmount,
                        stored
                );

        if (extracted <= 0) {
            return 0;
        }

        int remaining =
                stored - extracted;

        if (remaining > 0) {
            contents.put(
                    metal,
                    remaining
            );
        } else {
            contents.remove(metal);
        }

        redistribute(
                level,
                network,
                contents
        );

        return extracted;
    }

    public static Map<ResourceLocation, Integer> getContents(
            Level level,
            FoundryTankNetwork network
    ) {
        if (network == null) {
            return Map.of();
        }

        return getContents(
                level,
                network.getTankPositions()
        );
    }

    public static Map<ResourceLocation, Integer> getContents(
            Level level,
            Set<BlockPos> positions
    ) {
        if (
                level == null
                        || positions.isEmpty()
        ) {
            return Map.of();
        }

        Map<ResourceLocation, Integer> contents =
                new LinkedHashMap<>();

        List<BlockPos> orderedPositions =
                new ArrayList<>(positions);

        orderedPositions.sort(
                Comparator
                        .comparingInt(
                                (BlockPos tankPos) ->
                                        tankPos.getY()
                        )
                        .thenComparingInt(
                                tankPos ->
                                        tankPos.getX()
                        )
                        .thenComparingInt(
                                tankPos ->
                                        tankPos.getZ()
                        )
        );

        for (BlockPos tankPos : orderedPositions) {
            BlockEntity blockEntity =
                    level.getBlockEntity(tankPos);

            if (!(blockEntity instanceof FoundryTankBlockEntity tank)) {
                continue;
            }

            if (
                    !tank.isMultiMetalStorageInitialized()
                            && !tank.hasLegacyStorageCandidate()
            ) {
                /*
                 * A newly placed Tank may already have a structural shadow,
                 * but it has no real liquid until the active network performs
                 * its next redistribution.
                 */
                continue;
            }

            for (
                    FoundryMetalLayer layer :
                    tank.getLocalMetalLayers()
            ) {
                if (!ModMoltenMetals.contains(layer.metal())) {
                    continue;
                }

                contents.merge(
                        layer.metal(),
                        layer.amount(),
                        Integer::sum
                );
            }
        }

        contents.entrySet()
                .removeIf(
                        entry ->
                                entry.getValue() == null
                                        || entry.getValue() <= 0
                );

        return Map.copyOf(contents);
    }

    public static int getAmount(
            Level level,
            FoundryTankNetwork network,
            ResourceLocation metal
    ) {
        if (
                network == null
                        || metal == null
        ) {
            return 0;
        }

        return getContents(level, network)
                .getOrDefault(
                        metal,
                        0
                );
    }

    public static int getTotalAmount(
            Level level,
            FoundryTankNetwork network
    ) {
        return sum(
                getContents(level, network)
        );
    }

    public static boolean isEmpty(
            Level level,
            FoundryTankNetwork network
    ) {
        return getTotalAmount(level, network) <= 0;
    }

    public static boolean isFull(
            Level level,
            FoundryTankNetwork network
    ) {
        return network != null
                && getTotalAmount(level, network)
                >= network.getCapacity();
    }

    /**
     * Returns true when the selected metal physically occupies any Tank in the
     * horizontal layer containing the attached Faucet.
     */
    public static boolean hasMetalAtHeight(
            Level level,
            FoundryTankNetwork network,
            int tankY,
            ResourceLocation metal
    ) {
        if (
                network == null
                        || metal == null
        ) {
            return false;
        }

        for (BlockPos tankPos : network.getTankPositions()) {
            if (tankPos.getY() != tankY) {
                continue;
            }

            BlockEntity blockEntity =
                    level.getBlockEntity(tankPos);

            if (
                    blockEntity
                            instanceof FoundryTankBlockEntity tank
                            && tank.getLocalMetalAmount(metal) > 0
            ) {
                return true;
            }
        }

        return false;
    }

    public static void redistribute(
            Level level,
            FoundryTankNetwork network,
            Map<ResourceLocation, Integer> requestedContents
    ) {
        if (network == null) {
            return;
        }

        redistribute(
                level,
                network.getTankPositions(),
                requestedContents
        );
    }

    /**
     * Used before an upward Tank column is removed. The actual multi-metal
     * contents are moved into the surviving Tanks before the old structural
     * split code runs.
     */
    public static void prepareRemoval(
            Level level,
            FoundryTankNetwork network,
            Set<BlockPos> removedPositions
    ) {
        if (
                network == null
                        || removedPositions == null
                        || removedPositions.isEmpty()
        ) {
            return;
        }

        ensureMigrated(
                level,
                network
        );

        Set<BlockPos> remainingPositions =
                new java.util.HashSet<>(
                        network.getTankPositions()
                );

        remainingPositions.removeAll(
                removedPositions
        );

        Map<ResourceLocation, Integer> contents =
                new LinkedHashMap<>(
                        getContents(level, network)
                );

        for (BlockPos removedPos : removedPositions) {
            BlockEntity blockEntity =
                    level.getBlockEntity(removedPos);

            if (blockEntity instanceof FoundryTankBlockEntity tank) {
                tank.setLocalMetalLayers(
                        List.of()
                );

                tank.setLocalStorage(
                        null,
                        0
                );
            }
        }

        if (remainingPositions.isEmpty()) {
            return;
        }

        redistribute(
                level,
                remainingPositions,
                contents
        );
    }

    public static void redistribute(
            Level level,
            Set<BlockPos> positions,
            Map<ResourceLocation, Integer> requestedContents
    ) {
        if (
                level == null
                        || level.isClientSide()
                        || positions.isEmpty()
        ) {
            return;
        }

        int capacity =
                positions.size()
                        * FoundryTankBlockEntity.CAPACITY;

        Map<ResourceLocation, Integer> normalizedContents =
                normalizeContents(
                        requestedContents,
                        capacity
                );

        Map<Integer, List<BlockPos>> positionsByY =
                new TreeMap<>();

        for (BlockPos tankPos : positions) {
            positionsByY
                    .computeIfAbsent(
                            tankPos.getY(),
                            ignored -> new ArrayList<>()
                    )
                    .add(
                            tankPos.immutable()
                    );
        }

        for (List<BlockPos> layerPositions : positionsByY.values()) {
            layerPositions.sort(
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
        }

        List<MutableMetalAmount> remainingMetals =
                new ArrayList<>();

        for (
                MoltenMetalDefinition definition :
                ModMoltenMetals.heaviestFirst()
        ) {
            int amount =
                    normalizedContents.getOrDefault(
                            definition.id(),
                            0
                    );

            if (amount > 0) {
                remainingMetals.add(
                        new MutableMetalAmount(
                                definition.id(),
                                amount
                        )
                );
            }
        }

        int metalIndex =
                0;

        for (List<BlockPos> layerPositions : positionsByY.values()) {
            int tankCount =
                    layerPositions.size();

            int layerCapacity =
                    tankCount
                            * FoundryTankBlockEntity.CAPACITY;

            List<GlobalLayerSegment> globalSegments =
                    new ArrayList<>();

            int freeInLayer =
                    layerCapacity;

            while (
                    freeInLayer > 0
                            && metalIndex < remainingMetals.size()
            ) {
                MutableMetalAmount current =
                        remainingMetals.get(
                                metalIndex
                        );

                int placed =
                        Math.min(
                                freeInLayer,
                                current.amount
                        );

                if (placed > 0) {
                    globalSegments.add(
                            new GlobalLayerSegment(
                                    current.metal,
                                    placed
                            )
                    );

                    current.amount -=
                            placed;

                    freeInLayer -=
                            placed;
                }

                if (current.amount <= 0) {
                    metalIndex++;
                }
            }

            List<List<FoundryMetalLayer>> perTankLayers =
                    new ArrayList<>();

            int[] previousCumulativeFill =
                    new int[tankCount];

            for (int index = 0; index < tankCount; index++) {
                perTankLayers.add(
                        new ArrayList<>()
                );
            }

            int cumulativeLayerAmount =
                    0;

            for (GlobalLayerSegment segment : globalSegments) {
                cumulativeLayerAmount +=
                        segment.amount();

                int baseTarget =
                        cumulativeLayerAmount
                                / tankCount;

                int targetRemainder =
                        cumulativeLayerAmount
                                % tankCount;

                for (int index = 0; index < tankCount; index++) {
                    int newCumulativeFill =
                            baseTarget
                                    + (
                                    index < targetRemainder
                                            ? 1
                                            : 0
                            );

                    int localSegmentAmount =
                            newCumulativeFill
                                    - previousCumulativeFill[index];

                    if (localSegmentAmount > 0) {
                        appendLayer(
                                perTankLayers.get(index),
                                segment.metal(),
                                localSegmentAmount
                        );
                    }

                    previousCumulativeFill[index] =
                            newCumulativeFill;
                }
            }

            for (int index = 0; index < tankCount; index++) {
                BlockEntity blockEntity =
                        level.getBlockEntity(
                                layerPositions.get(index)
                        );

                if (blockEntity instanceof FoundryTankBlockEntity tank) {
                    tank.setLocalMetalLayers(
                            perTankLayers.get(index)
                    );
                }
            }
        }

        writeCompatibilityShadow(
                level,
                positions,
                normalizedContents
        );
    }

    private static void appendLayer(
            List<FoundryMetalLayer> layers,
            ResourceLocation metal,
            int amount
    ) {
        if (amount <= 0) {
            return;
        }

        if (!layers.isEmpty()) {
            FoundryMetalLayer previous =
                    layers.getLast();

            if (previous.metal().equals(metal)) {
                layers.set(
                        layers.size() - 1,
                        new FoundryMetalLayer(
                                metal,
                                previous.amount()
                                        + amount
                        )
                );

                return;
            }
        }

        layers.add(
                new FoundryMetalLayer(
                        metal,
                        amount
                )
        );
    }

    private static Map<ResourceLocation, Integer> normalizeContents(
            Map<ResourceLocation, Integer> requestedContents,
            int capacity
    ) {
        Map<ResourceLocation, Integer> normalized =
                new LinkedHashMap<>();

        if (requestedContents != null) {
            for (
                    MoltenMetalDefinition definition :
                    ModMoltenMetals.heaviestFirst()
            ) {
                int amount =
                        Math.max(
                                0,
                                requestedContents.getOrDefault(
                                        definition.id(),
                                        0
                                )
                        );

                if (amount > 0) {
                    normalized.put(
                            definition.id(),
                            amount
                    );
                }
            }
        }

        int overflow =
                Math.max(
                        0,
                        sum(normalized)
                                - capacity
                );

        if (overflow > 0) {
            /*
             * Overflow leaves from the physical top first, so the lightest
             * stored metal is discarded before denser lower layers.
             */
            for (
                    MoltenMetalDefinition definition :
                    ModMoltenMetals.lightestFirst()
            ) {
                if (overflow <= 0) {
                    break;
                }

                int stored =
                        normalized.getOrDefault(
                                definition.id(),
                                0
                        );

                int removed =
                        Math.min(
                                stored,
                                overflow
                        );

                int remaining =
                        stored - removed;

                if (remaining > 0) {
                    normalized.put(
                            definition.id(),
                            remaining
                    );
                } else {
                    normalized.remove(
                            definition.id()
                    );
                }

                overflow -=
                        removed;
            }
        }

        return normalized;
    }

    private static void writeCompatibilityShadow(
            Level level,
            Set<BlockPos> positions,
            Map<ResourceLocation, Integer> contents
    ) {
        int total =
                sum(contents);

        /*
         * The old structural network understands only one metal id and rejects
         * layouts whose local ids differ. Use one universal sentinel for every
         * non-empty real composition so Tank sections containing different
         * actual metals can still be claimed, expanded, split, and rejoined.
         * Gameplay and rendering never read this sentinel as real storage.
         */
        ResourceLocation shadowMetal =
                total > 0
                        ? ModMoltenMetals.IRON.id()
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

        int remaining =
                total;

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
                int localAmount =
                        amountPerTank
                                + (
                                index < remainder
                                        ? 1
                                        : 0
                        );

                BlockEntity blockEntity =
                        level.getBlockEntity(
                                layer.get(index)
                        );

                if (blockEntity instanceof FoundryTankBlockEntity tank) {
                    tank.setLocalStorage(
                            localAmount > 0
                                    ? shadowMetal
                                    : null,
                            localAmount
                    );
                }
            }

            remaining -=
                    amountInLayer;
        }
    }

    private static int sum(
            Map<ResourceLocation, Integer> contents
    ) {
        int total =
                0;

        for (Integer amount : contents.values()) {
            if (amount != null && amount > 0) {
                total += amount;
            }
        }

        return total;
    }

    private static final class MutableMetalAmount {

        private final ResourceLocation metal;
        private int amount;

        private MutableMetalAmount(
                ResourceLocation metal,
                int amount
        ) {
            this.metal = metal;
            this.amount = amount;
        }
    }

    private record GlobalLayerSegment(
            ResourceLocation metal,
            int amount
    ) {
    }
}
