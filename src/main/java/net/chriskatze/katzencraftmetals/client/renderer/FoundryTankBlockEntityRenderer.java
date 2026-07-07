package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.block.entity.FoundryFaucetBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Dynamically renders the complete visual Foundry Tank multiblock.
 *
 * The JSON model remains available for the inventory item and particles,
 * while placed Tanks use this renderer for:
 *
 * - connected exterior frame textures
 * - hidden internal Tank faces
 * - seamless molten-metal volumes
 * - Faucet entrance shadow overlays
 */
public class FoundryTankBlockEntityRenderer
        implements BlockEntityRenderer<FoundryTankBlockEntity> {

    private static final float LIQUID_INSET =
            0.12f / 16.0f;

    private static final float LIQUID_MAX_INSET =
            15.88f / 16.0f;

    private static final float LIQUID_EPSILON =
            0.0001f;

    private static final float RISE_ANIMATION_TICKS =
            8.0f;

    private static final float DRAIN_ANIMATION_TICKS =
            FoundryFaucetBlockEntity.TRANSFER_INTERVAL;

    private final Map<FoundryTankBlockEntity, FoundryTankRenderState> renderStates =
            new WeakHashMap<>();

    private final Map<FoundryTankHorizontalLayerKey, FoundryTankMultiMetalLayerRenderState>
            multiMetalLayerRenderStates =
            new HashMap<>();

    public FoundryTankBlockEntityRenderer(
            BlockEntityRendererProvider.Context context
    ) {
    }

    @Override
    public void render(
            FoundryTankBlockEntity tank,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (tank.getLevel() == null) {
            return;
        }

        poseStack.pushPose();

        PoseStack.Pose pose =
                poseStack.last();

        FoundryTankCasingRenderer.renderTankCasing(
                tank,
                pose,
                bufferSource,
                packedLight,
                packedOverlay
        );

        renderMoltenLayers(
                tank,
                partialTick,
                pose,
                bufferSource,
                packedOverlay
        );

        /*
         * Render translucent entrance shadows after the molten metal so the
         * shadow blends over the visible liquid instead of hiding it through
         * the depth buffer.
         */
        FoundryTankCasingRenderer.renderAttachedFaucetOverlays(
                tank,
                partialTick,
                pose,
                bufferSource,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }

// =========================
    // SEAMLESS MOLTEN METAL
    // =========================

    private void renderMoltenLayers(
            FoundryTankBlockEntity tank,
            float partialTick,
            PoseStack.Pose pose,
            MultiBufferSource bufferSource,
            int packedOverlay
    ) {
        List<FoundryTankRenderedMetalLayer> renderedLayers =
                createRenderedLayers(
                        tank,
                        partialTick
                );

        if (renderedLayers.isEmpty()) {
            return;
        }

        MoltenIronAnimation.Frame animationFrame =
                MoltenIronAnimation.getFrame(
                        tank.getLevel().getGameTime()
                );

        float frameMinV =
                animationFrame.minV();

        float frameMaxV =
                animationFrame.maxV();

        float minX =
                FoundryTankVisualConnections.isSameComponent(
                        tank,
                        Direction.WEST
                )
                        ? 0.0f
                        : LIQUID_INSET;

        float maxX =
                FoundryTankVisualConnections.isSameComponent(
                        tank,
                        Direction.EAST
                )
                        ? 1.0f
                        : LIQUID_MAX_INSET;

        float minZ =
                FoundryTankVisualConnections.isSameComponent(
                        tank,
                        Direction.NORTH
                )
                        ? 0.0f
                        : LIQUID_INSET;

        float maxZ =
                FoundryTankVisualConnections.isSameComponent(
                        tank,
                        Direction.SOUTH
                )
                        ? 1.0f
                        : LIQUID_MAX_INSET;

        for (FoundryTankRenderedMetalLayer renderedLayer : renderedLayers) {
            MoltenMetalDefinition definition =
                    ModMoltenMetals.get(
                                    renderedLayer.metal()
                            )
                            .orElse(null);

            if (definition == null) {
                continue;
            }

            VertexConsumer consumer =
                    bufferSource.getBuffer(
                            RenderType.entityTranslucent(
                                    definition.animatedTexture()
                            )
                    );

            FoundryTankLiquidGeometry geometry =
                    new FoundryTankLiquidGeometry(
                            minX,
                            maxX,
                            renderedLayer.minY(),
                            renderedLayer.maxY(),
                            minZ,
                            maxZ,
                            renderedLayer.renderTop(),
                            renderedLayer.renderBottom()
                    );

            if (renderedLayer.renderTop()) {
                renderLiquidHorizontalFace(
                        Direction.UP,
                        consumer,
                        pose,
                        minX,
                        maxX,
                        renderedLayer.maxY(),
                        minZ,
                        maxZ,
                        packedOverlay,
                        frameMinV,
                        frameMaxV
                );
            }

            if (renderedLayer.renderBottom()) {
                renderLiquidHorizontalFace(
                        Direction.DOWN,
                        consumer,
                        pose,
                        minX,
                        maxX,
                        renderedLayer.minY(),
                        minZ,
                        maxZ,
                        packedOverlay,
                        frameMinV,
                        frameMaxV
                );
            }

            for (Direction side : Direction.Plane.HORIZONTAL) {
                renderLayerSide(
                        tank,
                        side,
                        renderedLayer,
                        geometry,
                        partialTick,
                        consumer,
                        pose,
                        packedOverlay,
                        frameMinV,
                        frameMaxV
                );
            }
        }
    }

    private void renderLayerSide(
            FoundryTankBlockEntity tank,
            Direction side,
            FoundryTankRenderedMetalLayer renderedLayer,
            FoundryTankLiquidGeometry geometry,
            float partialTick,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedOverlay,
            float frameMinV,
            float frameMaxV
    ) {
        FoundryTankBlockEntity neighbor =
                FoundryTankVisualConnections.getSameComponentNeighbor(
                        tank,
                        side
                );

        if (neighbor == null) {
            renderLiquidSideSegment(
                    side,
                    consumer,
                    pose,
                    geometry,
                    renderedLayer.minY(),
                    renderedLayer.maxY(),
                    false,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );

            return;
        }

        List<FoundryTankVerticalInterval> visibleIntervals =
                new ArrayList<>();

        visibleIntervals.add(
                new FoundryTankVerticalInterval(
                        renderedLayer.minY(),
                        renderedLayer.maxY()
                )
        );

        for (
                FoundryTankRenderedMetalLayer neighborLayer :
                createRenderedLayers(
                        neighbor,
                        partialTick
                )
        ) {
            /*
             * All Tanks in one horizontal level now use the same averaged
             * visual layer boundaries. Internal faces therefore disappear
             * cleanly without one Tank appearing one integer unit higher.
             */
            visibleIntervals =
                    subtractInterval(
                            visibleIntervals,
                            new FoundryTankVerticalInterval(
                                    neighborLayer.minY(),
                                    neighborLayer.maxY()
                            )
                    );

            if (visibleIntervals.isEmpty()) {
                return;
            }
        }

        for (FoundryTankVerticalInterval visible : visibleIntervals) {
            renderLiquidSideSegment(
                    side,
                    consumer,
                    pose,
                    geometry,
                    visible.minY(),
                    visible.maxY(),
                    true,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
        }
    }

    private static List<FoundryTankVerticalInterval> subtractInterval(
            List<FoundryTankVerticalInterval> source,
            FoundryTankVerticalInterval removed
    ) {
        List<FoundryTankVerticalInterval> result =
                new ArrayList<>();

        for (FoundryTankVerticalInterval interval : source) {
            float overlapMin =
                    Math.max(
                            interval.minY(),
                            removed.minY()
                    );

            float overlapMax =
                    Math.min(
                            interval.maxY(),
                            removed.maxY()
                    );

            if (overlapMax - overlapMin <= LIQUID_EPSILON) {
                result.add(interval);
                continue;
            }

            if (overlapMin - interval.minY() > LIQUID_EPSILON) {
                result.add(
                        new FoundryTankVerticalInterval(
                                interval.minY(),
                                overlapMin
                        )
                );
            }

            if (interval.maxY() - overlapMax > LIQUID_EPSILON) {
                result.add(
                        new FoundryTankVerticalInterval(
                                overlapMax,
                                interval.maxY()
                        )
                );
            }
        }

        return result;
    }

    /**
     * Builds the visible metal layers from the aggregate contents of the
     * complete horizontal Tank level, not from this Tank's integer local share.
     *
     * Persistent storage still uses integer units per Tank. Rendering uses the
     * exact floating-point average across the level, so every connected Tank
     * has one perfectly level surface and identical metal boundaries.
     */
    private List<FoundryTankRenderedMetalLayer> createRenderedLayers(
            FoundryTankBlockEntity tank,
            float partialTick
    ) {
        Map<ResourceLocation, Float> displayedAmounts =
                getDisplayedHorizontalLayerAmounts(
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
                        ? getDisplayedHorizontalLayerAmounts(
                        tankBelow,
                        partialTick
                )
                        : Map.of();

        Map<ResourceLocation, Float> aboveAmounts =
                tankAbove != null
                        ? getDisplayedHorizontalLayerAmounts(
                        tankAbove,
                        partialTick
                )
                        : Map.of();

        boolean anyLiquidBelow =
                sumDisplayedAmounts(
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
                            && sumDisplayedAmounts(
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

    private Map<ResourceLocation, Float> getDisplayedHorizontalLayerAmounts(
            FoundryTankBlockEntity tank,
            float partialTick
    ) {
        FoundryTankHorizontalLayerSnapshot snapshot =
                createHorizontalLayerSnapshot(
                        tank
                );

        double currentRenderTime =
                tank.getLevel().getGameTime()
                        + partialTick;

        FoundryTankMultiMetalLayerRenderState renderState =
                multiMetalLayerRenderStates.computeIfAbsent(
                        snapshot.key(),
                        ignored ->
                                new FoundryTankMultiMetalLayerRenderState(
                                        snapshot.targetAmounts(),
                                        currentRenderTime
                                )
                );

        return renderState.updateAndGet(
                snapshot.targetAmounts(),
                currentRenderTime
        );
    }

private static FoundryTankHorizontalLayerSnapshot createHorizontalLayerSnapshot(
            FoundryTankBlockEntity tank
    ) {
        return FoundryTankLayerSnapshotBuilder.create(
                tank
        );
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

    private static float sumDisplayedAmounts(
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

    private void renderMoltenMetal(
            FoundryTankBlockEntity tank,
            float displayedMoltenAmount,
            float partialTick,
            PoseStack.Pose pose,
            MultiBufferSource bufferSource,
            int packedOverlay
    ) {
        MoltenMetalDefinition metal =
                ModMoltenMetals.get(
                                tank.getStoredMetal()
                        )
                        .orElse(null);

        if (metal == null) {
            return;
        }

        FoundryTankLiquidGeometry geometry =
                createLiquidGeometry(
                        tank,
                        displayedMoltenAmount,
                        partialTick
                );

        MoltenIronAnimation.Frame animationFrame =
                MoltenIronAnimation.getFrame(
                        tank.getLevel().getGameTime()
                );

        VertexConsumer consumer =
                bufferSource.getBuffer(
                        RenderType.entityTranslucent(
                                metal.animatedTexture()
                        )
                );

        float frameMinV =
                animationFrame.minV();

        float frameMaxV =
                animationFrame.maxV();

        if (geometry.renderTop()) {
            renderLiquidHorizontalFace(
                    Direction.UP,
                    consumer,
                    pose,
                    geometry.minX(),
                    geometry.maxX(),
                    geometry.surfaceY(),
                    geometry.minZ(),
                    geometry.maxZ(),
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
        }

        if (geometry.renderBottom()) {
            renderLiquidHorizontalFace(
                    Direction.DOWN,
                    consumer,
                    pose,
                    geometry.minX(),
                    geometry.maxX(),
                    geometry.minY(),
                    geometry.minZ(),
                    geometry.maxZ(),
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
        }

        for (Direction side : Direction.Plane.HORIZONTAL) {
            renderLiquidSide(
                    tank,
                    side,
                    geometry,
                    partialTick,
                    consumer,
                    pose,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
        }
    }

    private FoundryTankLiquidGeometry createLiquidGeometry(
            FoundryTankBlockEntity tank,
            float displayedMoltenAmount,
            float partialTick
    ) {
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

        float belowAmount =
                tankBelow != null
                        ? getDisplayedMoltenAmount(
                        tankBelow,
                        partialTick
                )
                        : 0.0f;

        float aboveAmount =
                tankAbove != null
                        ? getDisplayedMoltenAmount(
                        tankAbove,
                        partialTick
                )
                        : 0.0f;

        boolean liquidContinuesBelow =
                tankBelow != null
                        && belowAmount > LIQUID_EPSILON;

        boolean liquidContinuesAbove =
                tankAbove != null
                        && aboveAmount > LIQUID_EPSILON;

        float minX =
                FoundryTankVisualConnections.isSameComponent(
                        tank,
                        Direction.WEST
                )
                        ? 0.0f
                        : LIQUID_INSET;

        float maxX =
                FoundryTankVisualConnections.isSameComponent(
                        tank,
                        Direction.EAST
                )
                        ? 1.0f
                        : LIQUID_MAX_INSET;

        float minZ =
                FoundryTankVisualConnections.isSameComponent(
                        tank,
                        Direction.NORTH
                )
                        ? 0.0f
                        : LIQUID_INSET;

        float maxZ =
                FoundryTankVisualConnections.isSameComponent(
                        tank,
                        Direction.SOUTH
                )
                        ? 1.0f
                        : LIQUID_MAX_INSET;

        float minY =
                liquidContinuesBelow
                        ? 0.0f
                        : LIQUID_INSET;

        float fillPercentage =
                Mth.clamp(
                        displayedMoltenAmount
                                / FoundryTankBlockEntity.CAPACITY,
                        0.0f,
                        1.0f
                );

        float surfaceY =
                liquidContinuesAbove
                        ? 1.0f
                        : Mth.lerp(
                        fillPercentage,
                        minY,
                        LIQUID_MAX_INSET
                );

        return new FoundryTankLiquidGeometry(
                minX,
                maxX,
                minY,
                surfaceY,
                minZ,
                maxZ,
                !liquidContinuesAbove,
                !liquidContinuesBelow
        );
    }

    private void renderLiquidSide(
            FoundryTankBlockEntity tank,
            Direction side,
            FoundryTankLiquidGeometry geometry,
            float partialTick,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedOverlay,
            float frameMinV,
            float frameMaxV
    ) {
        FoundryTankBlockEntity neighbor =
                FoundryTankVisualConnections.getSameComponentNeighbor(
                        tank,
                        side
                );

        if (neighbor == null) {
            renderLiquidSideSegment(
                    side,
                    consumer,
                    pose,
                    geometry,
                    geometry.minY(),
                    geometry.surfaceY(),
                    false,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );

            return;
        }

        float neighborAmount =
                getDisplayedMoltenAmount(
                        neighbor,
                        partialTick
                );

        if (neighborAmount <= LIQUID_EPSILON) {
            renderLiquidSideSegment(
                    side,
                    consumer,
                    pose,
                    geometry,
                    geometry.minY(),
                    geometry.surfaceY(),
                    true,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );

            return;
        }

        FoundryTankLiquidGeometry neighborGeometry =
                createLiquidGeometry(
                        neighbor,
                        neighborAmount,
                        partialTick
                );

        float currentMin =
                geometry.minY();

        float currentMax =
                geometry.surfaceY();

        float neighborMin =
                neighborGeometry.minY();

        float neighborMax =
                neighborGeometry.surfaceY();

        float lowerSegmentMax =
                Math.min(
                        currentMax,
                        neighborMin
                );

        if (lowerSegmentMax - currentMin > LIQUID_EPSILON) {
            renderLiquidSideSegment(
                    side,
                    consumer,
                    pose,
                    geometry,
                    currentMin,
                    lowerSegmentMax,
                    true,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
        }

        float upperSegmentMin =
                Math.max(
                        currentMin,
                        neighborMax
                );

        if (currentMax - upperSegmentMin > LIQUID_EPSILON) {
            renderLiquidSideSegment(
                    side,
                    consumer,
                    pose,
                    geometry,
                    upperSegmentMin,
                    currentMax,
                    true,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
        }
    }

    private static void renderLiquidSideSegment(
            Direction side,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            FoundryTankLiquidGeometry geometry,
            float minY,
            float maxY,
            boolean sharedBoundary,
            int packedOverlay,
            float frameMinV,
            float frameMaxV
    ) {
        if (maxY - minY <= LIQUID_EPSILON) {
            return;
        }

        float coordinate =
                switch (side) {
                    case NORTH -> sharedBoundary
                            ? 0.0f
                            : geometry.minZ();
                    case SOUTH -> sharedBoundary
                            ? 1.0f
                            : geometry.maxZ();
                    case WEST -> sharedBoundary
                            ? 0.0f
                            : geometry.minX();
                    case EAST -> sharedBoundary
                            ? 1.0f
                            : geometry.maxX();
                    default -> throw new IllegalArgumentException(
                            "Liquid side must be horizontal."
                    );
                };

        switch (side) {
            case NORTH -> renderLiquidQuad(
                    consumer,
                    pose,
                    geometry.minX(),
                    minY,
                    coordinate,
                    geometry.maxX(),
                    minY,
                    coordinate,
                    geometry.maxX(),
                    maxY,
                    coordinate,
                    geometry.minX(),
                    maxY,
                    coordinate,
                    geometry.minX(),
                    1.0f - minY,
                    geometry.maxX(),
                    1.0f - minY,
                    geometry.maxX(),
                    1.0f - maxY,
                    geometry.minX(),
                    1.0f - maxY,
                    0.0f,
                    0.0f,
                    -1.0f,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
            case SOUTH -> renderLiquidQuad(
                    consumer,
                    pose,
                    geometry.maxX(),
                    minY,
                    coordinate,
                    geometry.minX(),
                    minY,
                    coordinate,
                    geometry.minX(),
                    maxY,
                    coordinate,
                    geometry.maxX(),
                    maxY,
                    coordinate,
                    1.0f - geometry.maxX(),
                    1.0f - minY,
                    1.0f - geometry.minX(),
                    1.0f - minY,
                    1.0f - geometry.minX(),
                    1.0f - maxY,
                    1.0f - geometry.maxX(),
                    1.0f - maxY,
                    0.0f,
                    0.0f,
                    1.0f,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
            case WEST -> renderLiquidQuad(
                    consumer,
                    pose,
                    coordinate,
                    minY,
                    geometry.maxZ(),
                    coordinate,
                    minY,
                    geometry.minZ(),
                    coordinate,
                    maxY,
                    geometry.minZ(),
                    coordinate,
                    maxY,
                    geometry.maxZ(),
                    1.0f - geometry.maxZ(),
                    1.0f - minY,
                    1.0f - geometry.minZ(),
                    1.0f - minY,
                    1.0f - geometry.minZ(),
                    1.0f - maxY,
                    1.0f - geometry.maxZ(),
                    1.0f - maxY,
                    -1.0f,
                    0.0f,
                    0.0f,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
            case EAST -> renderLiquidQuad(
                    consumer,
                    pose,
                    coordinate,
                    minY,
                    geometry.minZ(),
                    coordinate,
                    minY,
                    geometry.maxZ(),
                    coordinate,
                    maxY,
                    geometry.maxZ(),
                    coordinate,
                    maxY,
                    geometry.minZ(),
                    geometry.minZ(),
                    1.0f - minY,
                    geometry.maxZ(),
                    1.0f - minY,
                    geometry.maxZ(),
                    1.0f - maxY,
                    geometry.minZ(),
                    1.0f - maxY,
                    1.0f,
                    0.0f,
                    0.0f,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
            default -> {
            }
        }
    }

    private static void renderLiquidHorizontalFace(
            Direction face,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float minX,
            float maxX,
            float y,
            float minZ,
            float maxZ,
            int packedOverlay,
            float frameMinV,
            float frameMaxV
    ) {
        if (face == Direction.UP) {
            renderLiquidQuad(
                    consumer,
                    pose,
                    minX,
                    y,
                    minZ,
                    minX,
                    y,
                    maxZ,
                    maxX,
                    y,
                    maxZ,
                    maxX,
                    y,
                    minZ,
                    minX,
                    minZ,
                    minX,
                    maxZ,
                    maxX,
                    maxZ,
                    maxX,
                    minZ,
                    0.0f,
                    1.0f,
                    0.0f,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
        } else {
            renderLiquidQuad(
                    consumer,
                    pose,
                    minX,
                    y,
                    maxZ,
                    minX,
                    y,
                    minZ,
                    maxX,
                    y,
                    minZ,
                    maxX,
                    y,
                    maxZ,
                    minX,
                    maxZ,
                    minX,
                    minZ,
                    maxX,
                    minZ,
                    maxX,
                    maxZ,
                    0.0f,
                    -1.0f,
                    0.0f,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
        }
    }

    // =========================
    // SMOOTH LOCAL FILL
    // =========================

    private float getDisplayedMoltenAmount(
            FoundryTankBlockEntity tank,
            float partialTick
    ) {
        double currentRenderTime =
                tank.getLevel().getGameTime()
                        + partialTick;

        float targetAmount =
                tank.getLocalVisualMoltenAmount();

        FoundryTankRenderState renderState =
                renderStates.computeIfAbsent(
                        tank,
                        ignored -> new FoundryTankRenderState(
                                targetAmount,
                                currentRenderTime
                        )
                );

        if (
                Math.abs(
                        targetAmount
                                - renderState.lastTargetAmount
                ) > 0.00001f
        ) {
            renderState.transitionStartAmount =
                    renderState.displayedAmount;

            renderState.transitionTargetAmount =
                    targetAmount;

            renderState.transitionStartTime =
                    currentRenderTime;

            renderState.transitionDuration =
                    targetAmount
                            > renderState.lastTargetAmount
                            ? RISE_ANIMATION_TICKS
                            : DRAIN_ANIMATION_TICKS;

            renderState.lastTargetAmount =
                    targetAmount;
        }

        float progress =
                renderState.transitionDuration <= 0.0f
                        ? 1.0f
                        : Mth.clamp(
                        (float) (
                                (
                                        currentRenderTime
                                                - renderState.transitionStartTime
                                )
                                        / renderState.transitionDuration
                        ),
                        0.0f,
                        1.0f
                );

        renderState.displayedAmount =
                Mth.lerp(
                        progress,
                        renderState.transitionStartAmount,
                        renderState.transitionTargetAmount
                );

        if (progress >= 1.0f) {
            renderState.displayedAmount =
                    renderState.transitionTargetAmount;
        }

        return renderState.displayedAmount;
    }

// =========================
    // LIQUID QUAD HELPERS
    // =========================

    private static void renderLiquidQuad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float x4,
            float y4,
            float z4,
            float u1,
            float localV1,
            float u2,
            float localV2,
            float u3,
            float localV3,
            float u4,
            float localV4,
            float normalX,
            float normalY,
            float normalZ,
            int packedOverlay,
            float frameMinV,
            float frameMaxV
    ) {
        addLiquidVertex(
                consumer,
                pose,
                x1,
                y1,
                z1,
                u1,
                localV1,
                normalX,
                normalY,
                normalZ,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        addLiquidVertex(
                consumer,
                pose,
                x2,
                y2,
                z2,
                u2,
                localV2,
                normalX,
                normalY,
                normalZ,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        addLiquidVertex(
                consumer,
                pose,
                x3,
                y3,
                z3,
                u3,
                localV3,
                normalX,
                normalY,
                normalZ,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        addLiquidVertex(
                consumer,
                pose,
                x4,
                y4,
                z4,
                u4,
                localV4,
                normalX,
                normalY,
                normalZ,
                packedOverlay,
                frameMinV,
                frameMaxV
        );
    }

    private static void addLiquidVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float localV,
            float normalX,
            float normalY,
            float normalZ,
            int packedOverlay,
            float frameMinV,
            float frameMaxV
    ) {
        float textureV =
                Mth.lerp(
                        localV,
                        frameMinV,
                        frameMaxV
                );

        consumer.addVertex(
                        pose.pose(),
                        x,
                        y,
                        z
                )
                .setColor(0xFFFFFFFF)
                .setUv(
                        u,
                        textureV
                )
                .setOverlay(packedOverlay)
                .setLight(
                        LightTexture.FULL_BRIGHT
                )
                .setNormal(
                        pose,
                        normalX,
                        normalY,
                        normalZ
                );
    }








    @Override
    public boolean shouldRenderOffScreen(
            FoundryTankBlockEntity tank
    ) {
        return true;
    }
}
