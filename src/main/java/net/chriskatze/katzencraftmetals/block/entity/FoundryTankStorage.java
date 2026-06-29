package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.metal.FoundryMetalLayer;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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
 * Authoritative molten-content implementation for a Foundry Tank network.
 *
 * Contents are persisted as bottom-to-top local layers in each Tank BlockEntity.
 * This class aggregates those layers into a network map and redistributes them
 * by density whenever contents or structure capacity change.
 */
final class FoundryTankStorage {

    private final FoundryTankNetwork network;

    FoundryTankStorage(
            FoundryTankNetwork network
    ) {
        this.network = network;
    }

    void ensureMigrated() {
        migrateLegacyContents(
                network.level(),
                network.getTankPositions()
        );
    }

    Map<ResourceLocation, Integer> getContents() {
        ensureMigrated();
        return readContentsWithoutMigration(
                network.level(),
                network.getTankPositions()
        );
    }

    int getAmount(
            ResourceLocation metal
    ) {
        if (metal == null) {
            return 0;
        }

        return getContents().getOrDefault(metal, 0);
    }

    int getTotalAmount() {
        return total(getContents());
    }

    boolean canAccept(
            ResourceLocation metal,
            int amount
    ) {
        return network.isActive()
                && metal != null
                && ModMoltenMetals.contains(metal)
                && amount > 0
                && getTotalAmount() + amount <= network.getCapacity();
    }

    int insert(
            ResourceLocation metal,
            int requestedAmount
    ) {
        if (
                !network.isActive()
                        || metal == null
                        || !ModMoltenMetals.contains(metal)
                        || requestedAmount <= 0
        ) {
            return 0;
        }

        Map<ResourceLocation, Integer> contents =
                new LinkedHashMap<>(getContents());

        int accepted = Math.min(
                requestedAmount,
                network.getCapacity() - total(contents)
        );

        if (accepted <= 0) {
            return 0;
        }

        contents.merge(metal, accepted, Integer::sum);

        writeContents(
                network.level(),
                network.getTankPositions(),
                contents
        );

        return accepted;
    }

    int extract(
            ResourceLocation metal,
            int requestedAmount
    ) {
        if (
                !network.isActive()
                        || metal == null
                        || requestedAmount <= 0
        ) {
            return 0;
        }

        Map<ResourceLocation, Integer> contents =
                new LinkedHashMap<>(getContents());

        int stored = contents.getOrDefault(metal, 0);
        int extracted = Math.min(requestedAmount, stored);

        if (extracted <= 0) {
            return 0;
        }

        int remaining = stored - extracted;

        if (remaining > 0) {
            contents.put(metal, remaining);
        } else {
            contents.remove(metal);
        }

        writeContents(
                network.level(),
                network.getTankPositions(),
                contents
        );

        return extracted;
    }

    boolean hasMetalAtHeight(
            int tankY,
            ResourceLocation metal
    ) {
        if (metal == null) {
            return false;
        }

        ensureMigrated();

        for (BlockPos tankPos : network.getTankPositions()) {
            if (tankPos.getY() != tankY) {
                continue;
            }

            BlockEntity blockEntity =
                    network.level().getBlockEntity(tankPos);

            if (
                    blockEntity instanceof FoundryTankBlockEntity tank
                            && tank.getLocalMetalAmount(metal) > 0
            ) {
                return true;
            }
        }

        return false;
    }

    float getLocalVisualMoltenAmount(
            BlockPos tankPos
    ) {
        if (!network.getTankPositions().contains(tankPos)) {
            return 0.0f;
        }

        Map<Integer, Integer> tanksPerLayer = new TreeMap<>();

        for (BlockPos position : network.getTankPositions()) {
            tanksPerLayer.merge(position.getY(), 1, Integer::sum);
        }

        int remaining = getTotalAmount();

        for (Map.Entry<Integer, Integer> layer : tanksPerLayer.entrySet()) {
            int layerCapacity =
                    layer.getValue() * FoundryTankBlockEntity.CAPACITY;

            int amountInLayer = Math.min(remaining, layerCapacity);

            if (layer.getKey() == tankPos.getY()) {
                return Mth.clamp(
                        (float) amountInLayer / layer.getValue(),
                        0.0f,
                        FoundryTankBlockEntity.CAPACITY
                );
            }

            remaining -= amountInLayer;
        }

        return 0.0f;
    }

