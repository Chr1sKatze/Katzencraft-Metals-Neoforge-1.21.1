package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.block.entity.FoundryControllerBlockEntity;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.CONTACT_RIM_GLASS_FADE_DISTANCE;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.CONTACT_RIM_MAX_ALPHA;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.CONTACT_RIM_LARGE_SURFACE_CELLS;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.CONTACT_RIM_MEDIUM_SURFACE_CELLS;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.CONTACT_RIM_WIDTH_LARGE;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.CONTACT_RIM_WIDTH_MEDIUM;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.CONTACT_RIM_WIDTH_SMALL;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.CONTACT_RIM_Y_OFFSET;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.HOT_SPOT_GLASS_FADE_DISTANCE;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.HOT_SPOT_MAX_INTENSITY;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.HOT_SPOT_MAX_LIFETIME_TICKS;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.HOT_SPOT_MIN_INTENSITY;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.HOT_SPOT_MIN_LIFETIME_TICKS;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.HOT_SPOT_MARGIN;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.HOT_SPOT_MAX_ALPHA;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.HOT_SPOT_MAX_SIZE;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.HOT_SPOT_MAX_TARGET_COUNT;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.HOT_SPOT_MIN_SIZE;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.HOT_SPOT_MIN_TARGET_COUNT;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.HOT_SPOT_Y_OFFSET;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.LIQUID_MAX_INSET;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.SURFACE_CONTACT_RIM_TEXTURE;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.SURFACE_HOT_SPOT_TEXTURE;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.SURFACE_PARTICLE_INTERVAL_TICKS;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.SURFACE_PARTICLE_TOP_LIMIT;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.SURFACE_SMOKE_HEADROOM_REQUIRED;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.SURFACE_SMOKE_PARTICLE_SPAWN_OFFSET;

/**
 * Controller-owned molten-surface effects.
 *
 * Hot spots are rendered per exposed liquid cell. Smoke uses a small
 * Foundry-wide budget and a per-cell probability that is normalized by the
 * horizontal liquid surface size, so connected Tanks do not become smoke
 * machines and the spawn position naturally moves around the whole surface.
 */
final class FoundryControllerSurfaceEffectsRenderer {

    private static final double MAX_EFFECT_DISTANCE_SQ = 24.0D * 24.0D;
    private static final double FULL_EFFECT_DISTANCE_SQ = 12.0D * 12.0D;

    /*
     * Keep roughly the same overall smoke frequency as the previous single
     * Controller-wide 45% attempt, but distribute that chance across every
     * exposed surface cell at the current liquid level.
     */
    private static final float SMOKE_BUDGET_PER_INTERVAL = 0.45f;

    /*
     * A large surface can occasionally emit a second small wisp, but never
     * more than two smoke particles from the entire Foundry during one
     * eligible particle tick.
     */
    private static final int MAX_SMOKE_PARTICLES_PER_INTERVAL = 2;
    private static final int SECOND_WISP_MIN_SURFACE_CELLS = 4;
    private static final float SECOND_WISP_CHANCE = 0.18f;

    private static final double SMOKE_HORIZONTAL_DRIFT = 0.010D;
    private static final double SMOKE_MIN_RISE_SPEED = 0.012D;
    private static final double SMOKE_EXTRA_RISE_SPEED = 0.010D;

    /*
     * At a completely full Tank the liquid surface sits exactly at
     * LIQUID_MAX_INSET. Rendering the hotspot on that same plane causes
     * z-fighting/jitter. Keep it a tiny fraction of a texture pixel above the
     * liquid, but still safely below the top glass face.
     */
    private static final float FULL_TANK_HOT_SPOT_SEPARATION =
            0.004f * (1.0f / 16.0f);

    private final Map<FoundryControllerBlockEntity, SmokeTickState> smokeTickStates =
            new WeakHashMap<>();

    void renderAndSpawn(
            FoundryControllerBlockEntity controller,
            BlockPos tankPos,
            Set<BlockPos> structure,
            MoltenMetalDefinition definition,
            FoundryTankLiquidGeometry geometry,
            float partialTick,
            PoseStack.Pose pose,
            MultiBufferSource bufferSource,
            int packedOverlay,
            float frameMinV,
            float frameMaxV
    ) {
        Level level = controller.getLevel();
        if (level == null) {
            return;
        }

        double distanceSq = distanceSquaredToCamera(tankPos);
        if (distanceSq > MAX_EFFECT_DISTANCE_SQ) {
            return;
        }

        renderContactRim(
                tankPos,
                structure,
                geometry,
                pose,
                bufferSource,
                packedOverlay
        );

        renderHotSpots(
                tankPos,
                structure,
                definition,
                geometry,
                partialTick,
                pose,
                bufferSource,
                packedOverlay,
                level.getGameTime(),
                distanceSq
        );

        maybeSpawnSmoke(
                controller,
                tankPos,
                structure,
                geometry,
                level
        );
    }

