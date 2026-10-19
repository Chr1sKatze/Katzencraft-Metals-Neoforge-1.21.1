package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;

final class FoundryFaucetRenderTransform {

    private FoundryFaucetRenderTransform() {
    }

    static void rotateDownAssembly(PoseStack poses, net.minecraft.world.level.block.state.BlockState state) {
        rotateDownAssembly(poses, state.getValue(net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock.DOWN_ROTATION));
    }

    static void rotateDownAssembly(PoseStack poses, Direction rotation) {
        float angle = switch (rotation) {
            case EAST -> -90;
            case SOUTH -> -180;
            case WEST -> -270;
            default -> 0;
        };
        poses.translate(.5, 0, .5);
        poses.mulPose(Axis.YP.rotationDegrees(angle));
        poses.translate(-.5, 0, -.5);
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
