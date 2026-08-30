package net.chriskatze.katzencraftmetals.client.renderer;

import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.LIQUID_EPSILON;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.LIQUID_INSET;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.LIQUID_MAX_INSET;

/**
 * Builds visible molten-metal layer slices for one rendered Tank.
 */
final class FoundryTankMoltenLayerBuilder {

    private final FoundryTankLiquidSmoother liquidSmoother;

    /*
     * The molten renderer can ask for the same Tank's rendered layers several
     * times in one frame:
     *
     * - once for the Tank currently being rendered
     * - again when a side-neighbor compares liquid intervals
     * - again when an above/below Tank is checked by another Tank render
     *
     * Building this list may call the liquid smoother for the Tank itself and
     * for its vertical neighbors, so caching the final rendered layer list is a
     * cheap and safe win.
     */
    private final Map<BlockPos, List<FoundryTankRenderedMetalLayer>>
            renderedLayersFrameCache =
            new HashMap<>();

    private Level renderedLayersFrameCacheLevel;
    private long renderedLayersFrameCacheGameTime =
            Long.MIN_VALUE;
    private int renderedLayersFrameCachePartialBits;

    FoundryTankMoltenLayerBuilder(
            FoundryTankLiquidSmoother liquidSmoother
    ) {
        this.liquidSmoother =
                liquidSmoother;
    }

    /**
     * Builds the visible metal layers from the aggregate contents of the
     * complete horizontal Tank level, not from this Tank's integer local share.
     *
     * Persistent storage still uses integer units per Tank. Rendering uses the
     * exact floating-point average across the level, so every connected Tank
     * has one perfectly level surface and identical metal boundaries.
     */
    List<FoundryTankRenderedMetalLayer> createRenderedLayers(
            FoundryTankBlockEntity tank,
            float partialTick
    ) {
        Level level =
                tank.getLevel();

        if (level == null) {
            return List.of();
        }

        long gameTime =
                level.getGameTime();

        int partialBits =
                Float.floatToIntBits(
                        partialTick
                );

        if (
                renderedLayersFrameCacheLevel != level
                        || renderedLayersFrameCacheGameTime != gameTime
                        || renderedLayersFrameCachePartialBits != partialBits
        ) {
            renderedLayersFrameCache.clear();

            renderedLayersFrameCacheLevel =
                    level;

            renderedLayersFrameCacheGameTime =
                    gameTime;

            renderedLayersFrameCachePartialBits =
                    partialBits;
        }

        BlockPos cacheKey =
                tank.getBlockPos()
                        .immutable();

        List<FoundryTankRenderedMetalLayer> cachedLayers =
                renderedLayersFrameCache.get(
                        cacheKey
                );

        if (cachedLayers != null) {
            return cachedLayers;
        }

        List<FoundryTankRenderedMetalLayer> renderedLayers =
                List.copyOf(
                        buildRenderedLayers(
                                tank,
                                partialTick
                        )
                );

        renderedLayersFrameCache.put(
                cacheKey,
                renderedLayers
        );

        return renderedLayers;
    }