    /**
     * Renders a very soft bright band where molten liquid meets the Tank wall.
     *
     * This is intentionally subtle. The outer edge is only lightly tinted and
     * the strip fades fully to transparent toward the center of the liquid, so
     * it reads as heat at the contact boundary rather than a hard outline.
     */
    private static void renderContactRim(
            BlockPos tankPos,
            Set<BlockPos> structure,
            FoundryTankLiquidGeometry geometry,
            PoseStack.Pose pose,
            MultiBufferSource bufferSource,
            int packedOverlay
    ) {
        float glassHeadroom =
                Math.max(
                        0.0f,
                        LIQUID_MAX_INSET - geometry.surfaceY()
                );

        /*
         * Keep the contact glow visible under the top glass as well, but make
         * it slightly softer there.
         */
        float glassFade =
                Mth.lerp(
                        Mth.clamp(
                                glassHeadroom / CONTACT_RIM_GLASS_FADE_DISTANCE,
                                0.0f,
                                1.0f
                        ),
                        0.60f,
                        1.0f
                );

        int outerAlpha =
                Mth.clamp(
                        Math.round(
                                255.0f
                                        * CONTACT_RIM_MAX_ALPHA
                                        * glassFade
                        ),
                        0,
                        255
                );

        if (outerAlpha <= 1) {
            return;
        }

        int horizontalSurfaceCells =
                countHorizontalSurfaceCells(
                        structure,
                        tankPos.getY()
                );

        float rimWidth =
                getContactRimWidth(
                        horizontalSurfaceCells
                );

        float y =
                Math.min(
                        geometry.surfaceY() + CONTACT_RIM_Y_OFFSET,
                        LIQUID_MAX_INSET
                                + FULL_TANK_HOT_SPOT_SEPARATION * 0.5f
                );

        VertexConsumer consumer =
                bufferSource.getBuffer(
                        RenderType.entityTranslucent(
                                SURFACE_CONTACT_RIM_TEXTURE
                        )
                );

        boolean northBoundary =
                !structure.contains(tankPos.north());

        boolean southBoundary =
                !structure.contains(tankPos.south());

        boolean westBoundary =
                !structure.contains(tankPos.west());

        boolean eastBoundary =
                !structure.contains(tankPos.east());

        if (northBoundary) {
            renderNorthContactRim(
                    consumer,
                    pose,
                    geometry,
                    rimWidth,
                    y,
                    outerAlpha,
                    packedOverlay
            );
        }

        if (southBoundary) {
            renderSouthContactRim(
                    consumer,
                    pose,
                    geometry,
                    rimWidth,
                    y,
                    outerAlpha,
                    packedOverlay
            );
        }

        /*
         * North/south own the corner squares. Trim west/east away from those
         * same squares so two translucent coplanar rim quads never overlap.
         *
         * The previous version rendered both strips across each corner, which
         * is why the corner area shimmered / z-fought.
         */
        float verticalMinZ =
                geometry.minZ()
                        + (northBoundary ? rimWidth : 0.0f);

        float verticalMaxZ =
                geometry.maxZ()
                        - (southBoundary ? rimWidth : 0.0f);

        if (westBoundary && verticalMaxZ > verticalMinZ) {
            renderWestContactRim(
                    consumer,
                    pose,
                    geometry,
                    rimWidth,
                    verticalMinZ,
                    verticalMaxZ,
                    y,
                    outerAlpha,
                    packedOverlay
            );
        }

        if (eastBoundary && verticalMaxZ > verticalMinZ) {
            renderEastContactRim(
                    consumer,
                    pose,
                    geometry,
                    rimWidth,
                    verticalMinZ,
                    verticalMaxZ,
                    y,
                    outerAlpha,
                    packedOverlay
            );
        }
    }

    private static void renderNorthContactRim(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            FoundryTankLiquidGeometry geometry,
            float rimWidth,
            float y,
            int outerAlpha,
            int packedOverlay
    ) {
        float innerZ =
                Math.min(
                        geometry.maxZ(),
                        geometry.minZ() + rimWidth
                );

        renderContactRimQuad(
                consumer,
                pose,
                geometry.minX(),
                geometry.maxX(),
                y,
                geometry.minZ(),
                innerZ,
                true,
                outerAlpha,
                packedOverlay
        );
    }

