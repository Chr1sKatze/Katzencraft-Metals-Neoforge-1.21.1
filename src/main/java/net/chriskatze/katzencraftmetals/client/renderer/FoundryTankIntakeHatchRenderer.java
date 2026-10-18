package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.chriskatze.katzencraftmetals.block.custom.FoundryTankBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.TOP_TEXTURE;

/** Custom intake-hatch visual for the existing tank HATCH_OPEN state. */
public final class FoundryTankIntakeHatchRenderer {
    private static final ModelResourceLocation HATCH_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("katzencraftmetals", "block/foundry_hatch"));
    private static final float FULL_MIN = 0.0f;
    private static final float FULL_MAX = 1.0f;
    private static final float BASE_Y = 1.003f;
    private static final int FULL_TOP_COLOR = 0xFFFFFFFF;

    private FoundryTankIntakeHatchRenderer() {}

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(HATCH_MODEL);
    }

    static void render(Level level, BlockPos tankPos, BlockState tankState,
                       PoseStack.Pose pose, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        if (level == null || tankPos == null || tankState == null
                || !(tankState.getBlock() instanceof FoundryTankBlock)
                || !tankState.hasProperty(FoundryTankBlock.HATCH_OPEN)
                || !tankState.getValue(FoundryTankBlock.HATCH_OPEN)
                || level.getBlockState(tankPos.above()).getBlock() instanceof FoundryTankBlock) return;

        // Keep the established tank-top connection surface; only the raised hatch is replaced.
        renderFullTopCap(bufferSource.getBuffer(RenderType.entityCutoutNoCullZOffset(TOP_TEXTURE)),
                pose, packedLight, packedOverlay);

        Minecraft minecraft = Minecraft.getInstance();
        var model = minecraft.getModelManager().getModel(HATCH_MODEL);
        // renderModel emits every directional group, without neighboring-block face culling.
        minecraft.getBlockRenderer().getModelRenderer().renderModel(pose,
                bufferSource.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS)),
                tankState, model, 1, 1, 1, packedLight, packedOverlay,
                ModelData.EMPTY, RenderType.cutout());

        // The hatch has only its existing open/enabled state, no faucet pouring state.
        renderHatchOpening(bufferSource.getBuffer(FoundryFaucetOpeningRenderType.get()),
                pose, packedLight, packedOverlay);
    }

    static void renderHatchOpening(VertexConsumer consumer, PoseStack.Pose pose,
                                   int packedLight, int packedOverlay) {
        float min = 6.0f / 16.0f, max = 10.0f / 16.0f, y = 18.0f / 16.0f;
        // Four existing filled texture texels per side, excluding transparent sprite padding.
        float u0 = 6.0f / 16.0f, u1 = 10.0f / 16.0f;
        float v0 = 5.0f / 16.0f, v1 = 9.0f / 16.0f;
        FoundryTankCasingQuads.renderCasingQuad(consumer, pose,
                min,y,min, min,y,max, max,y,max, max,y,min,
                u0,v0, u0,v1, u1,v1, u1,v0,
                0,1,0, packedLight,packedOverlay);
    }

    static void renderBottomFaucetCap(
            VertexConsumer consumer,
            PoseStack poseStack,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();
        // Mirror the existing cap through the tank: y=1.003 becomes y=0.997 below it.
        poseStack.translate(0.0f, 2.0f, 1.0f);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0f));
        renderFullTopCap(consumer, poseStack.last(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderFullTopCap(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            int packedLight,
            int packedOverlay
    ) {
        renderTopRect(
                consumer,
                pose,
                FULL_MIN,
                FULL_MAX,
                FULL_MIN,
                FULL_MAX,
                BASE_Y,
                FULL_TOP_COLOR,
                packedLight,
                packedOverlay
        );
    }

    private static void renderTopRect(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float minX,
            float maxX,
            float minZ,
            float maxZ,
            float y,
            int color,
            int packedLight,
            int packedOverlay
    ) {
        addVertex(
                consumer,
                pose,
                minX,
                y,
                minZ,
                minX,
                minZ,
                color,
                packedLight,
                packedOverlay,
                0.0f,
                1.0f,
                0.0f
        );

        addVertex(
                consumer,
                pose,
                minX,
                y,
                maxZ,
                minX,
                maxZ,
                color,
                packedLight,
                packedOverlay,
                0.0f,
                1.0f,
                0.0f
        );

        addVertex(
                consumer,
                pose,
                maxX,
                y,
                maxZ,
                maxX,
                maxZ,
                color,
                packedLight,
                packedOverlay,
                0.0f,
                1.0f,
                0.0f
        );

        addVertex(
                consumer,
                pose,
                maxX,
                y,
                minZ,
                maxX,
                minZ,
                color,
                packedLight,
                packedOverlay,
                0.0f,
                1.0f,
                0.0f
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
            int color,
            int packedLight,
            int packedOverlay,
            float normalX,
            float normalY,
            float normalZ
    ) {
        consumer.addVertex(
                        pose.pose(),
                        x,
                        y,
                        z
                )
                .setColor(
                        color
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
                        normalX,
                        normalY,
                        normalZ
                );
    }
}
