package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.PIXEL;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.TOP_TEXTURE;

/**
 * Small dark top opening for Tanks toggled into intake hatch mode.
 *
 * This is intentionally an illusion: the Tank remains a closed molten storage
 * block internally, but the top shows a loading mouth for dropped ore.
 */
final class FoundryTankIntakeHatchRenderer {

    private static final float MIN =
            5.0f * PIXEL;

    private static final float MAX =
            11.0f * PIXEL;

    private static final float Y =
            1.003f;

    private FoundryTankIntakeHatchRenderer() {
    }

    static void render(
            FoundryTankBlockEntity tank,
            PoseStack.Pose pose,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (
                !tank.isIntakeHatchOpen()
                        || !tank.isTopTank()
        ) {
            return;
        }

        VertexConsumer consumer =
                bufferSource.getBuffer(
                        RenderType.entityTranslucent(
                                TOP_TEXTURE
                        )
                );

        renderDarkTopQuad(
                consumer,
                pose,
                packedLight,
                packedOverlay
        );
    }

    private static void renderDarkTopQuad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        addVertex(
                consumer,
                pose,
                MIN,
                Y,
                MIN,
                0.0f,
                0.0f,
                packedLight,
                packedOverlay
        );

        addVertex(
                consumer,
                pose,
                MIN,
                Y,
                MAX,
                0.0f,
                1.0f,
                packedLight,
                packedOverlay
        );

        addVertex(
                consumer,
                pose,
                MAX,
                Y,
                MAX,
                1.0f,
                1.0f,
                packedLight,
                packedOverlay
        );

        addVertex(
                consumer,
                pose,
                MAX,
                Y,
                MIN,
                1.0f,
                0.0f,
                packedLight,
                packedOverlay
        );
    }

    private static void addVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            int packedLight,
            int packedOverlay
    ) {
        consumer.addVertex(
                        pose.pose(),
                        x,
                        y,
                        z
                )
                .setColor(
                        0xDD050505
                )
                .setUv(
                        u,
                        v
                )
                .setOverlay(
                        packedOverlay
                )
                .setLight(
                        packedLight
                )
                .setNormal(
                        pose,
                        0.0f,
                        1.0f,
                        0.0f
                );
    }
}
