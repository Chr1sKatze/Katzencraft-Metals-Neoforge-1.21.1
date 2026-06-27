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
     * molten_iron.png:
     *
     * 16 pixels wide
     * 320 pixels high
     * 320 / 16 = 20 animation frames
     */
    private static final int MOLTEN_FRAME_COUNT = 20;
    private static final int MOLTEN_FRAME_TIME = 4;

    /*
     * One smelted raw iron adds 6 molten units.
     *
     * Visually raise those 6 units over 8 ticks (0.4 seconds)
     * instead of jumping to the new height immediately.
     */
    private static final float RISE_ANIMATION_TICKS = 8.0f;
    private static final float RISE_UNITS_PER_TICK =
            6.0f / RISE_ANIMATION_TICKS;

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
                        / tank.getCapacity(),
                0.0f,
                1.0f
        );

        float surfaceY = Mth.lerp(
                fillPercentage,
                MIN_Y,
                MAX_Y
        );

        long gameTime =
                tank.getLevel() == null
                        ? 0L
                        : tank.getLevel().getGameTime();

        int frame =
                getPingPongFrame(gameTime);

        float frameMinV =
                (float) frame
                        / MOLTEN_FRAME_COUNT;

        float frameMaxV =
                (float) (frame + 1)
                        / MOLTEN_FRAME_COUNT;

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
     * Smooth both increases and decreases in the Tank's visible
     * liquid level.
     *
     * Rising uses the chosen ore-smelting animation speed.
     *
     * Draining does not assume that only one Faucet is active.
     * Whenever the real Tank amount decreases, the complete observed
     * decrease is animated over one Faucet transfer interval.
     *
     * A decrease of 1, 2, or 3 units therefore automatically renders
     * one, two, or three times as fast.
     */
    private float getDisplayedMoltenAmount(
            FoundryTankBlockEntity tank,
            float partialTick
    ) {
        double currentRenderTime =
                tank.getLevel().getGameTime()
                        + partialTick;

        TankRenderState renderState =
                renderStates.computeIfAbsent(
                        tank,
                        ignored -> new TankRenderState(
                                tank.getMoltenAmount(),
                                currentRenderTime
                        )
                );

        double elapsedTicks = Math.max(
                0.0,
                currentRenderTime
                        - renderState.lastRenderTime
        );

        renderState.lastRenderTime =
                currentRenderTime;

        float targetAmount =
                tank.getMoltenAmount();

        /*
         * Detect a newly received server-side decrease.
         *
         * Start a fresh interpolation from the currently displayed
         * amount to the newest real amount. If multiple Faucets have
         * transferred during the same update, targetAmount will have
         * fallen by multiple units and the animation covers that whole
         * distance within the same two-tick interval.
         */
        if (targetAmount < renderState.lastTargetAmount) {
            renderState.drainStartAmount =
                    renderState.displayedAmount;

            renderState.drainTargetAmount =
                    targetAmount;

            renderState.drainStartTime =
                    currentRenderTime;

            renderState.draining =
                    true;
        } else if (targetAmount > renderState.lastTargetAmount) {
            /*
             * A new insertion cancels any old drain interpolation.
             */
            renderState.draining =
                    false;
        }

        renderState.lastTargetAmount =
                targetAmount;

        if (targetAmount > renderState.displayedAmount) {
            float maximumRise =
                    RISE_UNITS_PER_TICK
                            * (float) elapsedTicks;

            renderState.displayedAmount =
                    Math.min(
                            targetAmount,
                            renderState.displayedAmount
                                    + maximumRise
                    );
        } else if (targetAmount < renderState.displayedAmount) {
            /*
             * This fallback also handles a Tank loaded with a visual
             * amount above its real amount.
             */
            if (!renderState.draining) {
                renderState.drainStartAmount =
                        renderState.displayedAmount;

                renderState.drainTargetAmount =
                        targetAmount;

                renderState.drainStartTime =
                        currentRenderTime;

                renderState.draining =
                        true;
            }

            float drainProgress = Mth.clamp(
                    (float) (
                            (
                                    currentRenderTime
                                            - renderState.drainStartTime
                            )
                                    / DRAIN_ANIMATION_TICKS
                    ),
                    0.0f,
                    1.0f
            );

            renderState.displayedAmount =
                    Mth.lerp(
                            drainProgress,
                            renderState.drainStartAmount,
                            renderState.drainTargetAmount
                    );

            if (drainProgress >= 1.0f) {
                renderState.displayedAmount =
                        targetAmount;

                renderState.draining =
                        false;
            }
        } else {
            renderState.draining =
                    false;
        }

        return renderState.displayedAmount;
    }

    /*
     * Plays the texture forwards and backwards to avoid a visible
     * jump between the last and first animation frames.
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

        private float drainStartAmount;
        private float drainTargetAmount;
        private double drainStartTime;
        private boolean draining;

        private double lastRenderTime;

        private TankRenderState(
                float displayedAmount,
                double currentRenderTime
        ) {
            this.displayedAmount =
                    displayedAmount;

            this.lastTargetAmount =
                    displayedAmount;

            this.drainStartAmount =
                    displayedAmount;

            this.drainTargetAmount =
                    displayedAmount;

            this.drainStartTime =
                    currentRenderTime;

            this.draining =
                    false;

            this.lastRenderTime =
                    currentRenderTime;
        }
    }

    @Override
    public boolean shouldRenderOffScreen(
            FoundryTankBlockEntity tank
    ) {
        return true;
    }
}
