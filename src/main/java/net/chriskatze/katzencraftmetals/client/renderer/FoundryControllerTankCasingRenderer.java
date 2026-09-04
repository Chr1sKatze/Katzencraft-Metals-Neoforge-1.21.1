package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.block.entity.FoundryControllerBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.Set;

import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.SIDE_TEXTURE;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.TOP_TEXTURE;

/**
 * Draws the old connected Tank casing once through the owning Controller.
 *
 * Important: each render-type pass obtains and consumes its VertexConsumer
 * completely before another render type is requested. MultiBufferSource may
 * finish the currently active shared BufferBuilder when getBuffer() is called
 * for a different RenderType, so retaining consumers across render-type
 * switches can cause IllegalStateException("Not building!").
 */
final class FoundryControllerTankCasingRenderer {

    private FoundryControllerTankCasingRenderer() {
    }

    static void render(
            FoundryControllerBlockEntity controller,
            Set<BlockPos> structure,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedOverlay
    ) {
        Level level = controller.getLevel();

        if (level == null || structure.isEmpty()) {
            return;
        }

        BlockPos controllerPos = controller.getBlockPos();

        /*
         * Pass 1: vertical side casing/frames.
         *
         * Do not request another RenderType until every write through this
         * consumer is finished.
         */
        VertexConsumer sideConsumer = bufferSource.getBuffer(
                RenderType.entityCutoutNoCullZOffset(SIDE_TEXTURE)
        );

        for (BlockPos tankPos : structure) {
            poseStack.pushPose();
            translateToTank(poseStack, controllerPos, tankPos);

            PoseStack.Pose pose = poseStack.last();
            int packedLight = LevelRenderer.getLightColor(level, tankPos);

            for (Direction face : Direction.Plane.HORIZONTAL) {
                if (structure.contains(tankPos.relative(face))) {
                    continue;
                }

                FoundryTankSideFrameRenderer.render(
                        tankPos,
                        structure,
                        face,
                        sideConsumer,
                        pose,
                        packedLight,
                        packedOverlay
                );
            }

            poseStack.popPose();
        }

        /*
         * Pass 2: top/bottom/horizontal frame pieces.
         * Requesting this buffer is allowed to finish the side pass because
         * sideConsumer is never used again below.
         */
        VertexConsumer topConsumer = bufferSource.getBuffer(
                RenderType.entityCutoutNoCullZOffset(TOP_TEXTURE)
        );

        for (BlockPos tankPos : structure) {
            poseStack.pushPose();
            translateToTank(poseStack, controllerPos, tankPos);

            PoseStack.Pose pose = poseStack.last();
            int packedLight = LevelRenderer.getLightColor(level, tankPos);

            if (!structure.contains(tankPos.above())) {
                FoundryTankHorizontalFrameRenderer.render(
                        tankPos,
                        structure,
                        Direction.UP,
                        topConsumer,
                        pose,
                        packedLight,
                        packedOverlay
                );
            }

            if (!structure.contains(tankPos.below())) {
                FoundryTankHorizontalFrameRenderer.render(
                        tankPos,
                        structure,
                        Direction.DOWN,
                        topConsumer,
                        pose,
                        packedLight,
                        packedOverlay
                );
            } else {
                FoundryTankHorizontalFrameRenderer.renderRaisedFootprintCornerCaps(
                        tankPos,
                        structure,
                        topConsumer,
                        pose,
                        packedLight,
                        packedOverlay
                );
            }

            poseStack.popPose();
        }

        /*
         * Pass 3: pieces that select their own RenderTypes.
         * No side/top consumer is touched after these calls begin, so their
         * internal getBuffer() calls cannot invalidate a consumer we still use.
         */
        for (BlockPos tankPos : structure) {
            poseStack.pushPose();
            translateToTank(poseStack, controllerPos, tankPos);

            PoseStack.Pose pose = poseStack.last();
            int packedLight = LevelRenderer.getLightColor(level, tankPos);

            FoundryTankIntakeHatchRenderer.render(
                    level,
                    tankPos,
                    level.getBlockState(tankPos),
                    pose,
                    bufferSource,
                    packedLight,
                    packedOverlay
            );

            FoundryControllerTankFaucetOverlayRenderer.render(
                    level,
                    tankPos,
                    structure,
                    pose,
                    bufferSource,
                    packedLight,
                    packedOverlay
            );

            poseStack.popPose();
        }
    }

    private static void translateToTank(
            PoseStack poseStack,
            BlockPos controllerPos,
            BlockPos tankPos
    ) {
        poseStack.translate(
                tankPos.getX() - controllerPos.getX(),
                tankPos.getY() - controllerPos.getY(),
                tankPos.getZ() - controllerPos.getZ()
        );
    }
}
