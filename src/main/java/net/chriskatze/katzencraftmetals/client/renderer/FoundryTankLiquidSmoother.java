package net.chriskatze.katzencraftmetals.client.renderer;

import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankNetwork;
import net.chriskatze.katzencraftmetals.metal.FoundryMetalLayer;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.DRAIN_ANIMATION_TICKS;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.LIQUID_EPSILON;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.RISE_ANIMATION_TICKS;

/**
 * Smooths rendered molten-metal amounts for a complete Tank network.
 *
 * Important:
 *
 * This class intentionally smooths whole-column metal boundaries first and
 * then slices those boundaries back into the requested Y-level.
 *
 * If each horizontal Y-level is smoothed independently, then after tanks are
 * removed and the available volume shrinks, bottom/middle/top layers all start
 * rising from their own local floor at the same time. That looks wrong.
 *
 * A Foundry Tank behaves like one connected liquid column, so visual changes
 * must rise from the lowest Tank level upward.
 */
final class FoundryTankLiquidSmoother {

    private final Map<LiquidColumnKey, LiquidColumnRenderState>
            liquidColumnRenderStates =
            new HashMap<>();

    /*
     * Multiple render paths ask for the same Tank's displayed amounts during
     * one frame:
     *
     * - the Tank itself
     * - the Tank above/below
     * - horizontal neighbor side comparisons
     *
     * This tiny per-frame cache avoids rebuilding the complete network
     * snapshot for the same Tank repeatedly during one render frame.
     */
    private final Map<BlockPos, Map<ResourceLocation, Float>>
            displayedAmountsFrameCache =
            new HashMap<>();

    /*
     * The important 4x4x4 optimization:
     *
     * Building a liquid column snapshot scans every Tank in the network and
     * aggregates all local metal layers. A full 4x4x4 foundry has up to 64
     * Tanks, and without this cache every visible Tank can trigger that same
     * 64-Tank scan again.
     *
     * This cache builds the aggregate liquid-column snapshot once per frame per
     * foundry network. Every Tank in that network then only slices the already
     * prepared displayed boundaries for its own Y level.
     */
    private final Map<LiquidColumnKey, LiquidColumnFrameSnapshot>
            liquidColumnFrameSnapshotCache =
            new HashMap<>();

    private Level displayedAmountsFrameCacheLevel;
    private long displayedAmountsFrameCacheGameTime =
            Long.MIN_VALUE;
    private int displayedAmountsFrameCachePartialBits;

    Map<ResourceLocation, Float> getDisplayedHorizontalLayerAmounts(
            FoundryTankBlockEntity tank,
            float partialTick
    ) {
        Level level =
                tank.getLevel();

        long gameTime =
                level.getGameTime();

        int partialBits =
                Float.floatToIntBits(
                        partialTick
                );

        if (
                displayedAmountsFrameCacheLevel != level
                        || displayedAmountsFrameCacheGameTime != gameTime
                        || displayedAmountsFrameCachePartialBits != partialBits
        ) {
            displayedAmountsFrameCache.clear();

            liquidColumnFrameSnapshotCache.clear();

            displayedAmountsFrameCacheLevel =
                    level;

            displayedAmountsFrameCacheGameTime =
                    gameTime;

            displayedAmountsFrameCachePartialBits =
                    partialBits;
        }

        BlockPos cacheKey =
                tank.getBlockPos()
                        .immutable();

        Map<ResourceLocation, Float> cachedAmounts =
                displayedAmountsFrameCache.get(
                        cacheKey
                );

        if (cachedAmounts != null) {
            return cachedAmounts;
        }

        LiquidColumnKey columnKey =
                createLiquidColumnKey(
                        tank
                );

        LiquidColumnFrameSnapshot frameSnapshot =
                liquidColumnFrameSnapshotCache.computeIfAbsent(
                        columnKey,
                        ignored -> createLiquidColumnFrameSnapshot(
                                tank,
                                columnKey
                        )
                );

        double currentRenderTime =
                gameTime
                        + partialTick;

        LiquidColumnRenderState renderState =
                liquidColumnRenderStates.computeIfAbsent(
                        frameSnapshot.key(),
                        ignored ->
                                new LiquidColumnRenderState(
                                        frameSnapshot.targetBoundaries(),
                                        currentRenderTime
                                )
                );

        Map<ResourceLocation, Float> displayedBoundaries =
                renderState.updateAndGet(
                        frameSnapshot.targetBoundaries(),
                        currentRenderTime
                );

        Map<ResourceLocation, Float> displayedAmounts =
                sliceDisplayedBoundariesForLayer(
                        frameSnapshot,
                        tank.getBlockPos()
                                .getY(),
                        displayedBoundaries
                );

        displayedAmountsFrameCache.put(
                cacheKey,
                displayedAmounts
        );

        return displayedAmounts;
    }

