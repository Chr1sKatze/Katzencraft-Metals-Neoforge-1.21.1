package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.LIQUID_EPSILON;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.LIQUID_INSET;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.LIQUID_MAX_INSET;

/**
 * Renders seamless molten-metal volumes inside connected Foundry Tanks.
 */
final class FoundryTankMoltenRenderer {

    private final FoundryTankMoltenLayerBuilder layerBuilder;

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

        for (
                FoundryTankRenderedMetalLayer neighborLayer :
                layerBuilder.createRenderedLayers(
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