    private List<FoundryTankRenderedMetalLayer> buildRenderedLayers(
            FoundryTankBlockEntity tank,
            float partialTick
    ) {
        Map<ResourceLocation, Float> displayedAmounts =
                liquidSmoother.getDisplayedHorizontalLayerAmounts(
                        tank,
                        partialTick
                );

        if (displayedAmounts.isEmpty()) {
            return List.of();
        }

        FoundryTankBlockEntity tankBelow =
                FoundryTankVisualConnections.getSameComponentNeighbor(
                        tank,
                        Direction.DOWN
                );

        FoundryTankBlockEntity tankAbove =
                FoundryTankVisualConnections.getSameComponentNeighbor(
                        tank,
                        Direction.UP
                );

        Map<ResourceLocation, Float> belowAmounts =
                tankBelow != null
                        ? liquidSmoother.getDisplayedHorizontalLayerAmounts(
                        tankBelow,
                        partialTick
                )
                        : Map.of();

        Map<ResourceLocation, Float> aboveAmounts =
                tankAbove != null
                        ? liquidSmoother.getDisplayedHorizontalLayerAmounts(
                        tankAbove,
                        partialTick
                )
                        : Map.of();

        boolean anyLiquidBelow =
                FoundryTankLiquidSmoother.sumDisplayedAmounts(
                        belowAmounts
                ) > LIQUID_EPSILON;

        float cumulativeAmount =
                0.0f;

        List<FoundryTankRenderedMetalLayer> result =
                new ArrayList<>();

        List<MoltenMetalDefinition> orderedDefinitions =
                ModMoltenMetals.heaviestFirst();

        for (int index = 0; index < orderedDefinitions.size(); index++) {
            MoltenMetalDefinition definition =
                    orderedDefinitions.get(index);

            float layerAmount =
                    displayedAmounts.getOrDefault(
                            definition.id(),
                            0.0f
                    );

            if (layerAmount <= LIQUID_EPSILON) {
                continue;
            }

            float startAmount =
                    cumulativeAmount;

            cumulativeAmount =
                    Math.min(
                            FoundryTankBlockEntity.CAPACITY,
                            cumulativeAmount
                                    + layerAmount
                    );

            float endAmount =
                    cumulativeAmount;

            float minY =
                    startAmount <= LIQUID_EPSILON
                            && anyLiquidBelow
                            ? 0.0f
                            : Mth.lerp(
                            Mth.clamp(
                                    startAmount
                                            / FoundryTankBlockEntity.CAPACITY,
                                    0.0f,
                                    1.0f
                            ),
                            LIQUID_INSET,
                            LIQUID_MAX_INSET
                    );

            boolean continuesAbove =
                    endAmount
                            >= FoundryTankBlockEntity.CAPACITY
                            - LIQUID_EPSILON
                            && FoundryTankLiquidSmoother.sumDisplayedAmounts(
                            aboveAmounts
                    ) > LIQUID_EPSILON;

            float maxY =
                    continuesAbove
                            ? 1.0f
                            : Mth.lerp(
                            Mth.clamp(
                                    endAmount
                                            / FoundryTankBlockEntity.CAPACITY,
                                    0.0f,
                                    1.0f
                            ),
                            LIQUID_INSET,
                            LIQUID_MAX_INSET
                    );

            ResourceLocation nextMetal =
                    getNextDisplayedMetal(
                            displayedAmounts,
                            index + 1
                    );

            boolean renderTop;

            if (nextMetal != null) {
                renderTop =
                        !nextMetal.equals(
                                definition.id()
                        );
            } else if (continuesAbove) {
                ResourceLocation metalAbove =
                        getBottomDisplayedMetal(
                                aboveAmounts
                        );

                renderTop =
                        metalAbove == null
                                || !metalAbove.equals(
                                definition.id()
                        );
            } else {
                renderTop =
                        true;
            }

            boolean renderBottom =
                    result.isEmpty()
                            && !anyLiquidBelow;

            if (maxY - minY > LIQUID_EPSILON) {
                result.add(
                        new FoundryTankRenderedMetalLayer(
                                definition.id(),
                                minY,
                                maxY,
                                renderTop,
                                renderBottom
                        )
                );
            }
        }

        return result;
    }

    private static ResourceLocation getNextDisplayedMetal(
            Map<ResourceLocation, Float> displayedAmounts,
            int definitionStartIndex
    ) {
        List<MoltenMetalDefinition> orderedDefinitions =
                ModMoltenMetals.heaviestFirst();

        for (
                int index = definitionStartIndex;
                index < orderedDefinitions.size();
                index++
        ) {
            MoltenMetalDefinition definition =
                    orderedDefinitions.get(index);

            if (
                    displayedAmounts.getOrDefault(
                            definition.id(),
                            0.0f
                    ) > LIQUID_EPSILON
            ) {
                return definition.id();
            }
        }

        return null;
    }

    private static ResourceLocation getBottomDisplayedMetal(
            Map<ResourceLocation, Float> displayedAmounts
    ) {
        for (
                MoltenMetalDefinition definition :
                ModMoltenMetals.heaviestFirst()
        ) {
            if (
                    displayedAmounts.getOrDefault(
                            definition.id(),
                            0.0f
                    ) > LIQUID_EPSILON
            ) {
                return definition.id();
            }
        }

        return null;
    }
}
