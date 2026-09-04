package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock;
import net.chriskatze.katzencraftmetals.block.entity.FoundryFaucetBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.WeakHashMap;

public class FoundryFaucetBlockEntityRenderer
        implements BlockEntityRenderer<FoundryFaucetBlockEntity> {

    private final Map<FoundryFaucetBlockEntity, FoundryFaucetStreamRenderState>
            streamRenderStates =
            new WeakHashMap<>();

    private final FoundryFaucetPourParticleEffects particleEffects =
            new FoundryFaucetPourParticleEffects();

    public FoundryFaucetBlockEntityRenderer(
            BlockEntityRendererProvider.Context context
    ) {
    }

    @Override
    public void render(
            FoundryFaucetBlockEntity faucet,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (faucet.getLevel() == null) {
            return;
        }

        /*
         * Restore the old translucent black entrance shadow.
         *
         * This must happen BEFORE the stream renderer's idle early-return:
         * an idle Faucet is exactly when the entrance is supposed to appear
         * closed/dark.
         *
         * The Faucet already owns this BER, so there is no Tank-BE renderer and
         * no per-Tank face scan involved.
         */
        FoundryTankFaucetOverlayRenderer.renderForFaucet(
                faucet,
                partialTick,
                poseStack,
                bufferSource,
                packedOverlay
        );

        float animationProgress =
                FoundryFaucetStreamAnimation
                        .updateAndGetAnimationProgress(
                                faucet,
                                partialTick,
                                streamRenderStates
                        );

        FoundryFaucetStreamRenderState renderState =
                streamRenderStates.get(
                        faucet
                );

        boolean pouring =
                faucet.isPouring();

        float shutdownProgress =
                pouring
                        ? 0.0f
                        : animationProgress;

        if (
                !pouring
                        && shutdownProgress >= 0.9999f
        ) {
            if (faucet.getStreamAnimationStep() <= 0) {
                streamRenderStates.remove(
                        faucet
                );
            }

            return;
        }

        FoundryFaucetRenderContext context =
                FoundryFaucetRenderContext.create(
                        faucet,
                        partialTick
                );

        if (context == null) {
            return;
        }

        FoundryFaucetDripStyle dripStyle =
                renderState != null
                        ? renderState.dripStyle
                        : FoundryFaucetDripStyle.HORIZONTAL_ONLY;

        float shutdownStartProgress =
                renderState != null
                        ? renderState.drainStartProgress
                        : 0.0f;

        FoundryFaucetStreamGeometry geometry =
                FoundryFaucetStreamGeometry.resolve(
                        pouring,
                        animationProgress,
                        shutdownStartProgress,
                        context.cauldronSurfaceY()
                );

        MoltenIronAnimation.Frame animationFrame =
                MoltenIronAnimation.getFrame(
                        faucet.getLevel().getGameTime()
                );

        VertexConsumer consumer =
                bufferSource.getBuffer(
                        RenderType.entityTranslucent(
                                context.metal().flowingTexture()
                        )
                );

        poseStack.pushPose();

        FoundryFaucetRenderTransform.rotateForFacing(
                poseStack,
                faucet.getBlockState()
                        .getValue(
                                FoundryFaucetBlock.FACING
                        )
        );

        FoundryFaucetStreamRenderer.render(
                consumer,
                poseStack.last(),
                geometry,
                dripStyle,
                context.dripDisappearY(),
                packedOverlay,
                animationFrame.minV(),
                animationFrame.maxV(),
                255
        );

        poseStack.popPose();

        particleEffects.spawnWhilePouring(
                faucet,
                context,
                animationProgress
        );
    }

    @Override
    public AABB getRenderBoundingBox(
            FoundryFaucetBlockEntity faucet
    ) {
        BlockPos faucetPos =
                faucet.getBlockPos();

        BlockPos tankPos =
                getAttachedTankPositionForBounds(
                        faucet,
                        faucetPos
                );

        int minX =
                Math.min(
                        faucetPos.getX(),
                        tankPos.getX()
                );

        int maxX =
                Math.max(
                        faucetPos.getX(),
                        tankPos.getX()
                );

        int minZ =
                Math.min(
                        faucetPos.getZ(),
                        tankPos.getZ()
                );

        int maxZ =
                Math.max(
                        faucetPos.getZ(),
                        tankPos.getZ()
                );

        return new AABB(
                minX,
                faucetPos.getY()
                        - FoundryFaucetBlockEntity.MAX_CAULDRON_DISTANCE,
                minZ,
                maxX + 1.0,
                faucetPos.getY() + 1.0,
                maxZ + 1.0
        );
    }

    /**
     * Include the attached Tank face in this BER's render bounds because the
     * entrance shadow is rendered one block behind the Faucet.
     */
    private static BlockPos getAttachedTankPositionForBounds(
            FoundryFaucetBlockEntity faucet,
            BlockPos faucetPos
    ) {
        BlockState state =
                faucet.getBlockState();

        if (!state.hasProperty(FoundryFaucetBlock.FACING)) {
            return faucetPos;
        }

        Direction facing =
                state.getValue(
                        FoundryFaucetBlock.FACING
                );

        return faucetPos.relative(
                facing.getOpposite()
        );
    }

    @Override
    public boolean shouldRenderOffScreen(
            FoundryFaucetBlockEntity faucet
    ) {
        return true;
    }
}