    private static LiquidColumnKey createLiquidColumnKey(
            FoundryTankBlockEntity tank
    ) {
        Level level =
                tank.getLevel();

        FoundryTankNetwork network =
                tank.getNetwork();

        if (network != null) {
            return new LiquidColumnKey(
                    level,
                    network.getOwnerId(),
                    BlockPos.ZERO
            );
        }

        return new LiquidColumnKey(
                level,
                null,
                tank.getBlockPos()
                        .immutable()
        );
    }

    private static LiquidColumnFrameSnapshot createLiquidColumnFrameSnapshot(
            FoundryTankBlockEntity tank,
            LiquidColumnKey key
    ) {
        Level level =
                tank.getLevel();

        FoundryTankNetwork network =
                tank.getNetwork();

        Set<BlockPos> networkPositions =
                network != null
                        ? network.getTankPositions()
                        : Set.of(
                        tank.getBlockPos()
                );

        List<BlockPos> sortedPositions =
                new ArrayList<>(
                        networkPositions
                );

        sortedPositions.sort(
                Comparator
                        .comparingInt(
                                (BlockPos position) ->
                                        position.getY()
                        )
                        .thenComparingInt(
                                position ->
                                        position.getX()
                        )
                        .thenComparingInt(
                                position ->
                                        position.getZ()
                        )
        );

        if (sortedPositions.isEmpty()) {
            sortedPositions =
                    List.of(
                            tank.getBlockPos()
                    );
        }

        Map<Integer, Integer> tankCountsByY =
                new LinkedHashMap<>();

        Map<ResourceLocation, Integer> aggregateAmounts =
                new LinkedHashMap<>();

        for (BlockPos tankPos : sortedPositions) {
            tankCountsByY.merge(
                    tankPos.getY(),
                    1,
                    Integer::sum
            );

            if (
                    level.getBlockEntity(
                            tankPos
                    )
                            instanceof FoundryTankBlockEntity layerTank
            ) {
                for (
                        FoundryMetalLayer layer :
                        layerTank.getLocalMetalLayers()
                ) {
                    if (
                            layer.amount() > 0
                                    && ModMoltenMetals.contains(
                                    layer.metal()
                            )
                    ) {
                        aggregateAmounts.merge(
                                layer.metal(),
                                layer.amount(),
                                Integer::sum
                        );
                    }
                }
            }
        }

        List<Integer> yLevels =
                new ArrayList<>(
                        tankCountsByY.keySet()
                );

        yLevels.sort(
                Integer::compareTo
        );

        Map<ResourceLocation, Float> targetBoundaries =
                createTargetBoundaries(
                        aggregateAmounts,
                        yLevels,
                        tankCountsByY
                );

        return new LiquidColumnFrameSnapshot(
                key,
                yLevels,
                Map.copyOf(
                        tankCountsByY
                ),
                Map.copyOf(
                        targetBoundaries
                )
        );
    }

