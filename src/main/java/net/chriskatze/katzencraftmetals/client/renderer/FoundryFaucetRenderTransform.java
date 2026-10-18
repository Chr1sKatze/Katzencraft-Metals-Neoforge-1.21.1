package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;

final class FoundryFaucetRenderTransform {

    private FoundryFaucetRenderTransform() {
    }

    static void rotateForFacing(
            PoseStack poseStack,
            Direction facing
    ) {
        if (facing == Direction.DOWN) {
            // Existing vertical stream center is z=9/16; the underside nozzle is centered.
            poseStack.translate(0, 0, FoundryFaucetRenderConstants.DOWN_STREAM_Z_OFFSET);
            return;
        }

        float rotationDegrees =
                switch (facing) {
                    case NORTH -> 0.0f;
                    case EAST -> 270.0f;
                    case SOUTH -> 180.0f;
                    case WEST -> 90.0f;
                    default -> 0.0f;
                };

        poseStack.translate(
                0.5f,
                0.0f,
                0.5f
        );

        poseStack.mulPose(
                Axis.YP.rotationDegrees(
                        rotationDegrees
                )
        );

        poseStack.translate(
                -0.5f,
                0.0f,
                -0.5f
        );
    }
}
