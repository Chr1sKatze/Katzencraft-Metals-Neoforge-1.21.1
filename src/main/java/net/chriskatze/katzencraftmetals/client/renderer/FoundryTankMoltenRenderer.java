package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankNetwork;
import net.chriskatze.katzencraftmetals.metal.FoundryMetalLayer;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.LIQUID_INSET;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.LIQUID_MAX_INSET;

/**
 * Renders seamless molten-metal volumes inside connected Foundry Tanks.
 */
final class FoundryTankMoltenRenderer {

    private static final double SURFACE_EFFECT_RENDER_DISTANCE_SQ =
            24.0D * 24.0D;

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

    /*
     * Empty Tank structures are very common while building/testing large
     * foundries. Without this guard, every visible empty Tank still enters the
     * molten layer builder, which asks the liquid smoother for displayed
     * horizontal layer amounts and can perform network-level visual snapshot
     * work before eventually returning no rendered layers.
     *
     * This per-tick cache keeps empty structures on the cheap path:
     *
     * - one cheap local-layer scan per Tank structure per client tick
     * - every rendered Tank in that same empty structure then returns before
     *   liquid smoothing, animation frame lookup, translucent buffer lookup, or
     *   surface-effect checks
     */
    private final Map<EmptyLiquidCheckKey, Boolean> hasRenderableMetalFrameCache =
            new HashMap<>();

    private Level hasRenderableMetalFrameCacheLevel;
    private long hasRenderableMetalFrameCacheGameTime =
            Long.MIN_VALUE;

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
        if (!hasAnyRenderableMetalInCurrentStructure(
                tank
        )) {
            return;
        }

        List<FoundryTankRenderedMetalLayer> renderedLayers =
                layerBuilder.createRenderedLayers(
                        tank,
                        partialTick
                );

        if (
                renderedLayers.isEmpty()
                        || !hasAnyVisibleMoltenQuad(
                        tank,
                        renderedLayers
                )
        ) {
            return;
        }

