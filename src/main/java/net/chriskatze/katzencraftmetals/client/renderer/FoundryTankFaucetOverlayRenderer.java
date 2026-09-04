package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock;
import net.chriskatze.katzencraftmetals.block.entity.FoundryFaucetBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.FAUCET_OVERLAY_OFFSET;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.FAUCET_OVERLAY_TEXTURE;

/**
 * Renders the translucent Faucet entrance shadow on the Tank face behind an
 * attached Faucet.
 *
 * Tanks are ordinary blocks and have no BlockEntity renderer. The active
 * Faucet already has a BlockEntity renderer for its stream, so that renderer
 * owns this small dynamic entrance shadow as well.
 */
final class FoundryTankFaucetOverlayRenderer {

    private FoundryTankFaucetOverlayRenderer() {
    }

    /**
     * The pose stack supplied to a Faucet BER is local to the Faucet block.
     * Translate one block backward to the attached Tank and then draw the
     * overlay quad slightly outward from that Tank's exposed face.
     */
    static void renderForFaucet(
            FoundryFaucetBlockEntity faucet,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedOverlay
    ) {
        if (
                faucet == null
                        || faucet.getLevel() == null
        ) {
            return;
        }

        Level level = faucet.getLevel();
        BlockState faucetState = faucet.getBlockState();

        if (!faucetState.hasProperty(FoundryFaucetBlock.FACING)) {
            return;
        }

        Direction face =
                faucetState.getValue(
                        FoundryFaucetBlock.FACING
                );

        if (!face.getAxis().isHorizontal()) {
            return;
        }

        BlockPos faucetPos =
                faucet.getBlockPos();

        BlockPos tankPos =
                faucetPos.relative(
                        face.getOpposite()
                );

        /*
         * Never render a detached/stale shadow. The neighboring source block
         * must still physically be a current Foundry Tank on the client.
         */
        if (!level.getBlockState(tankPos)
                .is(ModBlocks.FOUNDRY_TANK.get())) {
            return;
        }

        /*
         * Preserve the original "door" behavior:
         * - idle faucet: shadow is closed/visible
         * - active pour: shadow is open/hidden
         * - shutdown: remain open until the visible cauldron fill catches up
         */
        if (shouldKeepFaucetOverlayOpen(
                faucet,
                partialTick
        )) {
            return;
        }

        VertexConsumer overlayConsumer =
                bufferSource.getBuffer(
                        RenderType.entityTranslucent(
                                FAUCET_OVERLAY_TEXTURE
                        )
                );

        poseStack.pushPose();

        poseStack.translate(
                tankPos.getX() - faucetPos.getX(),
                tankPos.getY() - faucetPos.getY(),
                tankPos.getZ() - faucetPos.getZ()
        );

        renderFaucetOverlay(
                face,
                overlayConsumer,
                poseStack.last(),
                LevelRenderer.getLightColor(
                        level,
                        tankPos
                ),
                packedOverlay
        );

        poseStack.popPose();
    }

    private static boolean shouldKeepFaucetOverlayOpen(
            FoundryFaucetBlockEntity faucet,
            float partialTick
    ) {
        /*
         * The entrance remains open for the complete active pouring period.
         */
        if (faucet.isPouring()) {
            return true;
        }

        /*
         * Once the shutdown animation has finished, the entrance shadow is
         * closed again.
         */
        if (
                !faucet.isDraining()
                        || faucet.getLevel() == null
        ) {
            return false;
        }

        FoundryFaucetBlockEntity.CauldronTarget target =
                FoundryFaucetBlockEntity.findCauldronTarget(
                        faucet.getLevel(),
                        faucet.getBlockPos()
                );

        if (target == null) {
            return false;
        }

        float displayedMoltenAmount =
                CastingCauldronFillSmoother.getDisplayedMoltenAmount(
                        target.cauldron(),
                        partialTick
                );

        float actualMoltenAmount =
                target.cauldron()
                        .getMoltenAmount();

        /*
         * During shutdown, keep the entrance open only while the most recently
         * transferred molten metal is still visibly raising the Cauldron
         * surface. As soon as the visual height catches up, the black entrance
         * closes and appears to cut the remaining stream off at its source.
         */
        return displayedMoltenAmount
                < actualMoltenAmount - 0.0001f;
    }

    private static void renderFaucetOverlay(
            Direction face,
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        FoundryTankCasingQuads.renderSideRect(
                face,
                consumer,
                pose,
                0.0f,
                1.0f,
                0.0f,
                1.0f,
                0.0f,
                1.0f,
                0.0f,
                1.0f,
                packedLight,
                packedOverlay,
                FAUCET_OVERLAY_OFFSET
        );
    }
}
