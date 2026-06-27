package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.entity.CastingCauldronBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class CastingCauldronBlockEntityRenderer
        implements BlockEntityRenderer<CastingCauldronBlockEntity> {

    private static final ResourceLocation MOLTEN_IRON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/block/molten_iron.png"
            );

    private static final ResourceLocation COOLED_IRON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/block/cooled_iron.png"
            );

    /*
     * molten_iron.png:
     *
     * 16 pixels wide
     * 320 pixels high
     * 320 / 16 = 20 animation frames
     */
    private static final int MOLTEN_FRAME_COUNT = 20;

    /*
     * One frame every four game ticks.
     */
    private static final int MOLTEN_FRAME_TIME = 4;

    private static final float MOLTEN_FRAME_SIZE = 16.0f;
    private static final float COOLED_TEXTURE_SIZE = 16.0f;

    /*
     * Small outward offset for the cooled overlay.
     *
     * The two textures are cross-faded during cooling. Rendering
     * the cooled layer very slightly outside the molten layer avoids
     * Z-fighting while remaining visually indistinguishable in size.
     */
    private static final float COOLED_OVERLAY_OFFSET = 0.0005f;

    private static final float MIN_X = 2.05f / 16.0f;
    private static final float MAX_X = 13.95f / 16.0f;

    private static final float MIN_Z = 2.05f / 16.0f;
    private static final float MAX_Z = 13.95f / 16.0f;

    private static final float MIN_Y = 4.05f / 16.0f;
    private static final float MAX_Y = 14.25f / 16.0f;

    public CastingCauldronBlockEntityRenderer(
            BlockEntityRendererProvider.Context context
    ) {
    }

    @Override
    public void render(
            CastingCauldronBlockEntity cauldron,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (cauldron.isEmpty()) {
            return;
        }

        float fillPercentage = Mth.clamp(
                (float) cauldron.getMoltenAmount()
                        / CastingCauldronBlockEntity.REQUIRED_MOLTEN_AMOUNT,
                0.0f,
                1.0f
        );

        if (fillPercentage <= 0.0f) {
            return;
        }

        float surfaceY = Mth.lerp(
                fillPercentage,
                MIN_Y,
                MAX_Y
        );

        /*
         * Convert cooling progress into a smooth cross-fade.
         *
         * smoothstep keeps the beginning and end of the transition
         * softer than a plain linear blend.
         */
        float coolingBlend = Mth.clamp(
                (float) cauldron.getCoolingProgress()
                        / CastingCauldronBlockEntity.MAX_COOLING_PROGRESS,
                0.0f,
                1.0f
        );

        if (cauldron.isCooled()) {
            coolingBlend = 1.0f;
        }

        coolingBlend =
                coolingBlend
                        * coolingBlend
                        * (3.0f - 2.0f * coolingBlend);

        float moltenAlpha =
                1.0f - coolingBlend;

        float cooledAlpha =
                coolingBlend;

        long gameTime =
                cauldron.getLevel() == null
                        ? 0L
                        : cauldron.getLevel().getGameTime();

        int moltenFrame =
                getPingPongFrame(gameTime);

        float moltenFrameMinV =
                (float) moltenFrame
                        / MOLTEN_FRAME_COUNT;

        float moltenFrameMaxV =
                (float) (moltenFrame + 1)
                        / MOLTEN_FRAME_COUNT;

        poseStack.pushPose();

        PoseStack.Pose pose =
                poseStack.last();

        /*
         * Render the animated molten texture first.
         */
        if (moltenAlpha > 0.001f) {
            VertexConsumer moltenConsumer =
                    bufferSource.getBuffer(
                            RenderType.entityTranslucent(
                                    MOLTEN_IRON_TEXTURE
                            )
                    );

            renderMetalVolume(
                    moltenConsumer,
                    pose,
                    surfaceY,
                    MOLTEN_FRAME_SIZE,
                    moltenFrameMinV,
                    moltenFrameMaxV,
                    LightTexture.FULL_BRIGHT,
                    packedOverlay,
                    moltenAlpha,
                    0.0f
            );
        }

        /*
         * Fade the cooled texture in on top of the molten texture.
         */
        if (cooledAlpha > 0.001f) {
            VertexConsumer cooledConsumer =
                    bufferSource.getBuffer(
                            RenderType.entityTranslucent(
                                    COOLED_IRON_TEXTURE
                            )
                    );

            renderMetalVolume(
                    cooledConsumer,
                    pose,
                    surfaceY,
                    COOLED_TEXTURE_SIZE,
                    0.0f,
                    1.0f,
                    packedLight,
                    packedOverlay,
                    cooledAlpha,
                    COOLED_OVERLAY_OFFSET
            );
        }

        poseStack.popPose();
    }

    /*
     * Plays the frames forwards and then backwards:
     *
     * 0, 1, 2, ... 19, 18, 17, ... 1, 0
     *
     * This avoids the visible jump from the final frame directly
     * back to the first frame when the texture is not perfectly
     * seamless.
     */
    private static int getPingPongFrame(
            long gameTime
    ) {
        if (MOLTEN_FRAME_COUNT <= 1) {
            return 0;
        }

        int cycleLength =
                MOLTEN_FRAME_COUNT * 2 - 2;

        int cycleFrame =
                (int) (
                        gameTime / MOLTEN_FRAME_TIME
                                % cycleLength
                );

        if (cycleFrame < MOLTEN_FRAME_COUNT) {
            return cycleFrame;
        }

        return cycleLength - cycleFrame;
    }

    private static void renderMetalVolume(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float surfaceY,
            float textureSize,
            float frameMinV,
            float frameMaxV,
            int packedLight,
            int packedOverlay,
            float alpha,
            float geometryOffset
    ) {
        float minX =
                MIN_X - geometryOffset;

        float maxX =
                MAX_X + geometryOffset;

        float minZ =
                MIN_Z - geometryOffset;

        float maxZ =
                MAX_Z + geometryOffset;

        float minY =
                MIN_Y - geometryOffset;

        float topY =
                surfaceY + geometryOffset;

        float uvScale =
                16.0f / textureSize;

        float widthUv =
                (MAX_X - MIN_X) * uvScale;

        float depthUv =
                (MAX_Z - MIN_Z) * uvScale;

        float liquidHeightUv =
                (surfaceY - MIN_Y) * uvScale;

        float sideTopV =
                1.0f - liquidHeightUv;

        // =========================
        // TOP SURFACE
        // =========================

        addVertex(
                consumer,
                pose,
                minX,
                topY,
                minZ,
                0.0f,
                0.0f,
                0.0f,
                1.0f,
                0.0f,
                packedLight,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        addVertex(
                consumer,
                pose,
                minX,
                topY,
                maxZ,
                0.0f,
                depthUv,
                0.0f,
                1.0f,
                0.0f,
                packedLight,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        addVertex(
                consumer,
                pose,
                maxX,
                topY,
                maxZ,
                widthUv,
                depthUv,
                0.0f,
                1.0f,
                0.0f,
                packedLight,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        addVertex(
                consumer,
                pose,
                maxX,
                topY,
                minZ,
                widthUv,
                0.0f,
                0.0f,
                1.0f,
                0.0f,
                packedLight,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        // =========================
        // NORTH SIDE
        // =========================

        addVertex(
                consumer,
                pose,
                minX,
                minY,
                minZ,
                0.0f,
                1.0f,
                0.0f,
                0.0f,
                -1.0f,
                packedLight,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        addVertex(
                consumer,
                pose,
                maxX,
                minY,
                minZ,
                widthUv,
                1.0f,
                0.0f,
                0.0f,
                -1.0f,
                packedLight,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        addVertex(
                consumer,
                pose,
                maxX,
                topY,
                minZ,
                widthUv,
                sideTopV,
                0.0f,
                0.0f,
                -1.0f,
                packedLight,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        addVertex(
                consumer,
                pose,
                minX,
                topY,
                minZ,
                0.0f,
                sideTopV,
                0.0f,
                0.0f,
                -1.0f,
                packedLight,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        // =========================
        // SOUTH SIDE
        // =========================

        addVertex(
                consumer,
                pose,
                maxX,
                minY,
                maxZ,
                0.0f,
                1.0f,
                0.0f,
                0.0f,
                1.0f,
                packedLight,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        addVertex(
                consumer,
                pose,
                minX,
                minY,
                maxZ,
                widthUv,
                1.0f,
                0.0f,
                0.0f,
                1.0f,
                packedLight,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        addVertex(
                consumer,
                pose,
                minX,
                topY,
                maxZ,
                widthUv,
                sideTopV,
                0.0f,
                0.0f,
                1.0f,
                packedLight,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        addVertex(
                consumer,
                pose,
                maxX,
                topY,
                maxZ,
                0.0f,
                sideTopV,
                0.0f,
                0.0f,
                1.0f,
                packedLight,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        // =========================
        // WEST SIDE
        // =========================

        addVertex(
                consumer,
                pose,
                minX,
                minY,
                maxZ,
                0.0f,
                1.0f,
                -1.0f,
                0.0f,
                0.0f,
                packedLight,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        addVertex(
                consumer,
                pose,
                minX,
                minY,
                minZ,
                depthUv,
                1.0f,
                -1.0f,
                0.0f,
                0.0f,
                packedLight,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        addVertex(
                consumer,
                pose,
                minX,
                topY,
                minZ,
                depthUv,
                sideTopV,
                -1.0f,
                0.0f,
                0.0f,
                packedLight,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        addVertex(
                consumer,
                pose,
                minX,
                topY,
                maxZ,
                0.0f,
                sideTopV,
                -1.0f,
                0.0f,
                0.0f,
                packedLight,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        // =========================
        // EAST SIDE
        // =========================

        addVertex(
                consumer,
                pose,
                maxX,
                minY,
                minZ,
                0.0f,
                1.0f,
                1.0f,
                0.0f,
                0.0f,
                packedLight,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        addVertex(
                consumer,
                pose,
                maxX,
                minY,
                maxZ,
                depthUv,
                1.0f,
                1.0f,
                0.0f,
                0.0f,
                packedLight,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        addVertex(
                consumer,
                pose,
                maxX,
                topY,
                maxZ,
                depthUv,
                sideTopV,
                1.0f,
                0.0f,
                0.0f,
                packedLight,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );

        addVertex(
                consumer,
                pose,
                maxX,
                topY,
                minZ,
                0.0f,
                sideTopV,
                1.0f,
                0.0f,
                0.0f,
                packedLight,
                packedOverlay,
                frameMinV,
                frameMaxV,
                alpha
        );
    }

    private static void addVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float localV,
            float normalX,
            float normalY,
            float normalZ,
            int packedLight,
            int packedOverlay,
            float frameMinV,
            float frameMaxV,
            float alpha
    ) {
        float textureV =
                Mth.lerp(
                        localV,
                        frameMinV,
                        frameMaxV
                );

        int alphaValue =
                Mth.clamp(
                        Math.round(alpha * 255.0f),
                        0,
                        255
                );

        int color =
                alphaValue << 24
                        | 0x00FFFFFF;

        consumer.addVertex(
                        pose.pose(),
                        x,
                        y,
                        z
                )
                .setColor(color)
                .setUv(u, textureV)
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(
                        pose,
                        normalX,
                        normalY,
                        normalZ
                );
    }
}