        double distanceSquared =
                distanceSquaredToCamera(
                        tank
                );

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
                        consumer,
                        pose,
                        packedOverlay,
                        frameMinV,
                        frameMaxV
                );
            }

            boolean exposedTopSurfaceLayer =
                    index == renderedLayers.size() - 1
                            && renderedLayer.renderTop();

            /*
             * Surface effects already stop at 24 blocks internally. This cheap
             * outer distance gate avoids entering the effect renderer for
             * mid/far Tanks where it cannot render hot spots or smoke anyway.
             */
            if (
                    exposedTopSurfaceLayer
                            && distanceSquared <= SURFACE_EFFECT_RENDER_DISTANCE_SQ
            ) {
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

    private boolean hasAnyRenderableMetalInCurrentStructure(
            FoundryTankBlockEntity tank
    ) {
        Level level =
                tank.getLevel();

        if (level == null) {
            return false;
        }

        long gameTime =
                level.getGameTime();

        if (
                hasRenderableMetalFrameCacheLevel != level
                        || hasRenderableMetalFrameCacheGameTime != gameTime
        ) {
            hasRenderableMetalFrameCache.clear();

            hasRenderableMetalFrameCacheLevel =
                    level;

            hasRenderableMetalFrameCacheGameTime =
                    gameTime;
        }

        EmptyLiquidCheckKey cacheKey =
                createEmptyLiquidCheckKey(
                        tank
                );

        Boolean cached =
                hasRenderableMetalFrameCache.get(
                        cacheKey
                );

        if (cached != null) {
            return cached;
        }

        boolean hasMetal =
                computeHasAnyRenderableMetalInCurrentStructure(
                        tank
                );

        hasRenderableMetalFrameCache.put(
                cacheKey,
                hasMetal
        );

        return hasMetal;
    }

    private static boolean computeHasAnyRenderableMetalInCurrentStructure(
            FoundryTankBlockEntity tank
    ) {
        Level level =
                tank.getLevel();

        if (level == null) {
            return false;
        }

        FoundryTankNetwork network =
                tank.getNetwork();

        if (
                network == null
                        || network.getTankPositions()
                        .isEmpty()
        ) {
            return tankHasAnyRenderableLocalMetal(
                    tank
            );
        }

        for (BlockPos tankPos : network.getTankPositions()) {
            if (
                    level.getBlockEntity(
                            tankPos
                    )
                            instanceof FoundryTankBlockEntity networkTank
                            && tankHasAnyRenderableLocalMetal(
                            networkTank
                    )
            ) {
                return true;
            }
        }

        return false;
    }

    private static boolean tankHasAnyRenderableLocalMetal(
            FoundryTankBlockEntity tank
    ) {
        for (FoundryMetalLayer layer : tank.getLocalMetalLayers()) {
            if (
                    layer.amount() > 0
                            && ModMoltenMetals.contains(
                            layer.metal()
                    )
            ) {
                return true;
            }
        }

        return false;
    }

    private static EmptyLiquidCheckKey createEmptyLiquidCheckKey(
            FoundryTankBlockEntity tank
    ) {
        Level level =
                tank.getLevel();

        FoundryTankNetwork network =
                tank.getNetwork();

        if (network != null) {
            UUID ownerId =
                    network.getOwnerId();

            if (ownerId != null) {
                return new EmptyLiquidCheckKey(
                        level,
                        ownerId,
                        BlockPos.ZERO
                );
            }

            return new EmptyLiquidCheckKey(
                    level,
                    null,
                    canonicalNetworkAnchor(
                            network.getTankPositions(),
                            tank.getBlockPos()
                    )
            );
        }

        return new EmptyLiquidCheckKey(
                level,
                null,
                tank.getBlockPos()
                        .immutable()
        );
    }

    private static BlockPos canonicalNetworkAnchor(
            Set<BlockPos> positions,
            BlockPos fallback
    ) {
        if (
                positions == null
                        || positions.isEmpty()
        ) {
            return fallback.immutable();
        }

        BlockPos best =
                null;

        for (BlockPos position : positions) {
            if (
                    best == null
                            || compareBlockPositions(
                            position,
                            best
                    ) < 0
            ) {
                best =
                        position;
            }
        }

        return best == null
                ? fallback.immutable()
                : best.immutable();
    }

    private static int compareBlockPositions(
            BlockPos first,
            BlockPos second
    ) {
        int byY =
                Integer.compare(
                        first.getY(),
                        second.getY()
                );

        if (byY != 0) {
            return byY;
        }

        int byX =
                Integer.compare(
                        first.getX(),
                        second.getX()
                );

        if (byX != 0) {
            return byX;
        }

        return Integer.compare(
                first.getZ(),
                second.getZ()
        );
    }

    private void renderLayerSide(
            FoundryTankBlockEntity tank,
            Direction side,
            FoundryTankRenderedMetalLayer renderedLayer,
            FoundryTankLiquidGeometry geometry,
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

        /*
         * Horizontal same-component sides are internal liquid faces.
         *
         * The visual liquid levels are already averaged across the connected
         * horizontal tank level, so neighboring tanks on the same Y level share
         * the same rendered liquid boundaries. That means the internal side
         * cannot be visible from outside the foundry.
         *
         * Previously we still fetched the neighbor's rendered layers and
         * subtracted intervals just to prove that nothing was visible. In large
         * 4x4x4 foundries, most horizontal side checks are internal, so this
         * shortcut avoids a lot of repeated hidden-side work.
         */
        if (neighbor != null) {
            return;
        }

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
    }

    private static boolean hasAnyVisibleMoltenQuad(
            FoundryTankBlockEntity tank,
            List<FoundryTankRenderedMetalLayer> renderedLayers
    ) {
        for (FoundryTankRenderedMetalLayer renderedLayer : renderedLayers) {
            if (
                    renderedLayer.renderTop()
                            || renderedLayer.renderBottom()
            ) {
                return true;
            }
        }

        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (
                    shouldRenderLiquidSideForCamera(
                            tank,
                            side
                    )
                            && !FoundryTankVisualConnections.isSameComponent(
                            tank,
                            side
                    )
            ) {
                return true;
            }
        }

        return false;
    }

    private static double distanceSquaredToCamera(
            FoundryTankBlockEntity tank
    ) {
        Entity camera =
                Minecraft.getInstance()
                        .cameraEntity;

        if (camera == null) {
            return 0.0D;
        }

        double tankCenterX =
                tank.getBlockPos()
                        .getX()
                        + 0.5D;

        double tankCenterY =
                tank.getBlockPos()
                        .getY()
                        + 0.5D;

        double tankCenterZ =
                tank.getBlockPos()
                        .getZ()
                        + 0.5D;

        double deltaX =
                camera.getX() - tankCenterX;

        double deltaY =
                camera.getY() - tankCenterY;

        double deltaZ =
                camera.getZ() - tankCenterZ;

        return deltaX * deltaX
                + deltaY * deltaY
                + deltaZ * deltaZ;
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

    private record EmptyLiquidCheckKey(
            Level level,
            UUID ownerId,
            BlockPos fallbackAnchor
    ) {
    }

}
