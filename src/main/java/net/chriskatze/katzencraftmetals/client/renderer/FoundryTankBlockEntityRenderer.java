package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;

/**
 * Dynamically renders the complete visual Foundry Tank multiblock.
 *
 * This class intentionally only coordinates the render order:
 *
 * 1. connected exterior casing
 * 2. seamless molten-metal layers
 * 3. Faucet entrance overlays
 */
public class FoundryTankBlockEntityRenderer
        implements BlockEntityRenderer<FoundryTankBlockEntity> {

    private static final int MAX_VIEW_DISTANCE_CHUNKS =
            20;

    private static final int BLOCKS_PER_CHUNK =
            16;

    private static final int VIEW_DISTANCE =
            MAX_VIEW_DISTANCE_CHUNKS * BLOCKS_PER_CHUNK;

    private static final double FAUCET_OVERLAY_DISTANCE_SQ =
            50.0D * 50.0D;

    private final FoundryTankLiquidSmoother liquidSmoother =
            new FoundryTankLiquidSmoother();

    private final FoundryTankMoltenLayerBuilder moltenLayerBuilder =
            new FoundryTankMoltenLayerBuilder(
                    liquidSmoother
            );

    private final FoundryTankMoltenRenderer moltenRenderer =
            new FoundryTankMoltenRenderer(
                    moltenLayerBuilder
            );

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

        boolean completelyHiddenInsideSameComponent =
                isCompletelyHiddenInsideSameComponent(
                        tank
                );

        poseStack.pushPose();

        PoseStack.Pose pose =
                poseStack.last();

        if (!completelyHiddenInsideSameComponent) {
            FoundryTankCasingRenderer.render(
                    tank,
                    pose,
                    bufferSource,
                    packedLight,
                    packedOverlay
            );

            FoundryTankIntakeHatchRenderer.render(
                    tank,
                    pose,
                    bufferSource,
                    packedLight,
                    packedOverlay
            );
        }

        /*
         * Even a Tank surrounded by same-component Tank blocks can still need
         * to render molten metal.
         *
         * Example: if the Tank above is empty, a liquid surface inside this
         * otherwise internal Tank is visible through the transparent upper Tank
         * volume. Skipping molten rendering here caused the 2x2 center hole in
         * full 4x4x4 foundries with multiple metals.
         */
        moltenRenderer.render(
                tank,
                partialTick,
                pose,
                bufferSource,
                packedOverlay
        );

        if (
                !completelyHiddenInsideSameComponent
                        && shouldRenderFaucetOverlay(
                        tank
                )
        ) {
            /*
             * Render translucent entrance shadows after the molten metal so the
             * shadow blends over the visible liquid instead of hiding it through
             * the depth buffer.
             *
             * Past about 50 blocks this tiny entrance shadow is not readable,
             * but it still performs per-side checks and translucent buffer
             * work, so it is the only detail skipped in this LOD step.
             */
            FoundryTankFaucetOverlayRenderer.render(
                    tank,
                    partialTick,
                    pose,
                    bufferSource,
                    packedLight,
                    packedOverlay
            );
        }

        poseStack.popPose();
    }

    private static boolean isCompletelyHiddenInsideSameComponent(
            FoundryTankBlockEntity tank
    ) {
        /*
         * A Tank that has same-component neighbors on all six sides cannot
         * expose casing, faucet overlays, or a top intake hatch.
         *
         * It may still need molten rendering if an empty Tank above makes the
         * liquid surface visible through the multiblock interior.
         */
        for (Direction direction : Direction.values()) {
            if (!FoundryTankVisualConnections.isSameComponent(
                    tank,
                    direction
            )) {
                return false;
            }
        }

        return true;
    }

    private static boolean shouldRenderFaucetOverlay(
            FoundryTankBlockEntity tank
    ) {
        Entity camera =
                Minecraft.getInstance()
                        .cameraEntity;

        if (camera == null) {
            return true;
        }

        double centerX =
                tank.getBlockPos()
                        .getX()
                        + 0.5D;

        double centerY =
                tank.getBlockPos()
                        .getY()
                        + 0.5D;

        double centerZ =
                tank.getBlockPos()
                        .getZ()
                        + 0.5D;

        double deltaX =
                camera.getX() - centerX;

        double deltaY =
                camera.getY() - centerY;

        double deltaZ =
                camera.getZ() - centerZ;

        double distanceSquared =
                deltaX * deltaX
                        + deltaY * deltaY
                        + deltaZ * deltaZ;

        return distanceSquared <= FAUCET_OVERLAY_DISTANCE_SQ;
    }

    @Override
    public boolean shouldRenderOffScreen(
            FoundryTankBlockEntity tank
    ) {
        /*
         * This renderer only draws the current Tank block and small local
         * overlays. It does not need to stay active when the block entity is
         * outside the camera frustum.
         *
         * Keeping this false lets Minecraft skip offscreen Tank block entities
         * normally, which matters a lot for large 4x4x4 foundries and multiple
         * foundries in one area.
         */
        return false;
    }

    @Override
    public int getViewDistance() {
        /*
         * Block-entity renderers have their own distance cap in addition to the
         * user's actual chunk render distance.
         *
         * This cap supports up to 20 chunks:
         *
         * 20 chunks * 16 blocks = 320 blocks.
         *
         * If the player's normal render distance is lower than 20 chunks, the
         * foundry still disappears earlier because those chunks are not rendered
         * / available to the client. This value only prevents the block-entity
         * renderer from cutting the foundry off early when the player has a high
         * render distance.
         */
        return VIEW_DISTANCE;
    }
}
