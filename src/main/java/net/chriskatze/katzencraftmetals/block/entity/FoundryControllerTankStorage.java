package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Controller-owned authoritative molten storage for the Foundry Tank structure.
 *
 * Tank blocks never own storage. There is no old-world migration path: the
 * Controller is the only molten-storage owner, and unconnected Tanks are inert.
 */
final class FoundryControllerTankStorage {

    private static final String ROOT_TAG =
            "ControllerTankStorage";

    private static final String CONTENTS_TAG =
            "Contents";

    private static final String POSITIONS_TAG =
            "TankPositions";

    private static final Comparator<BlockPos> POSITION_ORDER =
            Comparator
                    .comparingInt((BlockPos pos) -> pos.getY())
                    .thenComparingInt(BlockPos::getX)
                    .thenComparingInt(BlockPos::getZ);

    private final FoundryControllerBlockEntity controller;

    private final Map<ResourceLocation, Integer> contents =
            new LinkedHashMap<>();

    /* Runtime-only mutation revision for cheap same-tick scheduling caches. */
    private long revision;

    /**
     * Exact physical Tank layout represented by the current controller store.
     * It is persisted so capacity and client visual state are deterministic.
     */
    private Set<BlockPos> boundTankPositions =
            Set.of();

    FoundryControllerTankStorage(
            FoundryControllerBlockEntity controller
    ) {
        this.controller = controller;
    }

    // =========================
    // NETWORK BINDING
    // =========================

    void ensureBound(
            FoundryTankNetwork network
    ) {
        Level level = controller.getLevel();

        if (
                level == null
                        || network == null
                        || !controller.getControllerId()
                        .equals(network.getOwnerId())
        ) {
            return;
        }

        /*
         * Client copies are populated exclusively by the Controller update tag.
         * No client-side structure/storage discovery occurs here.
         */
        if (level.isClientSide()) {
            return;
        }

        Set<BlockPos> currentPositions =
                immutablePositions(network.getTankPositions());

        if (currentPositions.isEmpty()) {
            boolean changed =
                    !contents.isEmpty()
                            || !boundTankPositions.isEmpty();

            contents.clear();
            boundTankPositions = Set.of();

            if (changed) {
                revision++;
                controller.setChanged();
                controller.syncToClient();
            }

            return;
        }

        boolean positionsChanged =
                !boundTankPositions.equals(currentPositions);

        if (positionsChanged) {
            boundTankPositions = currentPositions;
        }

        boolean normalized =
                normalizeToCapacity(network.getCapacity());

        if (positionsChanged || normalized) {
            revision++;
            controller.setChanged();
            controller.syncToClient();
        }
    }

    /**
     * Called by the event-driven structure cache when the physical vessel
     * changes. Existing Controller contents remain authoritative; only capacity
     * may force top-layer overflow.
     */
    void onTankStructureChanged(
            Set<BlockPos> oldPositions,
            Set<BlockPos> newPositions
    ) {
        Level level = controller.getLevel();

        if (level == null || level.isClientSide()) {
            return;
        }

        Set<BlockPos> normalizedNew =
                immutablePositions(newPositions);

        boundTankPositions = normalizedNew;

        int capacity =
                normalizedNew.size()
                        * FoundryTankNetwork.TANK_CAPACITY;

        normalizeToCapacity(capacity);

        if (normalizedNew.isEmpty()) {
            contents.clear();
        }

        /*
         * Physical Tank BlockStates are independent from molten-storage state.
         * FoundryControllerTankStructure performs the one Controller sync after
         * both structure positions and pooled contents are final.
         */
        revision++;
        controller.setChanged();
    }

    void clearForControllerRemoval() {
        contents.clear();
        boundTankPositions = Set.of();
        revision++;

        controller.setChanged();
    }

    Set<BlockPos> getBoundTankPositions() {
        return boundTankPositions;
    }

    long getRevision() {
        return revision;
    }

    /*
     * Persistence must not resolve structure state. The last resolved
     * boundTankPositions + contents snapshot is internally consistent, and a
     * server load revalidates physical structure on the next Controller tick.
     */

