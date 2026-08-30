package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

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

        poseStack.pushPose();

        PoseStack.Pose pose =
                poseStack.last();

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

        moltenRenderer.render(
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
        FoundryTankFaucetOverlayRenderer.render(
                tank,
                partialTick,
                pose,
                bufferSource,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
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
}
