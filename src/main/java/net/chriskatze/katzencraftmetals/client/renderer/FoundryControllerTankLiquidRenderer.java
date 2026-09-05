package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.block.entity.FoundryControllerBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankNetwork;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.LIQUID_EPSILON;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.LIQUID_INSET;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.LIQUID_MAX_INSET;

/**
 * Renders all molten Tank volume once from the Controller.
 *
 * The renderer consumes only the Controller's cached structure and synchronized
 * pooled metal amounts. It never discovers a Tank network while rendering.
 */
final class FoundryControllerTankLiquidRenderer {

    private final FoundryControllerLiquidSmoother smoother =
            new FoundryControllerLiquidSmoother();

    private final FoundryControllerSurfaceEffectsRenderer surfaceEffects =
            new FoundryControllerSurfaceEffectsRenderer();

    void render(
            FoundryControllerBlockEntity controller,
            FoundryTankNetwork network,
            Set<BlockPos> structure,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedOverlay
    ) {
        Level level = controller.getLevel();

        if (
                level == null
                        || network == null
                        || structure == null
                        || structure.isEmpty()
        ) {
            return;
        }

        Map<ResourceLocation, Float> displayedGlobalAmounts =
                smoother.getDisplayedAmounts(
                        controller,
                        network,
                        partialTick
                );

        if (displayedGlobalAmounts.isEmpty()) {
            return;
        }

        Map<Integer, Map<ResourceLocation, Float>> amountsByY =
                distributeAcrossHorizontalLevels(
                        structure,
                        displayedGlobalAmounts
                );

        Map<BlockPos, List<FoundryTankRenderedMetalLayer>> renderedLayers =
                new LinkedHashMap<>();

        for (BlockPos tankPos : structure) {
            renderedLayers.put(
                    tankPos,
                    createRenderedLayers(
                            tankPos,
                            structure,
                            amountsByY
                    )
            );
        }

        BlockPos controllerPos = controller.getBlockPos();

        /*
         * Give the complete Foundry one stable molten-animation phase derived
         * from its Controller position.
         *
         * Every Tank cell and every metal layer in this Foundry receives the
         * exact same frame, so the connected liquid remains visually seamless.
         * A different Foundry normally has a different Controller position and
         * therefore a different phase, preventing all Foundries in the world
         * from pulsing in perfect synchronization.
         */
        MoltenIronAnimation.Frame animationFrame =
                MoltenIronAnimation.getFrame(
                        level.getGameTime(),
                        controllerPos.asLong()
                );

        for (BlockPos tankPos : structure) {
            List<FoundryTankRenderedMetalLayer> localLayers =
                    renderedLayers.getOrDefault(tankPos, List.of());

            if (localLayers.isEmpty()) {
                continue;
            }

            float minX = structure.contains(tankPos.west())
                    ? 0.0f
                    : LIQUID_INSET;
            float maxX = structure.contains(tankPos.east())
                    ? 1.0f
                    : LIQUID_MAX_INSET;
            float minZ = structure.contains(tankPos.north())
                    ? 0.0f
                    : LIQUID_INSET;
            float maxZ = structure.contains(tankPos.south())
                    ? 1.0f
                    : LIQUID_MAX_INSET;

            poseStack.pushPose();
            poseStack.translate(
                    tankPos.getX() - controllerPos.getX(),
                    tankPos.getY() - controllerPos.getY(),
                    tankPos.getZ() - controllerPos.getZ()
            );
            PoseStack.Pose pose = poseStack.last();

            for (FoundryTankRenderedMetalLayer renderedLayer : localLayers) {
                MoltenMetalDefinition definition =
                        ModMoltenMetals.get(renderedLayer.metal())
                                .orElse(null);

                if (definition == null) {
                    continue;
                }

                VertexConsumer consumer = bufferSource.getBuffer(
                        RenderType.entityTranslucentCull(
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
                    FoundryTankLiquidQuads.renderLiquidHorizontalFace(
                            Direction.UP,
                            consumer,
                            pose,
                            minX,
                            maxX,
                            renderedLayer.maxY(),
                            minZ,
                            maxZ,
                            packedOverlay,
                            animationFrame.minV(),
                            animationFrame.maxV()
                    );
                }

                if (renderedLayer.renderBottom()) {
                    FoundryTankLiquidQuads.renderLiquidHorizontalFace(
                            Direction.DOWN,
                            consumer,
                            pose,
                            minX,
                            maxX,
                            renderedLayer.minY(),
                            minZ,
                            maxZ,
                            packedOverlay,
                            animationFrame.minV(),
                            animationFrame.maxV()
                    );
                }

                for (Direction side : Direction.Plane.HORIZONTAL) {
                    renderLayerSide(
                            tankPos,
                            side,
                            renderedLayer,
                            geometry,
                            renderedLayers,
                            consumer,
                            pose,
                            packedOverlay,
                            animationFrame.minV(),
                            animationFrame.maxV()
                    );
                }

                boolean isExposedTopSurface =
                        renderedLayer == localLayers.getLast()
                                && allLayersEmpty(
                                renderedLayers.get(tankPos.above())
                        );

                if (renderedLayer.renderTop() && isExposedTopSurface) {
                    surfaceEffects.renderAndSpawn(
                            controller,
                            tankPos,
                            structure,
                            definition,
                            geometry,
                            partialTick,
                            pose,
                            bufferSource,
                            packedOverlay,
                            animationFrame.minV(),
                            animationFrame.maxV()
                    );
                }
            }

            poseStack.popPose();
        }
    }

    private static Map<Integer, Map<ResourceLocation, Float>> distributeAcrossHorizontalLevels(
            Set<BlockPos> structure,
            Map<ResourceLocation, Float> globalAmounts
    ) {
        Map<Integer, Integer> tanksPerY = new TreeMap<>();

        for (BlockPos pos : structure) {
            tanksPerY.merge(pos.getY(), 1, Integer::sum);
        }

        Map<ResourceLocation, Float> remaining =
                new LinkedHashMap<>(globalAmounts);

        Map<Integer, Map<ResourceLocation, Float>> result =
                new TreeMap<>();

        for (Map.Entry<Integer, Integer> yEntry : tanksPerY.entrySet()) {
            int tankCount = Math.max(1, yEntry.getValue());
            float free = tankCount * (float) FoundryTankNetwork.TANK_CAPACITY;
            Map<ResourceLocation, Float> averagePerTank = new LinkedHashMap<>();

            for (MoltenMetalDefinition definition : ModMoltenMetals.heaviestFirst()) {
                if (free <= LIQUID_EPSILON) {
                    break;
                }

                float available = remaining.getOrDefault(definition.id(), 0.0f);

                if (available <= LIQUID_EPSILON) {
                    continue;
                }

                float placed = Math.min(free, available);
                averagePerTank.put(definition.id(), placed / tankCount);

                float left = available - placed;
                if (left > LIQUID_EPSILON) {
                    remaining.put(definition.id(), left);
                } else {
                    remaining.remove(definition.id());
                }

                free -= placed;
            }

            result.put(yEntry.getKey(), Map.copyOf(averagePerTank));
        }

        return result;
    }

    private static List<FoundryTankRenderedMetalLayer> createRenderedLayers(
            BlockPos tankPos,
            Set<BlockPos> structure,
            Map<Integer, Map<ResourceLocation, Float>> amountsByY
    ) {
        Map<ResourceLocation, Float> displayedAmounts =
                amountsByY.getOrDefault(tankPos.getY(), Map.of());

        if (displayedAmounts.isEmpty()) {
            return List.of();
        }

        Map<ResourceLocation, Float> belowAmounts =
                structure.contains(tankPos.below())
                        ? amountsByY.getOrDefault(tankPos.getY() - 1, Map.of())
                        : Map.of();

        Map<ResourceLocation, Float> aboveAmounts =
                structure.contains(tankPos.above())
                        ? amountsByY.getOrDefault(tankPos.getY() + 1, Map.of())
                        : Map.of();

        boolean anyLiquidBelow = sum(belowAmounts) > LIQUID_EPSILON;
        float cumulativeAmount = 0.0f;
        List<FoundryTankRenderedMetalLayer> result = new ArrayList<>();
        List<MoltenMetalDefinition> ordered = ModMoltenMetals.heaviestFirst();

        for (int index = 0; index < ordered.size(); index++) {
            MoltenMetalDefinition definition = ordered.get(index);
            float layerAmount = displayedAmounts.getOrDefault(definition.id(), 0.0f);

            if (layerAmount <= LIQUID_EPSILON) {
                continue;
            }

            float startAmount = cumulativeAmount;
            cumulativeAmount = Math.min(
                    FoundryTankNetwork.TANK_CAPACITY,
                    cumulativeAmount + layerAmount
            );
            float endAmount = cumulativeAmount;

            float minY = startAmount <= LIQUID_EPSILON && anyLiquidBelow
                    ? 0.0f
                    : Mth.lerp(
                            Mth.clamp(
                                    startAmount / FoundryTankNetwork.TANK_CAPACITY,
                                    0.0f,
                                    1.0f
                            ),
                            LIQUID_INSET,
                            LIQUID_MAX_INSET
                    );

            boolean continuesAbove =
                    endAmount >= FoundryTankNetwork.TANK_CAPACITY - LIQUID_EPSILON
                            && sum(aboveAmounts) > LIQUID_EPSILON;

            float maxY = continuesAbove
                    ? 1.0f
                    : Mth.lerp(
                            Mth.clamp(
                                    endAmount / FoundryTankNetwork.TANK_CAPACITY,
                                    0.0f,
                                    1.0f
                            ),
                            LIQUID_INSET,
                            LIQUID_MAX_INSET
                    );

            ResourceLocation nextMetal =
                    getNextDisplayedMetal(displayedAmounts, ordered, index + 1);

            boolean renderTop;
            if (nextMetal != null) {
                renderTop = !nextMetal.equals(definition.id());
            } else if (continuesAbove) {
                ResourceLocation metalAbove = getBottomDisplayedMetal(aboveAmounts);
                renderTop = metalAbove == null || !metalAbove.equals(definition.id());
            } else {
                renderTop = true;
            }

            boolean renderBottom = result.isEmpty() && !anyLiquidBelow;

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

        return List.copyOf(result);
    }

    private static void renderLayerSide(
            BlockPos tankPos,
            Direction side,
            FoundryTankRenderedMetalLayer renderedLayer,
            FoundryTankLiquidGeometry geometry,
            Map<BlockPos, List<FoundryTankRenderedMetalLayer>> allLayers,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedOverlay,
            float frameMinV,
            float frameMaxV
    ) {
        BlockPos neighborPos = tankPos.relative(side);
        List<FoundryTankRenderedMetalLayer> neighborLayers = allLayers.get(neighborPos);

        if (neighborLayers == null) {
            FoundryTankLiquidQuads.renderLiquidSideSegment(
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

        List<FoundryTankVerticalInterval> visible = new ArrayList<>();
        visible.add(
                new FoundryTankVerticalInterval(
                        renderedLayer.minY(),
                        renderedLayer.maxY()
                )
        );

        for (FoundryTankRenderedMetalLayer neighborLayer : neighborLayers) {
            visible = subtractInterval(
                    visible,
                    new FoundryTankVerticalInterval(
                            neighborLayer.minY(),
                            neighborLayer.maxY()
                    )
            );

            if (visible.isEmpty()) {
                return;
            }
        }

        for (FoundryTankVerticalInterval interval : visible) {
            FoundryTankLiquidQuads.renderLiquidSideSegment(
                    side,
                    consumer,
                    pose,
                    geometry,
                    interval.minY(),
                    interval.maxY(),
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
        List<FoundryTankVerticalInterval> result = new ArrayList<>();

        for (FoundryTankVerticalInterval interval : source) {
            float overlapMin = Math.max(interval.minY(), removed.minY());
            float overlapMax = Math.min(interval.maxY(), removed.maxY());

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

    private static ResourceLocation getNextDisplayedMetal(
            Map<ResourceLocation, Float> displayedAmounts,
            List<MoltenMetalDefinition> ordered,
            int startIndex
    ) {
        for (int index = startIndex; index < ordered.size(); index++) {
            MoltenMetalDefinition definition = ordered.get(index);
            if (displayedAmounts.getOrDefault(definition.id(), 0.0f) > LIQUID_EPSILON) {
                return definition.id();
            }
        }
        return null;
    }

    private static ResourceLocation getBottomDisplayedMetal(
            Map<ResourceLocation, Float> displayedAmounts
    ) {
        for (MoltenMetalDefinition definition : ModMoltenMetals.heaviestFirst()) {
            if (displayedAmounts.getOrDefault(definition.id(), 0.0f) > LIQUID_EPSILON) {
                return definition.id();
            }
        }
        return null;
    }

    private static boolean allLayersEmpty(
            List<FoundryTankRenderedMetalLayer> layers
    ) {
        return layers == null || layers.isEmpty();
    }

    private static float sum(
            Map<ResourceLocation, Float> amounts
    ) {
        float total = 0.0f;
        for (Float amount : amounts.values()) {
            if (amount != null && amount > 0.0f) {
                total += amount;
            }
        }
        return total;
    }
}
