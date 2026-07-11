package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.block.entity.FoundryFaucetBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;

import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.FAUCET_OVERLAY_OFFSET;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.FAUCET_OVERLAY_TEXTURE;

/**
 * Renders Faucet entrance shadow overlays on exposed Tank faces.
 */
final class FoundryTankFaucetOverlayRenderer {

    private FoundryTankFaucetOverlayRenderer() {
    }

    static void render(
            FoundryTankBlockEntity tank,
            float partialTick,
            PoseStack.Pose pose,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        VertexConsumer overlayConsumer =
                bufferSource.getBuffer(
                        RenderType.entityTranslucent(
                                FAUCET_OVERLAY_TEXTURE
                        )
                );

        for (Direction face : Direction.Plane.HORIZONTAL) {
            if (
                    FoundryTankVisualConnections.isSameComponent(
                            tank,
                            face
                    )
                            || !FoundryTankVisualConnections.hasAttachedFaucet(
                            tank,
                            face
                    )
            ) {
                continue;
            }

            if (
                    tank.getLevel()
                            .getBlockEntity(
                                    tank.getBlockPos()
                                            .relative(face)
                            )
                            instanceof FoundryFaucetBlockEntity faucet
                            && shouldKeepFaucetOverlayOpen(
                            faucet,
                            partialTick
                    )
            ) {
                continue;
            }

            renderFaucetOverlay(
                    face,
                    overlayConsumer,
                    pose,
                    packedLight,
                    packedOverlay
            );
        }
    }

    private static boolean shouldKeepFaucetOverlayOpen(
            FoundryFaucetBlockEntity faucet,
            float partialTick
    ) {
        /*
         * The door remains open for the complete active pouring period.
         */
        if (faucet.isPouring()) {
            return true;
        }

        /*
         * Once the shutdown animation has finished, the door is closed.
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
         * During shutdown, keep the door open only while the most recently
         * transferred molten metal is still visibly raising the Cauldron surface.
         *
         * As soon as the visual height catches up, the door closes and appears to
         * cut the remaining stream off at its source.
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