    /**
     * Converts whole-network metal amounts into stacked liquid-column boundary
     * heights.
     *
     * Boundary coordinates are measured in Tank-level units:
     *
     * - 0.0 is the bottom of the lowest Tank level
     * - 1.0 is the top of the first Tank level
     * - 2.0 is the top of the second Tank level
     *
     * Variable horizontal footprints are respected by using each level's Tank
     * count as that level's cross-section.
     */
    private static Map<ResourceLocation, Float> createTargetBoundaries(
            Map<ResourceLocation, Integer> aggregateAmounts,
            List<Integer> yLevels,
            Map<Integer, Integer> tankCountsByY
    ) {
        if (
                aggregateAmounts.isEmpty()
                        || yLevels.isEmpty()
        ) {
            return Map.of();
        }

        float totalCapacity =
                0.0f;

        for (Integer y : yLevels) {
            totalCapacity +=
                    tankCountsByY.getOrDefault(
                            y,
                            0
                    ) * FoundryTankBlockEntity.CAPACITY;
        }

        float remainingCapacity =
                totalCapacity;

        float currentBoundary =
                0.0f;

        Map<ResourceLocation, Float> targetBoundaries =
                new LinkedHashMap<>();

        for (
                MoltenMetalDefinition definition :
                ModMoltenMetals.heaviestFirst()
        ) {
            int aggregateAmount =
                    aggregateAmounts.getOrDefault(
                            definition.id(),
                            0
                    );

            if (
                    aggregateAmount <= 0
                            || remainingCapacity <= LIQUID_EPSILON
            ) {
                continue;
            }

            float acceptedAmount =
                    Math.min(
                            remainingCapacity,
                            aggregateAmount
                    );

            currentBoundary =
                    advanceBoundary(
                            currentBoundary,
                            acceptedAmount,
                            yLevels,
                            tankCountsByY
                    );

            targetBoundaries.put(
                    definition.id(),
                    currentBoundary
            );

            remainingCapacity -=
                    acceptedAmount;
        }

        return targetBoundaries;
    }

    private static float advanceBoundary(
            float startBoundary,
            float amount,
            List<Integer> yLevels,
            Map<Integer, Integer> tankCountsByY
    ) {
        if (
                amount <= LIQUID_EPSILON
                        || yLevels.isEmpty()
        ) {
            return startBoundary;
        }

        float boundary =
                Mth.clamp(
                        startBoundary,
                        0.0f,
                        yLevels.size()
                );

        int levelIndex =
                Mth.clamp(
                        (int) Math.floor(
                                boundary
                        ),
                        0,
                        Math.max(
                                0,
                                yLevels.size() - 1
                        )
                );

        float localStart =
                Mth.clamp(
                        boundary - levelIndex,
                        0.0f,
                        1.0f
                );

        float remainingAmount =
                amount;

        while (levelIndex < yLevels.size()) {
            int y =
                    yLevels.get(
                            levelIndex
                    );

            int tankCount =
                    Math.max(
                            1,
                            tankCountsByY.getOrDefault(
                                    y,
                                    1
                            )
                    );

            float levelCapacity =
                    tankCount
                            * FoundryTankBlockEntity.CAPACITY;

            float availableInLevel =
                    levelCapacity
                            * (1.0f - localStart);

            if (
                    remainingAmount
                            <= availableInLevel + LIQUID_EPSILON
            ) {
                return levelIndex
                        + localStart
                        + remainingAmount / levelCapacity;
            }

            remainingAmount -=
                    availableInLevel;

            levelIndex++;
            localStart =
                    0.0f;
        }

        return yLevels.size();
    }

    private static Map<ResourceLocation, Float> sliceDisplayedBoundariesForLayer(
            LiquidColumnFrameSnapshot snapshot,
            int currentY,
            Map<ResourceLocation, Float> displayedBoundaries
    ) {
        int layerIndex =
                snapshot.yLevels()
                        .indexOf(
                                currentY
                        );

        if (layerIndex < 0) {
            return Map.of();
        }

        float layerMin =
                layerIndex;

        float layerMax =
                layerIndex + 1.0f;

        Map<ResourceLocation, Float> layerAmounts =
                new LinkedHashMap<>();

        float previousBoundary =
                0.0f;

        for (
                MoltenMetalDefinition definition :
                ModMoltenMetals.heaviestFirst()
        ) {
            ResourceLocation metal =
                    definition.id();

            if (!displayedBoundaries.containsKey(metal)) {
                continue;
            }

            float endBoundary =
                    Mth.clamp(
                            Math.max(
                                    previousBoundary,
                                    displayedBoundaries.getOrDefault(
                                            metal,
                                            previousBoundary
                                    )
                            ),
                            0.0f,
                            snapshot.yLevels()
                                    .size()
                    );

            float overlap =
                    Math.max(
                            0.0f,
                            Math.min(
                                    endBoundary,
                                    layerMax
                            ) - Math.max(
                                    previousBoundary,
                                    layerMin
                            )
                    );

            float amountInThisTank =
                    overlap
                            * FoundryTankBlockEntity.CAPACITY;

            if (amountInThisTank > LIQUID_EPSILON) {
                layerAmounts.put(
                        metal,
                        amountInThisTank
                );
            }

            previousBoundary =
                    endBoundary;
        }

        return Map.copyOf(
                layerAmounts
        );
    }

