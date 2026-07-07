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
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.WeakHashMap;

public class FoundryFaucetBlockEntityRenderer
        implements BlockEntityRenderer<FoundryFaucetBlockEntity> {

    private final Map<FoundryFaucetBlockEntity, FoundryFaucetStreamRenderState>
            streamRenderStates =
            new WeakHashMap<>();

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
                                context.metal().animatedTexture()
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
    }

    @Override
    public AABB getRenderBoundingBox(
            FoundryFaucetBlockEntity faucet
    ) {
        BlockPos pos =
                faucet.getBlockPos();

        return new AABB(
                pos.getX(),
                pos.getY()
                        - FoundryFaucetBlockEntity.MAX_CAULDRON_DISTANCE,
                pos.getZ(),
                pos.getX() + 1.0,
                pos.getY() + 1.0,
                pos.getZ() + 1.0
        );
    }

    @Override
    public boolean shouldRenderOffScreen(
            FoundryFaucetBlockEntity faucet
    ) {
        return true;
    }
}
