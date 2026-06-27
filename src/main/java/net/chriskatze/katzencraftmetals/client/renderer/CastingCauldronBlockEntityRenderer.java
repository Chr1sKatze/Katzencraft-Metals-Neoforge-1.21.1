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

    private static final float MOLTEN_FRAME_SIZE = 16.0f;
    private static final float COOLED_TEXTURE_SIZE = 16.0f;

    /*
     * The cooled layer is rendered very slightly outside the molten
     * layer so both can overlap without Z-fighting during the fade.
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
        if (cauldron.getLevel() == null) {
            return;
        }

        /*
         * Smooth the visible increase between the one-unit server
         * updates sent by the Faucet.
         */
        float displayedMoltenAmount =
                CastingCauldronFillSmoother.getDisplayedMoltenAmount(
                        cauldron,
                        partialTick
                );

        if (displayedMoltenAmount <= 0.0f) {
            return;
        }

        float fillPercentage = Mth.clamp(
                displayedMoltenAmount
                        / CastingCauldronBlockEntity.REQUIRED_MOLTEN_AMOUNT,
                0.0f,
                1.0f
        );

        float surfaceY = Mth.lerp(
                fillPercentage,
                MIN_Y,
                MAX_Y
        );

        /*
         * The cooled texture fades uniformly over the whole molten
         * texture. This is a simple texture cross-fade, not a moving
         * top-to-bottom cooling front.
         */
        float coolingBlend;

        if (cauldron.isCooled()) {
            coolingBlend = 1.0f;
        } else {
            coolingBlend = Mth.clamp(
                    (
                            cauldron.getCoolingProgress()
                                    + partialTick
                    )
                            / CastingCauldronBlockEntity.MAX_COOLING_PROGRESS,
                    0.0f,
                    1.0f
            );
        }

        /*
         * Smoothstep softens both the start and end of the fade.
         */
        coolingBlend =
                coolingBlend
                        * coolingBlend
                        * (3.0f - 2.0f * coolingBlend);

        MoltenIronAnimation.Frame animationFrame =
                MoltenIronAnimation.getFrame(
                        cauldron.getLevel().getGameTime()
                );

        float moltenFrameMinV =
                animationFrame.minV();

        float moltenFrameMaxV =
                animationFrame.maxV();

        poseStack.pushPose();

        PoseStack.Pose pose =
                poseStack.last();

        /*
         * Keep the molten texture fully visible underneath while the
         * cooled texture fades in over it.
         *
         * This avoids the metal becoming transparent during cooling.
         */
        if (!cauldron.isCooled()) {
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
                    1.0f,
                    0.0f
            );
        }

        /*
         * Fade the cooled texture in across the entire metal volume.
         */
        if (coolingBlend > 0.001f) {
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
                    coolingBlend,
                    COOLED_OVERLAY_OFFSET
            );
        }

        poseStack.popPose();
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
        float minX = MIN_X - geometryOffset;
        float maxX = MAX_X + geometryOffset;

        float minZ = MIN_Z - geometryOffset;
        float maxZ = MAX_Z + geometryOffset;

        float minY = MIN_Y - geometryOffset;
        float topY = surfaceY + geometryOffset;

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

        // TOP
        addVertex(consumer, pose, minX, topY, minZ,
                0.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                packedLight, packedOverlay,
                frameMinV, frameMaxV, alpha);

        addVertex(consumer, pose, minX, topY, maxZ,
                0.0f, depthUv,
                0.0f, 1.0f, 0.0f,
                packedLight, packedOverlay,
                frameMinV, frameMaxV, alpha);

        addVertex(consumer, pose, maxX, topY, maxZ,
                widthUv, depthUv,
                0.0f, 1.0f, 0.0f,
                packedLight, packedOverlay,
                frameMinV, frameMaxV, alpha);

        addVertex(consumer, pose, maxX, topY, minZ,
                widthUv, 0.0f,
                0.0f, 1.0f, 0.0f,
                packedLight, packedOverlay,
                frameMinV, frameMaxV, alpha);

        // NORTH
        addVertex(consumer, pose, minX, minY, minZ,
                0.0f, 1.0f,
                0.0f, 0.0f, -1.0f,
                packedLight, packedOverlay,
                frameMinV, frameMaxV, alpha);

        addVertex(consumer, pose, maxX, minY, minZ,
                widthUv, 1.0f,
                0.0f, 0.0f, -1.0f,
                packedLight, packedOverlay,
                frameMinV, frameMaxV, alpha);

        addVertex(consumer, pose, maxX, topY, minZ,
                widthUv, sideTopV,
                0.0f, 0.0f, -1.0f,
                packedLight, packedOverlay,
                frameMinV, frameMaxV, alpha);

        addVertex(consumer, pose, minX, topY, minZ,
                0.0f, sideTopV,
                0.0f, 0.0f, -1.0f,
                packedLight, packedOverlay,
                frameMinV, frameMaxV, alpha);

        // SOUTH
        addVertex(consumer, pose, maxX, minY, maxZ,
                0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                packedLight, packedOverlay,
                frameMinV, frameMaxV, alpha);

        addVertex(consumer, pose, minX, minY, maxZ,
                widthUv, 1.0f,
                0.0f, 0.0f, 1.0f,
                packedLight, packedOverlay,
                frameMinV, frameMaxV, alpha);

        addVertex(consumer, pose, minX, topY, maxZ,
                widthUv, sideTopV,
                0.0f, 0.0f, 1.0f,
                packedLight, packedOverlay,
                frameMinV, frameMaxV, alpha);

        addVertex(consumer, pose, maxX, topY, maxZ,
                0.0f, sideTopV,
                0.0f, 0.0f, 1.0f,
                packedLight, packedOverlay,
                frameMinV, frameMaxV, alpha);

        // WEST
        addVertex(consumer, pose, minX, minY, maxZ,
                0.0f, 1.0f,
                -1.0f, 0.0f, 0.0f,
                packedLight, packedOverlay,
                frameMinV, frameMaxV, alpha);

        addVertex(consumer, pose, minX, minY, minZ,
                depthUv, 1.0f,
                -1.0f, 0.0f, 0.0f,
                packedLight, packedOverlay,
                frameMinV, frameMaxV, alpha);

        addVertex(consumer, pose, minX, topY, minZ,
                depthUv, sideTopV,
                -1.0f, 0.0f, 0.0f,
                packedLight, packedOverlay,
                frameMinV, frameMaxV, alpha);

        addVertex(consumer, pose, minX, topY, maxZ,
                0.0f, sideTopV,
                -1.0f, 0.0f, 0.0f,
                packedLight, packedOverlay,
                frameMinV, frameMaxV, alpha);

        // EAST
        addVertex(consumer, pose, maxX, minY, minZ,
                0.0f, 1.0f,
                1.0f, 0.0f, 0.0f,
                packedLight, packedOverlay,
                frameMinV, frameMaxV, alpha);

        addVertex(consumer, pose, maxX, minY, maxZ,
                depthUv, 1.0f,
                1.0f, 0.0f, 0.0f,
                packedLight, packedOverlay,
                frameMinV, frameMaxV, alpha);

        addVertex(consumer, pose, maxX, topY, maxZ,
                depthUv, sideTopV,
                1.0f, 0.0f, 0.0f,
                packedLight, packedOverlay,
                frameMinV, frameMaxV, alpha);

        addVertex(consumer, pose, maxX, topY, minZ,
                0.0f, sideTopV,
                1.0f, 0.0f, 0.0f,
                packedLight, packedOverlay,
                frameMinV, frameMaxV, alpha);
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

        consumer.addVertex(
                        pose.pose(),
                        x,
                        y,
                        z
                )
                .setColor(
                        255,
                        255,
                        255,
                        alphaValue
                )
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