    static float sumDisplayedAmounts(
            Map<ResourceLocation, Float> amounts
    ) {
        float total =
                0.0f;

        for (Float amount : amounts.values()) {
            if (amount != null) {
                total +=
                        Math.max(
                                0.0f,
                                amount
                        );
            }
        }

        return total;
    }

    private record LiquidColumnFrameSnapshot(
            LiquidColumnKey key,
            List<Integer> yLevels,
            Map<Integer, Integer> tankCountsByY,
            Map<ResourceLocation, Float> targetBoundaries
    ) {
    }

    private record LiquidColumnKey(
            Level level,
            UUID ownerId,
            BlockPos fallbackAnchor
    ) {
    }

    private static final class LiquidColumnRenderState {

        private Map<ResourceLocation, Float> displayedBoundaries;
        private Map<ResourceLocation, Float> transitionStartBoundaries;
        private Map<ResourceLocation, Float> transitionTargetBoundaries;
        private Map<ResourceLocation, Float> lastTargetBoundaries;

        private double transitionStartTime;
        private float transitionDuration;

        private LiquidColumnRenderState(
                Map<ResourceLocation, Float> initialBoundaries,
                double currentRenderTime
        ) {
            Map<ResourceLocation, Float> normalizedInitial =
                    normalizeBoundaryMap(
                            initialBoundaries
                    );

            displayedBoundaries =
                    normalizedInitial;

            transitionStartBoundaries =
                    normalizedInitial;

            transitionTargetBoundaries =
                    normalizedInitial;

            lastTargetBoundaries =
                    normalizedInitial;

            transitionStartTime =
                    currentRenderTime;

            transitionDuration =
                    0.0f;
        }

        private Map<ResourceLocation, Float> updateAndGet(
                Map<ResourceLocation, Float> requestedTargets,
                double currentRenderTime
        ) {
            Map<ResourceLocation, Float> normalizedTargets =
                    normalizeBoundaryMap(
                            requestedTargets
                    );

            /*
             * Empty is a hard visual state. If the server says the network is
             * empty, drop all displayed/transition boundaries immediately.
             *
             * Without this, a tank that becomes empty for a very short time can
             * keep stale old displayed boundaries. When it receives the first
             * new unit of metal again, the smoother can briefly produce tiny
             * zero-thickness entries for several metals, which looks like the
             * liquid cycling through every molten color.
             */
            if (normalizedTargets.isEmpty()) {
                resetTo(
                        Map.of(),
                        currentRenderTime
                );

                return displayedBoundaries;
            }

            updateDisplayedBoundaries(
                    currentRenderTime
            );

            if (!sameBoundaryMap(
                    normalizedTargets,
                    lastTargetBoundaries
            )) {
                float oldTop =
                        topBoundary(
                                displayedBoundaries
                        );

                float newTop =
                        topBoundary(
                                normalizedTargets
                        );

                transitionStartBoundaries =
                        displayedBoundaries;

                transitionTargetBoundaries =
                        normalizedTargets;

                lastTargetBoundaries =
                        normalizedTargets;

                transitionStartTime =
                        currentRenderTime;

                transitionDuration =
                        newTop
                                >= oldTop
                                ? RISE_ANIMATION_TICKS
                                : DRAIN_ANIMATION_TICKS;
            }

            updateDisplayedBoundaries(
                    currentRenderTime
            );

            return displayedBoundaries;
        }

