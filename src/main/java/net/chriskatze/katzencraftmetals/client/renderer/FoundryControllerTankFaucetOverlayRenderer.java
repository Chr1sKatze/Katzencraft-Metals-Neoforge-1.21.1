package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock;
import net.chriskatze.katzencraftmetals.block.entity.FoundryFaucetBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.FAUCET_OVERLAY_OFFSET;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.FAUCET_OVERLAY_TEXTURE;

/** Faucet entrance shadow for Controller-rendered active Tank faces. */
final class FoundryControllerTankFaucetOverlayRenderer {

    private FoundryControllerTankFaucetOverlayRenderer() {
    }

    static void render(
            Level level,
            BlockPos tankPos,
            Set<BlockPos> structure,
            PoseStack.Pose pose,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        VertexConsumer consumer = null;

        for (Direction face : Direction.Plane.HORIZONTAL) {
            if (structure.contains(tankPos.relative(face))) {
                continue;
            }

            BlockPos faucetPos = tankPos.relative(face);
            BlockState faucetState = level.getBlockState(faucetPos);

            if (
                    !(faucetState.getBlock() instanceof FoundryFaucetBlock)
                            || !faucetState.hasProperty(FoundryFaucetBlock.FACING)
                            || faucetState.getValue(FoundryFaucetBlock.FACING) != face
            ) {
                continue;
            }

            if (
                    level.getBlockEntity(faucetPos) instanceof FoundryFaucetBlockEntity faucet
                            && faucet.isPouring()
            ) {
                continue;
            }

            if (consumer == null) {
                consumer = bufferSource.getBuffer(
                        RenderType.entityTranslucent(FAUCET_OVERLAY_TEXTURE)
                );
            }

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
}