    // =========================
    // AUTHORITATIVE STORAGE API
    // =========================

    Map<ResourceLocation, Integer> getContents(
            FoundryTankNetwork network
    ) {
        ensureBound(network);
        return Map.copyOf(contents);
    }

    int getAmount(
            FoundryTankNetwork network,
            ResourceLocation metal
    ) {
        if (metal == null) {
            return 0;
        }

        ensureBound(network);
        return contents.getOrDefault(metal, 0);
    }

    int getTotalAmount(
            FoundryTankNetwork network
    ) {
        ensureBound(network);
        return total(contents);
    }

    boolean canAccept(
            FoundryTankNetwork network,
            ResourceLocation metal,
            int amount
    ) {
        if (
                metal == null
                        || !ModMoltenMetals.contains(metal)
                        || amount <= 0
        ) {
            return false;
        }

        ensureBound(network);

        return total(contents) + amount
                <= network.getCapacity();
    }

    int insert(
            FoundryTankNetwork network,
            ResourceLocation metal,
            int requestedAmount
    ) {
        if (
                metal == null
                        || !ModMoltenMetals.contains(metal)
                        || requestedAmount <= 0
        ) {
            return 0;
        }

        ensureBound(network);

        int accepted =
                Math.min(
                        requestedAmount,
                        network.getCapacity()
                                - total(contents)
                );

        if (accepted <= 0) {
            return 0;
        }

        contents.merge(
                metal,
                accepted,
                Integer::sum
        );

        normalizeToCapacity(
                network.getCapacity()
        );

        revision++;
        controller.setChanged();
        controller.syncToClient();
        return accepted;
    }

    int extract(
            FoundryTankNetwork network,
            ResourceLocation metal,
            int requestedAmount
    ) {
        if (
                metal == null
                        || requestedAmount <= 0
        ) {
            return 0;
        }

        ensureBound(network);

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

        revision++;
        controller.setChanged();
        controller.syncToClient();
        return extracted;
    }

    boolean hasMetalAtHeight(
            FoundryTankNetwork network,
            int tankY,
            ResourceLocation metal
    ) {
        if (metal == null) {
            return false;
        }

        ensureBound(network);

        if (contents.getOrDefault(metal, 0) <= 0) {
            return false;
        }

        Map<Integer, Integer> tanksPerLayer =
                getTanksPerLayer(
                        network.getTankPositions()
                );

        Map<ResourceLocation, Integer> remaining =
                new HashMap<>(contents);

        for (
                Map.Entry<Integer, Integer> layer :
                tanksPerLayer.entrySet()
        ) {
            int freeInLayer =
                    layer.getValue()
                            * FoundryTankNetwork.TANK_CAPACITY;

            for (
                    MoltenMetalDefinition definition :
                    ModMoltenMetals.heaviestFirst()
            ) {
                if (freeInLayer <= 0) {
                    break;
                }

                int available =
                        remaining.getOrDefault(
                                definition.id(),
                                0
                        );

                if (available <= 0) {
                    continue;
                }

                int placed =
                        Math.min(
                                freeInLayer,
                                available
                        );

                if (
                        layer.getKey() == tankY
                                && definition.id().equals(metal)
                                && placed > 0
                ) {
                    return true;
                }

                int left =
                        available - placed;

                if (left > 0) {
                    remaining.put(
                            definition.id(),
                            left
                    );
                } else {
                    remaining.remove(
                            definition.id()
                    );
                }

                freeInLayer -=
                        placed;
            }
        }

        return false;
    }

    float getLocalVisualMoltenAmount(
            FoundryTankNetwork network,
            BlockPos tankPos
    ) {
        if (
                tankPos == null
                        || !network.getTankPositions()
                        .contains(tankPos)
        ) {
            return 0.0f;
        }

        ensureBound(network);


        Map<Integer, Integer> tanksPerLayer =
                getTanksPerLayer(
                        network.getTankPositions()
                );

        int remaining =
                total(contents);

        for (
                Map.Entry<Integer, Integer> layer :
                tanksPerLayer.entrySet()
        ) {
            int layerCapacity =
                    layer.getValue()
                            * FoundryTankNetwork.TANK_CAPACITY;

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
                        FoundryTankNetwork.TANK_CAPACITY
                );
            }

