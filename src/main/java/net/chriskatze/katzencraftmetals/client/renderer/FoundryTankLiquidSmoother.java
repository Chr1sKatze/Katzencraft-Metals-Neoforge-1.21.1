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

    Map<ResourceLocation, Float> getDisplayedHorizontalLayerAmounts(
            FoundryTankBlockEntity tank,
            float partialTick
    ) {
        LiquidColumnSnapshot snapshot =
                createLiquidColumnSnapshot(
                        tank
                );

        double currentRenderTime =
                tank.getLevel().getGameTime()
                        + partialTick;

        LiquidColumnRenderState renderState =
                liquidColumnRenderStates.computeIfAbsent(
                        snapshot.key(),
                        ignored ->
                                new LiquidColumnRenderState(
                                        snapshot.targetBoundaries(),
                                        currentRenderTime
                                )
                );

        Map<ResourceLocation, Float> displayedBoundaries =
                renderState.updateAndGet(
                        snapshot.targetBoundaries(),
                        currentRenderTime
                );

        return sliceDisplayedBoundariesForLayer(
                snapshot,
                displayedBoundaries
        );
    }

    private static LiquidColumnSnapshot createLiquidColumnSnapshot(
            FoundryTankBlockEntity tank
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

        BlockPos anchor =
                sortedPositions.getFirst()
                        .immutable();

        UUID ownerId =
                network != null
                        ? network.getOwnerId()
                        : null;

        /*
         * Owned networks keep the same key while their shape changes.
         * Unowned/orphan Tank groups fall back to their physical anchor.
         */
        LiquidColumnKey key =
                new LiquidColumnKey(
                        level,
                        ownerId,
                        ownerId == null
                                ? anchor
                                : BlockPos.ZERO
                );

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

        return new LiquidColumnSnapshot(
                key,
                yLevels,
                Map.copyOf(
                        tankCountsByY
                ),
                tank.getBlockPos()
                        .getY(),
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
            LiquidColumnSnapshot snapshot,
            Map<ResourceLocation, Float> displayedBoundaries
    ) {
        int layerIndex =
                snapshot.yLevels()
                        .indexOf(
                                snapshot.currentY()
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

    private record LiquidColumnSnapshot(
            LiquidColumnKey key,
            List<Integer> yLevels,
            Map<Integer, Integer> tankCountsByY,
            int currentY,
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

                if (displayed > LIQUID_EPSILON) {
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
                    float boundary =
                            Math.max(
                                    previousBoundary,
                                    source.getOrDefault(
                                            definition.id(),
                                            previousBoundary
                                    )
                            );

                    if (boundary > LIQUID_EPSILON) {
                        normalized.put(
                                definition.id(),
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