    private static void renderSouthContactRim(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            FoundryTankLiquidGeometry geometry,
            float rimWidth,
            float y,
            int outerAlpha,
            int packedOverlay
    ) {
        float innerZ =
                Math.max(
                        geometry.minZ(),
                        geometry.maxZ() - rimWidth
                );

        renderContactRimQuad(
                consumer,
                pose,
                geometry.minX(),
                geometry.maxX(),
                y,
                innerZ,
                geometry.maxZ(),
                false,
                outerAlpha,
                packedOverlay
        );
    }

    private static void renderWestContactRim(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            FoundryTankLiquidGeometry geometry,
            float rimWidth,
            float minZ,
            float maxZ,
            float y,
            int outerAlpha,
            int packedOverlay
    ) {
        float innerX =
                Math.min(
                        geometry.maxX(),
                        geometry.minX() + rimWidth
                );

        renderContactRimQuadWestEast(
                consumer,
                pose,
                geometry.minX(),
                innerX,
                y,
                minZ,
                maxZ,
                true,
                outerAlpha,
                packedOverlay
        );
    }

    private static void renderEastContactRim(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            FoundryTankLiquidGeometry geometry,
            float rimWidth,
            float minZ,
            float maxZ,
            float y,
            int outerAlpha,
            int packedOverlay
    ) {
        float innerX =
                Math.max(
                        geometry.minX(),
                        geometry.maxX() - rimWidth
                );

        renderContactRimQuadWestEast(
                consumer,
                pose,
                innerX,
                geometry.maxX(),
                y,
                minZ,
                maxZ,
                false,
                outerAlpha,
                packedOverlay
        );
    }