        private void resetTo(
                Map<ResourceLocation, Float> boundaries,
                double currentRenderTime
        ) {
            Map<ResourceLocation, Float> normalizedBoundaries =
                    normalizeBoundaryMap(
                            boundaries
                    );

            displayedBoundaries =
                    normalizedBoundaries;

            transitionStartBoundaries =
                    normalizedBoundaries;

            transitionTargetBoundaries =
                    normalizedBoundaries;

            lastTargetBoundaries =
                    normalizedBoundaries;

            transitionStartTime =
                    currentRenderTime;

            transitionDuration =
                    0.0f;
        }

        private void updateDisplayedBoundaries(
                double currentRenderTime
        ) {
            float progress =
                    transitionDuration <= 0.0f
                            ? 1.0f
                            : Mth.clamp(
                            (float) (
                                    (
                                            currentRenderTime
                                                    - transitionStartTime
                                    )
                                            / transitionDuration
                            ),
                            0.0f,
                            1.0f
                    );

            Map<ResourceLocation, Float> updated =
                    new LinkedHashMap<>();

            float previousBoundary =
                    0.0f;

            for (
                    MoltenMetalDefinition definition :
                    ModMoltenMetals.heaviestFirst()
            ) {
                ResourceLocation metal =
                        definition.id();

                float start =
                        transitionStartBoundaries.getOrDefault(
                                metal,
                                previousBoundary
                        );

                float target =
                        transitionTargetBoundaries.getOrDefault(
                                metal,
                                previousBoundary
                        );

                float displayed =
                        Mth.lerp(
                                progress,
                                start,
                                target
                        );

                displayed =
                        Math.max(
                                previousBoundary,
                                displayed
                        );

                /*
                 * Only write real layer thickness.
                 *
                 * The old code wrote any metal whose absolute boundary was
                 * above zero. That allowed zero-thickness trailing metals to
                 * leak into the displayed boundary map whenever a heavier metal
                 * already occupied space below them.
                 */
                if (displayed - previousBoundary > LIQUID_EPSILON) {
                    updated.put(
                            metal,
                            displayed
                    );
                }

                previousBoundary =
                        displayed;
            }

            displayedBoundaries =
                    Map.copyOf(
                            updated
                    );
        }

        private static Map<ResourceLocation, Float> normalizeBoundaryMap(
                Map<ResourceLocation, Float> source
        ) {
            Map<ResourceLocation, Float> normalized =
                    new LinkedHashMap<>();

            float previousBoundary =
                    0.0f;

            if (source != null) {
                for (
                        MoltenMetalDefinition definition :
                        ModMoltenMetals.heaviestFirst()
                ) {
                    ResourceLocation metal =
                            definition.id();

                    if (!source.containsKey(
                            metal
                    )) {
                        continue;
                    }

                    float boundary =
                            Math.max(
                                    previousBoundary,
                                    source.getOrDefault(
                                            metal,
                                            previousBoundary
                                    )
                            );

                    /*
                     * Only keep metals that actually add visible thickness.
                     * Do not duplicate the previous boundary for every lighter
                     * metal that is absent from the source map.
                     */
                    if (boundary - previousBoundary > LIQUID_EPSILON) {
                        normalized.put(
                                metal,
                                boundary
                        );
                    }

                    previousBoundary =
                            boundary;
                }
            }

            return Map.copyOf(
                    normalized
            );
        }

        private static boolean sameBoundaryMap(
                Map<ResourceLocation, Float> first,
                Map<ResourceLocation, Float> second
        ) {
            for (
                    MoltenMetalDefinition definition :
                    ModMoltenMetals.heaviestFirst()
            ) {
                float firstBoundary =
                        first.getOrDefault(
                                definition.id(),
                                0.0f
                        );

                float secondBoundary =
                        second.getOrDefault(
                                definition.id(),
                                0.0f
                        );

                if (
                        Math.abs(
                                firstBoundary
                                        - secondBoundary
                        ) > 0.00001f
                ) {
                    return false;
                }
            }

            return true;
        }

        private static float topBoundary(
                Map<ResourceLocation, Float> boundaries
        ) {
            float top =
                    0.0f;

            for (Float boundary : boundaries.values()) {
                if (boundary != null) {
                    top =
                            Math.max(
                                    top,
                                    boundary
                            );
                }
            }

            return top;
        }
    }
}