    // =========================
    // STRUCTURE-LEVEL ACCESS
    // =========================

    static Map<ResourceLocation, Integer> readContents(
            Level level,
            Set<BlockPos> positions
    ) {
        migrateLegacyContents(level, positions);
        return readContentsWithoutMigration(level, positions);
    }

    /**
     * Non-mutating structural snapshot. This is used while evaluating possible
     * claim/merge layouts so an old save is never migrated into a temporary
     * candidate subset before the winning layout has been selected.
     */
    static Map<ResourceLocation, Integer> snapshotContents(
            Level level,
            Set<BlockPos> positions
    ) {
        if (
                level == null
                        || positions == null
                        || positions.isEmpty()
        ) {
            return Map.of();
        }

        Map<ResourceLocation, Integer> contents = new LinkedHashMap<>();

        for (BlockPos tankPos : positions) {
            BlockEntity blockEntity = level.getBlockEntity(tankPos);

            if (!(blockEntity instanceof FoundryTankBlockEntity tank)) {
                continue;
            }

            for (
                    FoundryMetalLayer layer :
                    tank.getAuthoritativeLocalMetalLayers()
            ) {
                if (
                        layer.amount() > 0
                                && ModMoltenMetals.contains(layer.metal())
                ) {
                    contents.merge(
                            layer.metal(),
                            layer.amount(),
                            Integer::sum
                    );
                }
            }

            if (!tank.hasPendingLegacyStorage()) {
                continue;
            }

            ResourceLocation legacyMetal = tank.getPendingLegacyMetal();

            if (legacyMetal != null) {
                contents.merge(
                        legacyMetal,
                        tank.getPendingLegacyAmount(),
                        Integer::sum
                );
            }
        }

        contents.entrySet().removeIf(
                entry -> entry.getValue() == null || entry.getValue() <= 0
        );

        return Map.copyOf(contents);
    }

