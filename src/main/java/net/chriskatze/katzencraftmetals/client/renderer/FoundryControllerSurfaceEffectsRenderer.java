package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.block.entity.FoundryControllerBlockEntity;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.client.Minecraft;
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

import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.HOT_SPOT_COUNT;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.HOT_SPOT_MARGIN;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.HOT_SPOT_MAX_SIZE;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.HOT_SPOT_MIN_SIZE;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.HOT_SPOT_Y_OFFSET;
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

        int count = distanceSq <= FULL_EFFECT_DISTANCE_SQ
                ? HOT_SPOT_COUNT
                : 1;

        if (structure.size() >= 48 && distanceSq > FULL_EFFECT_DISTANCE_SQ) {
            long selection = mix64(tankPos.asLong());
            if (Math.floorMod(selection, 3L) != 0L) {
                count = 0;
            }
        }

        if (count > 0) {
            renderHotSpots(
                    tankPos,
                    definition,
                    geometry,
                    partialTick,
                    pose,
                    bufferSource,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    count,
                    level.getGameTime()
            );
        }

        maybeSpawnSmoke(
                controller,
                tankPos,
                structure,
                geometry,
                level
        );
    }

    private static void renderHotSpots(
            BlockPos tankPos,
            MoltenMetalDefinition definition,
            FoundryTankLiquidGeometry geometry,
            float partialTick,
            PoseStack.Pose pose,
            MultiBufferSource bufferSource,
            int packedOverlay,
            float frameMinV,
            float frameMaxV,
            int count,
            long gameTime
    ) {
        float usableMinX = geometry.minX() + HOT_SPOT_MARGIN;
        float usableMaxX = geometry.maxX() - HOT_SPOT_MARGIN;
        float usableMinZ = geometry.minZ() + HOT_SPOT_MARGIN;
        float usableMaxZ = geometry.maxZ() - HOT_SPOT_MARGIN;

        if (usableMaxX <= usableMinX || usableMaxZ <= usableMinZ) {
            return;
        }

        VertexConsumer consumer = bufferSource.getBuffer(
                RenderType.entityTranslucent(definition.animatedTexture())
        );

        for (int index = 0; index < count; index++) {
            float pulse = 0.5f + 0.5f * Mth.sin(
                    (gameTime + partialTick) * 0.23f
                            + stableUnit(tankPos, index, 11L) * 6.2831855f
            );

            if (pulse < 0.30f) {
                continue;
            }

            float centerX = Mth.lerp(
                    stableUnit(tankPos, index, 17L),
                    usableMinX,
                    usableMaxX
            );
            float centerZ = Mth.lerp(
                    stableUnit(tankPos, index, 41L),
                    usableMinZ,
                    usableMaxZ
            );
            float size = Mth.lerp(
                    pulse,
                    HOT_SPOT_MIN_SIZE,
                    HOT_SPOT_MAX_SIZE
            );

            float minX = Math.max(geometry.minX(), centerX - size * 0.5f);
            float maxX = Math.min(geometry.maxX(), centerX + size * 0.5f);
            float minZ = Math.max(geometry.minZ(), centerZ - size * 0.5f);
            float maxZ = Math.min(geometry.maxZ(), centerZ + size * 0.5f);

            FoundryTankLiquidQuads.renderLiquidHorizontalFace(
                    net.minecraft.core.Direction.UP,
                    consumer,
                    pose,
                    minX,
                    maxX,
                    Math.min(1.0f, geometry.surfaceY() + HOT_SPOT_Y_OFFSET),
                    minZ,
                    maxZ,
                    packedOverlay,
                    frameMinV,
                    frameMaxV
            );
        }
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
