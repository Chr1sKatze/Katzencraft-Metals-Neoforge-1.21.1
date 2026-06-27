package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.chriskatze.katzencraftmetals.block.entity.FoundryFaucetBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.WeakHashMap;

public class FoundryTankBlockEntityRenderer
        implements BlockEntityRenderer<FoundryTankBlockEntity> {

    private static final ResourceLocation MOLTEN_IRON_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    KatzencraftMetalsMod.MODID,
                    "textures/block/molten_iron.png"
            );

    /*
     * Every observed increase is animated over eight ticks.
     *
     * This works for one Tank and for wide Tank layers where one
     * six-unit insertion is visually shared across several blocks.
     */
    private static final float RISE_ANIMATION_TICKS = 8.0f;

    /*
     * Each server-side Faucet transfer updates the Tank once every
     * TRANSFER_INTERVAL ticks.
     *
     * The renderer animates every observed decrease over exactly that
     * interval. Therefore:
     *
     * 1 Faucet  removing 1 unit -> 1 unit over 2 ticks
     * 2 Faucets removing 2 units -> 2 units over 2 ticks
     * 3 Faucets removing 3 units -> 3 units over 2 ticks
     *
     * The visual drain speed automatically matches however many
     * Faucets are drawing from the Tank.
     */
    private static final float DRAIN_ANIMATION_TICKS =
            FoundryFaucetBlockEntity.TRANSFER_INTERVAL;

    /*
     * Block-entity renderer instances are shared, so each Tank needs
     * its own remembered visual fill amount.
     *
     * WeakHashMap allows removed Tank block entities to be discarded
     * automatically instead of being retained forever.
     */
    private final Map<FoundryTankBlockEntity, TankRenderState> renderStates =
            new WeakHashMap<>();

    /*
     * Interior liquid bounds.
     *
     * These sit just inside the inner faces of the tank model.
     */
    private static final float MIN_X = 0.12f / 16.0f;
    private static final float MAX_X = 15.88f / 16.0f;

    private static final float MIN_Z = 0.12f / 16.0f;
    private static final float MAX_Z = 15.88f / 16.0f;

    private static final float MIN_Y = 0.12f / 16.0f;
    private static final float MAX_Y = 15.88f / 16.0f;

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

        float displayedMoltenAmount =
                getDisplayedMoltenAmount(
                        tank,
                        partialTick
                );

        if (displayedMoltenAmount <= 0.0f) {
            return;
        }

        float fillPercentage = Mth.clamp(
                displayedMoltenAmount
                        / FoundryTankBlockEntity.CAPACITY,
                0.0f,
                1.0f
        );

        float surfaceY = Mth.lerp(
                fillPercentage,
                MIN_Y,
                MAX_Y
        );

        MoltenIronAnimation.Frame animationFrame =
                MoltenIronAnimation.getFrame(
                        tank.getLevel().getGameTime()
                );

        float frameMinV =
                animationFrame.minV();

        float frameMaxV =
                animationFrame.maxV();

        VertexConsumer consumer =
                bufferSource.getBuffer(
                        RenderType.entityTranslucent(
                                MOLTEN_IRON_TEXTURE
                        )
                );

        poseStack.pushPose();

        PoseStack.Pose pose =
                poseStack.last();

        renderLiquidVolume(
                consumer,
                pose,
                surfaceY,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        poseStack.popPose();
    }

    /*
     * Smooth both filling and draining for this individual Tank block.
     *
     * The target value already accounts for:
     *
     * - the complete network amount
     * - bottom-to-top layer filling
     * - the number of Tanks in the current horizontal layer
     * - any number of simultaneously running Faucets
     */
    private float getDisplayedMoltenAmount(
            FoundryTankBlockEntity tank,
            float partialTick
    ) {
        double currentRenderTime =
                tank.getLevel().getGameTime()
                        + partialTick;

        float targetAmount =
                tank.getLocalVisualMoltenAmount();

        TankRenderState renderState =
                renderStates.computeIfAbsent(
                        tank,
                        ignored -> new TankRenderState(
                                targetAmount,
                                currentRenderTime
                        )
                );

        if (
                Math.abs(
                        targetAmount
                                - renderState.lastTargetAmount
                ) > 0.00001f
        ) {
            /*
             * Continue smoothly from the currently displayed value
             * whenever a new server-side target arrives.
             */
            renderState.transitionStartAmount =
                    renderState.displayedAmount;

            renderState.transitionTargetAmount =
                    targetAmount;

            renderState.transitionStartTime =
                    currentRenderTime;

            renderState.transitionDuration =
                    targetAmount
                            > renderState.lastTargetAmount
                            ? RISE_ANIMATION_TICKS
                            : DRAIN_ANIMATION_TICKS;

            renderState.lastTargetAmount =
                    targetAmount;
        }

        float progress = Mth.clamp(
                (float) (
                        (
                                currentRenderTime
                                        - renderState.transitionStartTime
                        )
                                / renderState.transitionDuration
                ),
                0.0f,
                1.0f
        );

        renderState.displayedAmount =
                Mth.lerp(
                        progress,
                        renderState.transitionStartAmount,
                        renderState.transitionTargetAmount
                );

        if (progress >= 1.0f) {
            renderState.displayedAmount =
                    renderState.transitionTargetAmount;
        }

        return renderState.displayedAmount;
    }

    private static void renderLiquidVolume(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float surfaceY,
            int packedOverlay,
            float frameMinV,
            float frameMaxV
    ) {
        float widthUv =
                MAX_X - MIN_X;

        float depthUv =
                MAX_Z - MIN_Z;

        float heightUv =
                surfaceY - MIN_Y;

        float sideTopV =
                1.0f - heightUv;

        // =========================
        // TOP SURFACE
        // =========================

        addVertex(
                consumer,
                pose,
                MIN_X,
                surfaceY,
                MIN_Z,
                0.0f,
                0.0f,
                0.0f,
                1.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        addVertex(
                consumer,
                pose,
                MIN_X,
                surfaceY,
                MAX_Z,
                0.0f,
                depthUv,
                0.0f,
                1.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        addVertex(
                consumer,
                pose,
                MAX_X,
                surfaceY,
                MAX_Z,
                widthUv,
                depthUv,
                0.0f,
                1.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        addVertex(
                consumer,
                pose,
                MAX_X,
                surfaceY,
                MIN_Z,
                widthUv,
                0.0f,
                0.0f,
                1.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        // =========================
        // NORTH SIDE
        // =========================

        addVertex(
                consumer,
                pose,
                MIN_X,
                MIN_Y,
                MIN_Z,
                0.0f,
                1.0f,
                0.0f,
                0.0f,
                -1.0f,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        addVertex(
                consumer,
                pose,
                MAX_X,
                MIN_Y,
                MIN_Z,
                widthUv,
                1.0f,
                0.0f,
                0.0f,
                -1.0f,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        addVertex(
                consumer,
                pose,
                MAX_X,
                surfaceY,
                MIN_Z,
                widthUv,
                sideTopV,
                0.0f,
                0.0f,
                -1.0f,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        addVertex(
                consumer,
                pose,
                MIN_X,
                surfaceY,
                MIN_Z,
                0.0f,
                sideTopV,
                0.0f,
                0.0f,
                -1.0f,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        // =========================
        // SOUTH SIDE
        // =========================

        addVertex(
                consumer,
                pose,
                MAX_X,
                MIN_Y,
                MAX_Z,
                0.0f,
                1.0f,
                0.0f,
                0.0f,
                1.0f,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        addVertex(
                consumer,
                pose,
                MIN_X,
                MIN_Y,
                MAX_Z,
                widthUv,
                1.0f,
                0.0f,
                0.0f,
                1.0f,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        addVertex(
                consumer,
                pose,
                MIN_X,
                surfaceY,
                MAX_Z,
                widthUv,
                sideTopV,
                0.0f,
                0.0f,
                1.0f,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        addVertex(
                consumer,
                pose,
                MAX_X,
                surfaceY,
                MAX_Z,
                0.0f,
                sideTopV,
                0.0f,
                0.0f,
                1.0f,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        // =========================
        // WEST SIDE
        // =========================

        addVertex(
                consumer,
                pose,
                MIN_X,
                MIN_Y,
                MAX_Z,
                0.0f,
                1.0f,
                -1.0f,
                0.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        addVertex(
                consumer,
                pose,
                MIN_X,
                MIN_Y,
                MIN_Z,
                depthUv,
                1.0f,
                -1.0f,
                0.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        addVertex(
                consumer,
                pose,
                MIN_X,
                surfaceY,
                MIN_Z,
                depthUv,
                sideTopV,
                -1.0f,
                0.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        addVertex(
                consumer,
                pose,
                MIN_X,
                surfaceY,
                MAX_Z,
                0.0f,
                sideTopV,
                -1.0f,
                0.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        // =========================
        // EAST SIDE
        // =========================

        addVertex(
                consumer,
                pose,
                MAX_X,
                MIN_Y,
                MIN_Z,
                0.0f,
                1.0f,
                1.0f,
                0.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        addVertex(
                consumer,
                pose,
                MAX_X,
                MIN_Y,
                MAX_Z,
                depthUv,
                1.0f,
                1.0f,
                0.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        addVertex(
                consumer,
                pose,
                MAX_X,
                surfaceY,
                MAX_Z,
                depthUv,
                sideTopV,
                1.0f,
                0.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV
        );

        addVertex(
                consumer,
                pose,
                MAX_X,
                surfaceY,
                MIN_Z,
                0.0f,
                sideTopV,
                1.0f,
                0.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV
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
            int packedOverlay,
            float frameMinV,
            float frameMaxV
    ) {
        float textureV =
                Mth.lerp(
                        localV,
                        frameMinV,
                        frameMaxV
                );

        consumer.addVertex(
                        pose.pose(),
                        x,
                        y,
                        z
                )
                .setColor(0xFFFFFFFF)
                .setUv(
                        u,
                        textureV
                )
                .setOverlay(packedOverlay)
                .setLight(
                        LightTexture.FULL_BRIGHT
                )
                .setNormal(
                        pose,
                        normalX,
                        normalY,
                        normalZ
                );
    }

    private static final class TankRenderState {

        private float displayedAmount;
        private float lastTargetAmount;

        private float transitionStartAmount;
        private float transitionTargetAmount;

        private double transitionStartTime;
        private float transitionDuration;

        private TankRenderState(
                float initialAmount,
                double currentRenderTime
        ) {
            this.displayedAmount =
                    initialAmount;

            this.lastTargetAmount =
                    initialAmount;

            this.transitionStartAmount =
                    initialAmount;

            this.transitionTargetAmount =
                    initialAmount;

            this.transitionStartTime =
                    currentRenderTime;

            this.transitionDuration =
                    1.0f;
        }
    }

    @Override
    public boolean shouldRenderOffScreen(
            FoundryTankBlockEntity tank
    ) {
        return true;
    }
}