    static void writeContents(
            Level level,
            Set<BlockPos> positions,
            Map<ResourceLocation, Integer> requestedContents
    ) {
        if (
                level == null
                        || level.isClientSide()
                        || positions == null
                        || positions.isEmpty()
        ) {
            return;
        }

        int capacity =
                positions.size() * FoundryTankBlockEntity.CAPACITY;

        Map<ResourceLocation, Integer> normalizedContents =
                normalizeContents(requestedContents, capacity);

        Map<Integer, List<BlockPos>> positionsByY = new TreeMap<>();

        for (BlockPos tankPos : positions) {
            positionsByY
                    .computeIfAbsent(
                            tankPos.getY(),
                            ignored -> new ArrayList<>()
                    )
                    .add(tankPos.immutable());
        }

        for (List<BlockPos> layerPositions : positionsByY.values()) {
            layerPositions.sort(
                    Comparator
                            .comparingInt(
                                    (BlockPos tankPos) -> tankPos.getX()
                            )
                            .thenComparingInt(
                                    tankPos -> tankPos.getZ()
                            )
            );
        }

        List<MutableMetalAmount> remainingMetals = new ArrayList<>();

        for (MoltenMetalDefinition definition : ModMoltenMetals.heaviestFirst()) {
            int amount = normalizedContents.getOrDefault(definition.id(), 0);

            if (amount > 0) {
                remainingMetals.add(
                        new MutableMetalAmount(definition.id(), amount)
                );
            }
        }

        int metalIndex = 0;

        for (List<BlockPos> layerPositions : positionsByY.values()) {
            int tankCount = layerPositions.size();
            int layerCapacity =
                    tankCount * FoundryTankBlockEntity.CAPACITY;

            List<GlobalLayerSegment> globalSegments = new ArrayList<>();
            int freeInLayer = layerCapacity;

            while (
                    freeInLayer > 0
                            && metalIndex < remainingMetals.size()
            ) {
                MutableMetalAmount current = remainingMetals.get(metalIndex);
                int placed = Math.min(freeInLayer, current.amount);

                if (placed > 0) {
                    globalSegments.add(
                            new GlobalLayerSegment(current.metal, placed)
                    );
                    current.amount -= placed;
                    freeInLayer -= placed;
                }

                if (current.amount <= 0) {
                    metalIndex++;
                }
            }

            List<List<FoundryMetalLayer>> perTankLayers = new ArrayList<>();
            int[] previousCumulativeFill = new int[tankCount];

            for (int index = 0; index < tankCount; index++) {
                perTankLayers.add(new ArrayList<>());
            }

            int cumulativeLayerAmount = 0;

            for (GlobalLayerSegment segment : globalSegments) {
                cumulativeLayerAmount += segment.amount();

                int baseTarget = cumulativeLayerAmount / tankCount;
                int targetRemainder = cumulativeLayerAmount % tankCount;

                for (int index = 0; index < tankCount; index++) {
                    int newCumulativeFill =
                            baseTarget + (index < targetRemainder ? 1 : 0);

                    int localSegmentAmount =
                            newCumulativeFill - previousCumulativeFill[index];

                    if (localSegmentAmount > 0) {
                        appendLayer(
                                perTankLayers.get(index),
                                segment.metal(),
                                localSegmentAmount
                        );
                    }

                    previousCumulativeFill[index] = newCumulativeFill;
                }
            }

            for (int index = 0; index < tankCount; index++) {
                BlockEntity blockEntity =
                        level.getBlockEntity(layerPositions.get(index));

                if (blockEntity instanceof FoundryTankBlockEntity tank) {
                    tank.setLocalMetalLayers(perTankLayers.get(index));
                }
            }
        }
    }

    static void clearContents(
            Level level,
            Set<BlockPos> positions
    ) {
        if (level == null || positions == null) {
            return;
        }

        for (BlockPos position : positions) {
            BlockEntity blockEntity = level.getBlockEntity(position);

            if (blockEntity instanceof FoundryTankBlockEntity tank) {
                tank.setLocalMetalLayers(List.of());
            }
        }
    }

    static int total(
            Map<ResourceLocation, Integer> contents
    ) {
        int total = 0;

        if (contents == null) {
            return total;
        }

        for (Integer amount : contents.values()) {
            if (amount != null && amount > 0) {
                total += amount;
            }
        }

        return total;
    }

    // =========================
    // READ / MIGRATION
    // =========================

    private static Map<ResourceLocation, Integer> readContentsWithoutMigration(
            Level level,
            Set<BlockPos> positions
    ) {
        if (
                level == null
                        || positions == null
                        || positions.isEmpty()
        ) {
            return Map.of();
        }

        Map<ResourceLocation, Integer> contents = new LinkedHashMap<>();
        List<BlockPos> orderedPositions = new ArrayList<>(positions);

        orderedPositions.sort(
                Comparator
                        .comparingInt(
                                (BlockPos tankPos) -> tankPos.getY()
                        )
                        .thenComparingInt(
                                tankPos -> tankPos.getX()
                        )
                        .thenComparingInt(
                                tankPos -> tankPos.getZ()
                        )
        );

        for (BlockPos tankPos : orderedPositions) {
            BlockEntity blockEntity = level.getBlockEntity(tankPos);

            if (!(blockEntity instanceof FoundryTankBlockEntity tank)) {
                continue;
            }

            for (FoundryMetalLayer layer : tank.getLocalMetalLayers()) {
                if (
                        layer.amount() <= 0
                                || !ModMoltenMetals.contains(layer.metal())
                ) {
                    continue;
                }

                contents.merge(
                        layer.metal(),
                        layer.amount(),
                        Integer::sum
                );
            }
        }

        contents.entrySet().removeIf(
                entry -> entry.getValue() == null || entry.getValue() <= 0
        );

        return Map.copyOf(contents);
    }