            remaining -=
                    amountInLayer;
        }

        return 0.0f;
    }

    // =========================
    // PERSISTENCE
    // =========================

    void save(
            CompoundTag parent
    ) {
        CompoundTag storageTag =
                new CompoundTag();

        storageTag.put(
                CONTENTS_TAG,
                FoundryAlloyAmounts.writeAmounts(
                        contents
                )
        );

        long[] positions =
                boundTankPositions
                        .stream()
                        .sorted(POSITION_ORDER)
                        .mapToLong(BlockPos::asLong)
                        .toArray();

        storageTag.putLongArray(
                POSITIONS_TAG,
                positions
        );

        parent.put(
                ROOT_TAG,
                storageTag
        );
    }

    void load(
            CompoundTag parent
    ) {
        contents.clear();
        boundTankPositions = Set.of();
        revision++;

        if (!parent.contains(ROOT_TAG, Tag.TAG_COMPOUND)) {
            return;
        }

        CompoundTag storageTag = parent.getCompound(ROOT_TAG);

        if (storageTag.contains(CONTENTS_TAG, Tag.TAG_COMPOUND)) {
            FoundryAlloyAmounts.readAmounts(
                    storageTag.getCompound(CONTENTS_TAG),
                    contents
            );
        }

        if (storageTag.contains(POSITIONS_TAG, Tag.TAG_LONG_ARRAY)) {
            Set<BlockPos> loadedPositions = new LinkedHashSet<>();

            for (long packedPos : storageTag.getLongArray(POSITIONS_TAG)) {
                loadedPositions.add(BlockPos.of(packedPos).immutable());
            }

            boundTankPositions = Set.copyOf(loadedPositions);
        }
    }

    // =========================
    // INTERNAL HELPERS
    // =========================

    /*
     * No Tank BlockState mutation belongs in molten storage.
     *
     * The Controller renderer already renders molten metal full-bright. More
     * importantly, storage/structure changes must not rewrite physical Tank
     * states, because those blocks are intentionally dumb vessel geometry.
     */

    private boolean normalizeToCapacity(
            int capacity
    ) {
        Map<ResourceLocation, Integer> normalized =
                normalizeContents(
                        contents,
                        capacity
                );

        if (contents.equals(normalized)) {
            return false;
        }

        contents.clear();
        contents.putAll(normalized);
        return true;
    }

    /** Overflow leaves from the physical top/lightest metal first. */
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
                        total(normalized)
                                - Math.max(0, capacity)
                );

        if (overflow > 0) {
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

    private static int total(
            Map<ResourceLocation, Integer> source
    ) {
        long total =
                0L;

        if (source == null) {
            return 0;
        }

        for (Integer amount : source.values()) {
            if (amount != null && amount > 0) {
                total += amount;

                if (total >= Integer.MAX_VALUE) {
                    return Integer.MAX_VALUE;
                }
            }
        }

        return (int) total;
    }

    private static Map<Integer, Integer> getTanksPerLayer(
            Set<BlockPos> positions
    ) {
        Map<Integer, Integer> result =
                new TreeMap<>();

        for (BlockPos position : positions) {
            result.merge(
                    position.getY(),
                    1,
                    Integer::sum
            );
        }

        return result;
    }

    private static Set<BlockPos> immutablePositions(
            Set<BlockPos> positions
    ) {
        if (
                positions == null
                        || positions.isEmpty()
        ) {
            return Set.of();
        }

        Set<BlockPos> result =
                new LinkedHashSet<>();

        for (BlockPos position : positions) {
            if (position != null) {
                result.add(
                        position.immutable()
                );
            }
        }

        return Set.copyOf(result);
    }
}