    /*
     * North/south-oriented strip: alpha fades across Z.
     */
    private static void renderContactRimQuad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float minX,
            float maxX,
            float y,
            float minZ,
            float maxZ,
            boolean outerOnMinZ,
            int outerAlpha,
            int packedOverlay
    ) {
        int innerAlpha = 0;

        if (outerOnMinZ) {
            addContactRimVertex(
                    consumer,
                    pose,
                    minX,
                    y,
                    minZ,
                    0.0f,
                    0.0f,
                    outerAlpha,
                    packedOverlay
            );
            addContactRimVertex(
                    consumer,
                    pose,
                    minX,
                    y,
                    maxZ,
                    0.0f,
                    1.0f,
                    innerAlpha,
                    packedOverlay
            );
            addContactRimVertex(
                    consumer,
                    pose,
                    maxX,
                    y,
                    maxZ,
                    1.0f,
                    1.0f,
                    innerAlpha,
                    packedOverlay
            );
            addContactRimVertex(
                    consumer,
                    pose,
                    maxX,
                    y,
                    minZ,
                    1.0f,
                    0.0f,
                    outerAlpha,
                    packedOverlay
            );
        } else {
            addContactRimVertex(
                    consumer,
                    pose,
                    minX,
                    y,
                    minZ,
                    0.0f,
                    0.0f,
                    innerAlpha,
                    packedOverlay
            );
            addContactRimVertex(
                    consumer,
                    pose,
                    minX,
                    y,
                    maxZ,
                    0.0f,
                    1.0f,
                    outerAlpha,
                    packedOverlay
            );
            addContactRimVertex(
                    consumer,
                    pose,
                    maxX,
                    y,
                    maxZ,
                    1.0f,
                    1.0f,
                    outerAlpha,
                    packedOverlay
            );
            addContactRimVertex(
                    consumer,
                    pose,
                    maxX,
                    y,
                    minZ,
                    1.0f,
                    0.0f,
                    innerAlpha,
                    packedOverlay
            );
        }
    }

    /*
     * West/east-oriented strip: alpha fades across X.
     */
    private static void renderContactRimQuadWestEast(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float minX,
            float maxX,
            float y,
            float minZ,
            float maxZ,
            boolean outerOnMinX,
            int outerAlpha,
            int packedOverlay
    ) {
        int innerAlpha = 0;

        if (outerOnMinX) {
            addContactRimVertex(
                    consumer,
                    pose,
                    minX,
                    y,
                    minZ,
                    0.0f,
                    0.0f,
                    outerAlpha,
                    packedOverlay
            );
            addContactRimVertex(
                    consumer,
                    pose,
                    minX,
                    y,
                    maxZ,
                    0.0f,
                    1.0f,
                    outerAlpha,
                    packedOverlay
            );
            addContactRimVertex(
                    consumer,
                    pose,
                    maxX,
                    y,
                    maxZ,
                    1.0f,
                    1.0f,
                    innerAlpha,
                    packedOverlay
            );
            addContactRimVertex(
                    consumer,
                    pose,
                    maxX,
                    y,
                    minZ,
                    1.0f,
                    0.0f,
                    innerAlpha,
                    packedOverlay
            );
        } else {
            addContactRimVertex(
                    consumer,
                    pose,
                    minX,
                    y,
                    minZ,
                    0.0f,
                    0.0f,
                    innerAlpha,
                    packedOverlay
            );
            addContactRimVertex(
                    consumer,
                    pose,
                    minX,
                    y,
                    maxZ,
                    0.0f,
                    1.0f,
                    innerAlpha,
                    packedOverlay
            );
            addContactRimVertex(
                    consumer,
                    pose,
                    maxX,
                    y,
                    maxZ,
                    1.0f,
                    1.0f,
                    outerAlpha,
                    packedOverlay
            );
            addContactRimVertex(
                    consumer,
                    pose,
                    maxX,
                    y,
                    minZ,
                    1.0f,
                    0.0f,
                    outerAlpha,
                    packedOverlay
            );
        }
    }

    private static void addContactRimVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            int alpha,
            int packedOverlay
    ) {
        consumer.addVertex(
                        pose.pose(),
                        x,
                        y,
                        z
                )
                .setColor(
                        255,
                        214,
                        132,
                        alpha
                )
                .setUv(
                        u,
                        v
                )
                .setOverlay(packedOverlay)
                .setLight(
                        LightTexture.FULL_BRIGHT
                )
                .setNormal(
                        pose,
                        0.0f,
                        1.0f,
                        0.0f
                );
    }

    /**
     * Size-dependent contact-rim width.
     *
     * Requested targets:
     *   1x1 surface  -> 2.5 px
     *   2x2 surface  -> 3.5 px
     *   4x4 surface  -> 4.5 px
     *
     * For intermediate layouts, interpolate by horizontal surface cell count
     * so irregular and non-square shapes still scale smoothly.
     */
    private static float getContactRimWidth(
            int horizontalSurfaceCells
    ) {
        if (horizontalSurfaceCells <= CONTACT_RIM_MEDIUM_SURFACE_CELLS) {
            float t =
                    Mth.clamp(
                            (horizontalSurfaceCells - 1.0f)
                                    / (CONTACT_RIM_MEDIUM_SURFACE_CELLS - 1.0f),
                            0.0f,
                            1.0f
                    );

            return Mth.lerp(
                    t,
                    CONTACT_RIM_WIDTH_SMALL,
                    CONTACT_RIM_WIDTH_MEDIUM
            );
        }

        float t =
                Mth.clamp(
                        (horizontalSurfaceCells
                                - (float) CONTACT_RIM_MEDIUM_SURFACE_CELLS)
                                / (CONTACT_RIM_LARGE_SURFACE_CELLS
                                - (float) CONTACT_RIM_MEDIUM_SURFACE_CELLS),
                        0.0f,
                        1.0f
                );

        return Mth.lerp(
                t,
                CONTACT_RIM_WIDTH_MEDIUM,
                CONTACT_RIM_WIDTH_LARGE
        );
    }

    /**
     * Renders visible molten heat blooms instead of re-drawing tiny pieces of
     * the normal liquid texture.
     *
     * The old spots used the exact same full-bright molten texture as the
     * surface underneath them, were less than two pixels wide, and used fixed
     * positions derived only from Tank position. That made them almost
     * invisible until the liquid was near the top glass, and when visible they
     * always appeared in the same places.
     *
     * The new spots:
     *   - use a dedicated soft glow texture;
     *   - fade in and out over independently varied 1.5-3.0 second lifetimes;
     *   - vary brightness so broad blooms are softer and small blooms hotter;
     *   - pick a new deterministic random position after each fade-out;
     *   - scale their density to the whole connected horizontal surface rather
     *     than multiplying blindly with Tank count.
     */
    private static void renderHotSpots(
            BlockPos tankPos,
            Set<BlockPos> structure,
            MoltenMetalDefinition definition,
            FoundryTankLiquidGeometry geometry,
            float partialTick,
            PoseStack.Pose pose,
            MultiBufferSource bufferSource,
            int packedOverlay,
            long gameTime,
            double distanceSq
    ) {
        float usableMinX =
                geometry.minX() + HOT_SPOT_MARGIN;

        float usableMaxX =
                geometry.maxX() - HOT_SPOT_MARGIN;

        float usableMinZ =
                geometry.minZ() + HOT_SPOT_MARGIN;

        float usableMaxZ =
                geometry.maxZ() - HOT_SPOT_MARGIN;

        if (
                usableMaxX <= usableMinX
                        || usableMaxZ <= usableMinZ
        ) {
            return;
        }

        /*
         * The top glass begins at the same inner height as LIQUID_MAX_INSET.
         * The old hotspot Y offset could therefore push a full-Tank hotspot
         * through the glass plane, changing its apparent color/order.
         *
         * Fade the heat blooms away during the final ~1.25 texture pixels of
         * headroom and never let their quad cross the inner glass plane.
         */
        float glassHeadroom =
                Math.max(
                        0.0f,
                        LIQUID_MAX_INSET - geometry.surfaceY()
                );

        /*
         * Keep the bloom visible even when the Tank is completely full.
         *
         * Near the top glass we reduce its strength, but never to zero. The
         * quad is also clamped to the liquid surface / inner-glass plane
         * instead of being pushed through the tinted glass.
         */
        float glassFade =
                Mth.lerp(
                        Mth.clamp(
                                glassHeadroom / HOT_SPOT_GLASS_FADE_DISTANCE,
                                0.0f,
                                1.0f
                        ),
                        0.58f,
                        1.0f
                );

        /*
         * Never collapse the hotspot back onto the liquid's exact plane.
         *
         * At full height LIQUID_MAX_INSET is also the liquid top. The previous
         * clamp therefore made both quads coplanar, which is what caused the
         * visible jitter/z-fighting. The tiny extra ceiling here leaves a
         * stable gap above the liquid while remaining below the glass.
         */
        float hotSpotY =
                Math.min(
                        geometry.surfaceY() + HOT_SPOT_Y_OFFSET,
                        LIQUID_MAX_INSET + FULL_TANK_HOT_SPOT_SEPARATION
                );

        int horizontalSurfaceCells =
                countHorizontalSurfaceCells(
                        structure,
                        tankPos.getY()
                );

        /*
         * A single Tank gets one large active bloom. As the connected surface
         * grows, aim for a few blooms across the whole surface rather than a
         * fixed number per Tank cell.
         */
        int targetCount =
                horizontalSurfaceCells == 1
                        ? 1
                        : Mth.clamp(
                        2 + horizontalSurfaceCells / 3,
                        HOT_SPOT_MIN_TARGET_COUNT,
                        HOT_SPOT_MAX_TARGET_COUNT
                );

        /*
         * At longer distance keep the effect visible but lighter.
         */
        if (distanceSq > FULL_EFFECT_DISTANCE_SQ) {
            targetCount =
                    Math.min(
                            targetCount,
                            3
                    );
        }

        /*
         * Use one bloom candidate per exposed Tank cell.
         *
         * A 1x1 Tank used to create two candidates inside the same 16x16
         * surface. Once the blooms became large, the second candidate could
         * repeatedly pass/fail the overlap test as both spots pulsed in size,
         * which looked like a fast blink. One larger bloom is cleaner for a
         * single Tank and still relocates smoothly after each fade-out cycle.
         */
        int attemptsPerCell = 1;

        float activeChance =
                Math.min(
                        1.0f,
                        targetCount
                                / (float) Math.max(
                                1,
                                horizontalSurfaceCells * attemptsPerCell
                        )
                );

        VertexConsumer consumer =
                bufferSource.getBuffer(
                        RenderType.entityTranslucent(
                                SURFACE_HOT_SPOT_TEXTURE
                        )
                );

        long metalSalt =
                definition.id().hashCode();

        float previousCenterX = Float.NaN;
        float previousCenterZ = Float.NaN;
        float previousSize = 0.0f;

        for (int slot = 0; slot < attemptsPerCell; slot++) {
            /*
             * Give every potential bloom a different lifetime offset so the
             * entire surface does not pulse in sync.
             */
            long phaseSeed =
                    mix64(
                            tankPos.asLong()
                                    ^ (long) slot * 0x9E3779B97F4A7C15L
                                    ^ metalSalt * 0xC2B2AE3D27D4EB4FL
                    );

            /*
             * Give every surface cell its own stable lifetime. That means one
             * bloom may take ~1.5 seconds to complete a cycle while another
             * takes close to ~3 seconds. Their fade timing therefore never
             * collapses into one obvious synchronized pulse.
             */
            long hotSpotLifetimeTicks =
                    Math.max(
                            1L,
                            Math.round(
                                    Mth.lerp(
                                            unit(phaseSeed ^ 0x7F4A7C15L),
                                            (float) HOT_SPOT_MIN_LIFETIME_TICKS,
                                            (float) HOT_SPOT_MAX_LIFETIME_TICKS
                                    )
                            )
                    );

            double phaseOffset =
                    unit(phaseSeed ^ 0x13579BDFL)
                            * hotSpotLifetimeTicks;

            double animatedTime =
                    gameTime
                            + partialTick
                            + phaseOffset;

            long cycle =
                    (long) Math.floor(
                            animatedTime
                                    / hotSpotLifetimeTicks
                    );

            double cycleStart =
                    cycle
                            * (double) hotSpotLifetimeTicks;

            float phase =
                    (float) (
                            (animatedTime - cycleStart)
                                    / hotSpotLifetimeTicks
                    );

            /*
             * Zero at both ends means the position can change between cycles
             * while the bloom is fully invisible, so there is no teleport pop.
             */
            float envelope =
                    Mth.sin(
                            Mth.clamp(
                                    phase,
                                    0.0f,
                                    1.0f
                            )
                                    * (float) Math.PI
                    );

            if (envelope <= 0.04f) {
                continue;
            }

            long seed =
                    mix64(
                            tankPos.asLong()
                                    ^ (long) slot * 0xD6E8FEB86659FD93L
                                    ^ cycle * 0x9E3779B97F4A7C15L
                                    ^ metalSalt * 0x94D049BB133111EBL
                    );

            if (unit(seed ^ 0x2468ACE0L) > activeChance) {
                continue;
            }

            /*
             * Each bloom now chooses its own base diameter. The lifetime pulse
             * changes that diameter only slightly, so different spots remain
             * obviously different sizes instead of all converging on one size.
             */
            float baseSize =
                    Mth.lerp(
                            unit(seed ^ 0x5555L),
                            HOT_SPOT_MIN_SIZE,
                            HOT_SPOT_MAX_SIZE
                    );

            /*
             * Size and intensity are intentionally related:
             *
             *   smaller bloom -> hotter / brighter
             *   broader bloom -> softer / fainter
             *
             * A small independent per-cycle multiplier prevents equal-sized
             * blooms from still looking mechanically identical.
             */
            float sizeRange =
                    Math.max(
                            0.0001f,
                            HOT_SPOT_MAX_SIZE - HOT_SPOT_MIN_SIZE
                    );

            float normalizedSize =
                    Mth.clamp(
                            (baseSize - HOT_SPOT_MIN_SIZE) / sizeRange,
                            0.0f,
                            1.0f
                    );

            float sizeBasedIntensity =
                    Mth.lerp(
                            normalizedSize,
                            HOT_SPOT_MAX_INTENSITY,
                            HOT_SPOT_MIN_INTENSITY
                    );

            float randomIntensity =
                    Mth.lerp(
                            unit(seed ^ 0xABCD1234L),
                            0.88f,
                            1.10f
                    );

            float hotSpotIntensity =
                    Mth.clamp(
                            sizeBasedIntensity * randomIntensity,
                            HOT_SPOT_MIN_INTENSITY,
                            HOT_SPOT_MAX_INTENSITY
                    );

            float size =
                    baseSize
                            * Mth.lerp(
                            envelope,
                            0.84f,
                            1.06f
                    );

            float centerX = 0.0f;
            float centerZ = 0.0f;
            boolean foundNonOverlappingPosition = false;

            /*
             * Multi-Tank surfaces use only one candidate per cell, and
             * HOT_SPOT_MARGIN is larger than the maximum radius, so spots in
             * neighboring cells cannot overlap. A single Tank uses two
             * candidates, so explicitly keep those two apart.
             */
            for (int placementAttempt = 0; placementAttempt < 10; placementAttempt++) {
                long placementSeed =
                        mix64(
                                seed
                                        ^ (long) placementAttempt
                                        * 0xA24BAED4963EE407L
                        );

                centerX =
                        Mth.lerp(
                                unit(placementSeed ^ 0x1111L),
                                usableMinX,
                                usableMaxX
                        );

                centerZ =
                        Mth.lerp(
                                unit(placementSeed ^ 0x2222L),
                                usableMinZ,
                                usableMaxZ
                        );

                if (Float.isNaN(previousCenterX)) {
                    foundNonOverlappingPosition = true;
                    break;
                }

                float dx =
                        centerX - previousCenterX;

                float dz =
                        centerZ - previousCenterZ;

                float requiredDistance =
                        (size + previousSize) * 0.5f
                                + 0.50f * (1.0f / 16.0f);

                if (
                        dx * dx + dz * dz
                                >= requiredDistance * requiredDistance
                ) {
                    foundNonOverlappingPosition = true;
                    break;
                }
            }

            if (!foundNonOverlappingPosition) {
                continue;
            }

            /*
             * Very slight motion makes a bloom feel alive without sliding
             * obviously across the liquid.
             */
            float driftEnvelope =
                    Mth.sin(
                            phase
                                    * (float) Math.PI
                                    * 2.0f
                    );

            centerX +=
                    (unit(seed ^ 0x3333L) - 0.5f)
                            * 0.030f
                            * driftEnvelope;

            centerZ +=
                    (unit(seed ^ 0x4444L) - 0.5f)
                            * 0.030f
                            * driftEnvelope;

            centerX =
                    Mth.clamp(
                            centerX,
                            usableMinX,
                            usableMaxX
                    );

            centerZ =
                    Mth.clamp(
                            centerZ,
                            usableMinZ,
                            usableMaxZ
                    );

            float halfSize =
                    size * 0.5f;

            float minX =
                    Math.max(
                            geometry.minX(),
                            centerX - halfSize
                    );

            float maxX =
                    Math.min(
                            geometry.maxX(),
                            centerX + halfSize
                    );

            float minZ =
                    Math.max(
                            geometry.minZ(),
                            centerZ - halfSize
                    );

            float maxZ =
                    Math.min(
                            geometry.maxZ(),
                            centerZ + halfSize
                    );

            int alpha =
                    Mth.clamp(
                            Math.round(
                                    255.0f
                                            * HOT_SPOT_MAX_ALPHA
                                            * hotSpotIntensity
                                            * envelope
                                            * glassFade
                            ),
                            0,
                            255
                    );

            if (alpha <= 2) {
                continue;
            }

            renderHotSpotQuad(
                    consumer,
                    pose,
                    minX,
                    maxX,
                    hotSpotY,
                    minZ,
                    maxZ,
                    alpha,
                    packedOverlay
            );

            previousCenterX = centerX;
            previousCenterZ = centerZ;
            previousSize = size;
        }
    }

    private static void renderHotSpotQuad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float minX,
            float maxX,
            float y,
            float minZ,
            float maxZ,
            int alpha,
            int packedOverlay
    ) {
        int color =
                (alpha << 24)
                        | 0x00FFFFFF;

        addHotSpotVertex(
                consumer,
                pose,
                minX,
                y,
                minZ,
                0.0f,
                0.0f,
                color,
                packedOverlay
        );

        addHotSpotVertex(
                consumer,
                pose,
                minX,
                y,
                maxZ,
                0.0f,
                1.0f,
                color,
                packedOverlay
        );

        addHotSpotVertex(
                consumer,
                pose,
                maxX,
                y,
                maxZ,
                1.0f,
                1.0f,
                color,
                packedOverlay
        );

        addHotSpotVertex(
                consumer,
                pose,
                maxX,
                y,
                minZ,
                1.0f,
                0.0f,
                color,
                packedOverlay
        );
    }

    private static void addHotSpotVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            int color,
            int packedOverlay
    ) {
        consumer.addVertex(
                        pose.pose(),
                        x,
                        y,
                        z
                )
                .setColor(color)
                .setUv(
                        u,
                        v
                )
                .setOverlay(packedOverlay)
                .setLight(
                        LightTexture.FULL_BRIGHT
                )
                .setNormal(
                        pose,
                        0.0f,
                        1.0f,
                        0.0f
                );
    }

    private void maybeSpawnSmoke(
            FoundryControllerBlockEntity controller,
            BlockPos tankPos,
            Set<BlockPos> structure,
            FoundryTankLiquidGeometry geometry,
            Level level
    ) {
        long gameTime = level.getGameTime();

        if (gameTime % SURFACE_PARTICLE_INTERVAL_TICKS != 0L) {
            return;
        }

        SmokeTickState tickState =
                smokeTickStates.get(controller);

        if (tickState == null || tickState.gameTime != gameTime) {
            tickState = new SmokeTickState(gameTime);
            smokeTickStates.put(controller, tickState);
        }

        /*
         * A BER can render several times during one game tick. Each exposed
         * surface cell is allowed to make its random smoke decision only once.
         */
        BlockPos immutableTankPos = tankPos.immutable();

        if (!tickState.attemptedSurfaceCells.add(immutableTankPos)) {
            return;
        }

        if (tickState.spawnedParticles >= MAX_SMOKE_PARTICLES_PER_INTERVAL) {
            return;
        }

        if (!hasSmokeHeadroom(tankPos, structure, geometry)) {
            return;
        }

        int horizontalSurfaceCells =
                countHorizontalSurfaceCells(
                        structure,
                        tankPos.getY()
                );

        /*
         * Divide the old 45% Controller-wide chance over the horizontal
         * surface. A 1x1 Tank therefore keeps the old frequency, while a 4x4
         * surface does not emit sixteen times as much smoke.
         */
        float spawnChance =
                SMOKE_BUDGET_PER_INTERVAL
                        / Math.max(1, horizontalSurfaceCells);

        long seed = mix64(
                tankPos.asLong()
                        ^ controller.getBlockPos().asLong()
                        ^ gameTime * 0x9E3779B97F4A7C15L
        );

        if (unit(seed ^ 0x51A7L) > spawnChance) {
            return;
        }

        spawnSmokeParticle(
                tankPos,
                geometry,
                level,
                seed
        );

        tickState.spawnedParticles++;

        /*
         * Large connected surfaces occasionally get a second nearby wisp.
         * It uses an independent seed and a different position/velocity, so it
         * does not look like two particles stacked on exactly the same point.
         */
        if (
                horizontalSurfaceCells >= SECOND_WISP_MIN_SURFACE_CELLS
                        && tickState.spawnedParticles < MAX_SMOKE_PARTICLES_PER_INTERVAL
                        && unit(seed ^ 0x6A09E667F3BCC909L) < SECOND_WISP_CHANCE
        ) {
            spawnSmokeParticle(
                    tankPos,
                    geometry,
                    level,
                    mix64(seed ^ 0xBB67AE8584CAA73BL)
            );

            tickState.spawnedParticles++;
        }
    }

    private static void spawnSmokeParticle(
            BlockPos tankPos,
            FoundryTankLiquidGeometry geometry,
            Level level,
            long seed
    ) {
        float minX =
                Math.min(
                        geometry.maxX() - 0.02f,
                        geometry.minX() + 0.08f
                );

        float maxX =
                Math.max(
                        geometry.minX() + 0.02f,
                        geometry.maxX() - 0.08f
                );

        float minZ =
                Math.min(
                        geometry.maxZ() - 0.02f,
                        geometry.minZ() + 0.08f
                );

        float maxZ =
                Math.max(
                        geometry.minZ() + 0.02f,
                        geometry.maxZ() - 0.08f
                );

        double x =
                tankPos.getX()
                        + Mth.lerp(
                                unit(seed ^ 0x1111L),
                                minX,
                                maxX
                        );

        double y =
                tankPos.getY()
                        + geometry.surfaceY()
                        + SURFACE_SMOKE_PARTICLE_SPAWN_OFFSET;

        double z =
                tankPos.getZ()
                        + Mth.lerp(
                                unit(seed ^ 0x2222L),
                                minZ,
                                maxZ
                        );

        double velocityX =
                (unit(seed ^ 0x3333L) - 0.5D)
                        * SMOKE_HORIZONTAL_DRIFT;

        double velocityY =
                SMOKE_MIN_RISE_SPEED
                        + unit(seed ^ 0x4444L)
                        * SMOKE_EXTRA_RISE_SPEED;

        double velocityZ =
                (unit(seed ^ 0x5555L) - 0.5D)
                        * SMOKE_HORIZONTAL_DRIFT;

        level.addParticle(
                ParticleTypes.SMOKE,
                x,
                y,
                z,
                velocityX,
                velocityY,
                velocityZ
        );
    }

    private static boolean hasSmokeHeadroom(
            BlockPos tankPos,
            Set<BlockPos> structure,
            FoundryTankLiquidGeometry geometry
    ) {
        /*
         * If another Tank exists above this cell, the smoke can rise into that
         * empty Tank volume. Otherwise keep smoke away from liquid that is
         * almost touching the top glass, where the particle would immediately
         * clip through the casing.
         */
        if (structure.contains(tankPos.above())) {
            return true;
        }

        float headroom =
                1.0f - geometry.surfaceY();

        return geometry.surfaceY() <= SURFACE_PARTICLE_TOP_LIMIT
                && headroom >= SURFACE_SMOKE_HEADROOM_REQUIRED;
    }

    private static int countHorizontalSurfaceCells(
            Set<BlockPos> structure,
            int y
    ) {
        int count = 0;

        for (BlockPos pos : structure) {
            if (pos.getY() == y) {
                count++;
            }
        }

        return Math.max(1, count);
    }

    private static double distanceSquaredToCamera(
            BlockPos pos
    ) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera == null) {
            return Double.POSITIVE_INFINITY;
        }

        double dx = camera.getX() - (pos.getX() + 0.5D);
        double dy = camera.getY() - (pos.getY() + 0.5D);
        double dz = camera.getZ() - (pos.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }

    private static float stableUnit(
            BlockPos pos,
            int index,
            long salt
    ) {
        return unit(
                mix64(
                        pos.asLong()
                                ^ (long) index * 0x9E3779B97F4A7C15L
                                ^ salt
                )
        );
    }

    private static float unit(
            long value
    ) {
        return (float) ((value >>> 40) & 0xFFFFFFL) / (float) 0x1000000;
    }

    private static long mix64(
            long value
    ) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    private static final class SmokeTickState {

        private final long gameTime;
        private final Set<BlockPos> attemptedSurfaceCells =
                new HashSet<>();

        private int spawnedParticles;

        private SmokeTickState(
                long gameTime
        ) {
            this.gameTime = gameTime;
        }
    }
}
