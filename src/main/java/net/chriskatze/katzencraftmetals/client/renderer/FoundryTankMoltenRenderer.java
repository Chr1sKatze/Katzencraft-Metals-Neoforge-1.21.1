package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.LIQUID_EPSILON;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.LIQUID_INSET;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.LIQUID_MAX_INSET;

/**
 * Renders seamless molten-metal volumes inside connected Foundry Tanks.
 */
final class FoundryTankMoltenRenderer {

    /*
     * If the camera is very close to the tank center, render all side faces.
     * This avoids obvious side popping while walking directly beside/through a
     * transparent tank wall.
     */
    private static final double CLOSE_CAMERA_SIDE_RENDER_DISTANCE_SQ =
            1.75D * 1.75D;

    /*
     * Small dead-zone around the tank center line. At a perfect diagonal or
     * when the camera coordinate is nearly aligned with the center, allow both
     * relevant side directions instead of causing fast flip-flopping.
     */
    private static final double SIDE_CAMERA_EPSILON =
            0.025D;

    private final FoundryTankMoltenLayerBuilder layerBuilder;
    private final FoundryTankSurfaceEffectsRenderer surfaceEffectsRenderer =
            new FoundryTankSurfaceEffectsRenderer();

    FoundryTankMoltenRenderer(
            FoundryTankMoltenLayerBuilder layerBuilder
    ) {
        this.layerBuilder =
                layerBuilder;
    }

    void render(
            FoundryTankBlockEntity tank,
            float partialTick,
            PoseStack.Pose pose,
            MultiBufferSource bufferSource,
            int packedOverlay
    ) {
        List<FoundryTankRenderedMetalLayer> renderedLayers =
                layerBuilder.createRenderedLayers(
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

        Map<Direction, List<FoundryTankRenderedMetalLayer>> neighborLayerCache =
                new EnumMap<>(
                        Direction.class
                );

        for (int index = 0; index < renderedLayers.size(); index++) {
            FoundryTankRenderedMetalLayer renderedLayer =
                    renderedLayers.get(index);

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
                        frameMinV,
                        frameMaxV
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
                        frameMinV,
                        frameMaxV
                );
            }

            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (!shouldRenderLiquidSideForCamera(
                        tank,
                        side
                )) {
                    continue;
                }

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
                        frameMaxV,
                        neighborLayerCache
                );
            }

            /*
             * renderTop() can also be true for internal metal boundaries inside
             * one tank section. Surface effects should only belong to the actual
             * exposed top liquid surface.
             */
            boolean exposedTopSurfaceLayer =
                    index == renderedLayers.size() - 1
                            && renderedLayer.renderTop();

            if (exposedTopSurfaceLayer) {
                surfaceEffectsRenderer.renderAndSpawn(
                        tank,
                        definition,
                        geometry,
                        partialTick,
                        pose,
                        bufferSource,
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
            float frameMaxV,
            Map<Direction, List<FoundryTankRenderedMetalLayer>> neighborLayerCache
    ) {
        FoundryTankBlockEntity neighbor =
                FoundryTankVisualConnections.getSameComponentNeighbor(
                        tank,
                        side
                );

        if (neighbor == null) {
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

        List<FoundryTankVerticalInterval> visibleIntervals =
                new ArrayList<>();

        visibleIntervals.add(
                new FoundryTankVerticalInterval(
                        renderedLayer.minY(),
                        renderedLayer.maxY()
                )
        );

        List<FoundryTankRenderedMetalLayer> neighborLayers =
                neighborLayerCache.computeIfAbsent(
                        side,
                        ignored -> layerBuilder.createRenderedLayers(
                                neighbor,
                                partialTick
                        )
                );

        for (FoundryTankRenderedMetalLayer neighborLayer : neighborLayers) {
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
            FoundryTankLiquidQuads.renderLiquidSideSegment(
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

    private static boolean shouldRenderLiquidSideForCamera(
            FoundryTankBlockEntity tank,
            Direction side
    ) {
        Entity camera =
                Minecraft.getInstance()
                        .cameraEntity;

        if (camera == null) {
            return true;
        }

        double tankCenterX =
                tank.getBlockPos()
                        .getX()
                        + 0.5D;

        double tankCenterZ =
                tank.getBlockPos()
                        .getZ()
                        + 0.5D;

        double deltaX =
                camera.getX() - tankCenterX;

        double deltaZ =
                camera.getZ() - tankCenterZ;

        double horizontalDistanceSq =
                deltaX * deltaX
                        + deltaZ * deltaZ;

        if (horizontalDistanceSq <= CLOSE_CAMERA_SIDE_RENDER_DISTANCE_SQ) {
            return true;
        }

        return switch (side) {
            case NORTH -> deltaZ < SIDE_CAMERA_EPSILON;
            case SOUTH -> deltaZ > -SIDE_CAMERA_EPSILON;
            case WEST -> deltaX < SIDE_CAMERA_EPSILON;
            case EAST -> deltaX > -SIDE_CAMERA_EPSILON;
            default -> true;
        };
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
}
