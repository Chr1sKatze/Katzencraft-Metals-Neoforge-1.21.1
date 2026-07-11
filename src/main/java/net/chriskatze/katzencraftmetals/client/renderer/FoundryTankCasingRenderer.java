package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;

import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.SIDE_TEXTURE;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.TOP_TEXTURE;

/**
 * High-level connected casing renderer.
 */
final class FoundryTankCasingRenderer {

    private FoundryTankCasingRenderer() {
    }

    static void render(
            FoundryTankBlockEntity tank,
            PoseStack.Pose pose,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        VertexConsumer sideConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCullZOffset(
                                SIDE_TEXTURE
                        )
                );

        for (Direction face : Direction.Plane.HORIZONTAL) {
            if (FoundryTankVisualConnections.isSameComponent(
                    tank,
                    face
            )) {
                continue;
            }

            FoundryTankSideFrameRenderer.render(
                    tank,
                    face,
                    sideConsumer,
                    pose,
                    packedLight,
                    packedOverlay
            );
        }

        VertexConsumer topConsumer =
                bufferSource.getBuffer(
                        RenderType.entityCutoutNoCullZOffset(
                                TOP_TEXTURE
                        )
                );

        if (!FoundryTankVisualConnections.isSameComponent(
                tank,
                Direction.UP
        )) {
            FoundryTankHorizontalFrameRenderer.render(
                    tank,
                    Direction.UP,
                    topConsumer,
                    pose,
                    packedLight,
                    packedOverlay
            );
        }

        if (!FoundryTankVisualConnections.isSameComponent(
                tank,
                Direction.DOWN
        )) {
            FoundryTankHorizontalFrameRenderer.render(
                    tank,
                    Direction.DOWN,
                    topConsumer,
                    pose,
                    packedLight,
                    packedOverlay
            );
        } else {
            /*
             * A raised Tank can sit inside a wider lower footprint.
             *
             * The top frames of the surrounding lower Tanks meet at the four
             * corners of the raised Tank. Those junctions need an explicit
             * 1x1 cap even though the lower diagonal Tank exists.
             */
            FoundryTankHorizontalFrameRenderer.renderRaisedFootprintCornerCaps(
                    tank,
                    topConsumer,
                    pose,
                    packedLight,
                    packedOverlay
            );
        }
    }
}