    /**
     * Aggregates legacy values across the whole structure before converting
     * them, because an old master Tank may contain more than one Tank capacity.
     */
    private static void migrateLegacyContents(
            Level level,
            Set<BlockPos> positions
    ) {
        if (
                level == null
                        || level.isClientSide()
                        || positions == null
                        || positions.isEmpty()
        ) {
            return;
        }

        boolean hasPendingLegacy = false;
        Map<ResourceLocation, Integer> contents = new LinkedHashMap<>();

        for (BlockPos tankPos : positions) {
            BlockEntity blockEntity = level.getBlockEntity(tankPos);

            if (!(blockEntity instanceof FoundryTankBlockEntity tank)) {
                continue;
            }

            for (
                    FoundryMetalLayer layer :
                    tank.getAuthoritativeLocalMetalLayers()
            ) {
                if (
                        layer.amount() > 0
                                && ModMoltenMetals.contains(layer.metal())
                ) {
                    contents.merge(
                            layer.metal(),
                            layer.amount(),
                            Integer::sum
                    );
                }
            }

            if (!tank.hasPendingLegacyStorage()) {
                continue;
            }

            ResourceLocation legacyMetal = tank.getPendingLegacyMetal();

            if (legacyMetal == null) {
                continue;
            }

            hasPendingLegacy = true;
            contents.merge(
                    legacyMetal,
                    tank.getPendingLegacyAmount(),
                    Integer::sum
            );
        }

        if (!hasPendingLegacy) {
            return;
        }

        for (BlockPos tankPos : positions) {
            BlockEntity blockEntity = level.getBlockEntity(tankPos);

            if (blockEntity instanceof FoundryTankBlockEntity tank) {
                tank.clearPendingLegacyStorage();
            }
        }

        writeContents(level, positions, contents);
    }

    // =========================
    // NORMALIZATION
    // =========================

    private static void appendLayer(
            List<FoundryMetalLayer> layers,
            ResourceLocation metal,
            int amount
    ) {
        if (amount <= 0) {
            return;
        }

        if (!layers.isEmpty()) {
            FoundryMetalLayer previous = layers.getLast();

            if (previous.metal().equals(metal)) {
                layers.set(
                        layers.size() - 1,
                        new FoundryMetalLayer(
                                metal,
                                previous.amount() + amount
                        )
                );
                return;
            }
        }

        layers.add(new FoundryMetalLayer(metal, amount));
    }

    /** Overflow leaves from the physical top/lightest metal first. */
    private static Map<ResourceLocation, Integer> normalizeContents(
            Map<ResourceLocation, Integer> requestedContents,
            int capacity
    ) {
        Map<ResourceLocation, Integer> normalized = new LinkedHashMap<>();

        if (requestedContents != null) {
            for (MoltenMetalDefinition definition : ModMoltenMetals.heaviestFirst()) {
                int amount = Math.max(
                        0,
                        requestedContents.getOrDefault(definition.id(), 0)
                );

                if (amount > 0) {
                    normalized.put(definition.id(), amount);
                }
            }
        }

        int overflow = Math.max(0, total(normalized) - capacity);

        if (overflow > 0) {
            for (MoltenMetalDefinition definition : ModMoltenMetals.lightestFirst()) {
                if (overflow <= 0) {
                    break;
                }

                int stored = normalized.getOrDefault(definition.id(), 0);
                int removed = Math.min(stored, overflow);
                int remaining = stored - removed;

                if (remaining > 0) {
                    normalized.put(definition.id(), remaining);
                } else {
                    normalized.remove(definition.id());
                }

                overflow -= removed;
            }
        }

        return Map.copyOf(normalized);
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
