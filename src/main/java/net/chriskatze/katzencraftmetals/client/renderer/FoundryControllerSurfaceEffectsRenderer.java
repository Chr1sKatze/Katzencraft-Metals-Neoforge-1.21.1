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

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.HOT_SPOT_COUNT;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.HOT_SPOT_MARGIN;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.HOT_SPOT_MAX_SIZE;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.HOT_SPOT_MIN_SIZE;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.HOT_SPOT_Y_OFFSET;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.SURFACE_PARTICLE_INTERVAL_TICKS;

/** Lightweight controller-owned version of the old Tank surface effects. */
final class FoundryControllerSurfaceEffectsRenderer {

    private static final double MAX_EFFECT_DISTANCE_SQ = 24.0D * 24.0D;
    private static final double FULL_EFFECT_DISTANCE_SQ = 12.0D * 12.0D;

    private final Map<FoundryControllerBlockEntity, Long> lastSmokeAttemptTick =
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
            FoundryTankLiquidGeometry geometry,
            Level level
    ) {
        long gameTime = level.getGameTime();

        if (gameTime % SURFACE_PARTICLE_INTERVAL_TICKS != 0L) {
            return;
        }

        Long last = lastSmokeAttemptTick.get(controller);
        if (last != null && last == gameTime) {
            return;
        }
        lastSmokeAttemptTick.put(controller, gameTime);

        long seed = mix64(
                tankPos.asLong()
                        ^ gameTime * 0x9E3779B97F4A7C15L
        );

        if (unit(seed ^ 0x51A7L) > 0.45f) {
            return;
        }

        double x = tankPos.getX()
                + Mth.lerp(unit(seed ^ 0x1111L), geometry.minX() + 0.08f, geometry.maxX() - 0.08f);
        double y = tankPos.getY() + geometry.surfaceY() + 0.02D;
        double z = tankPos.getZ()
                + Mth.lerp(unit(seed ^ 0x2222L), geometry.minZ() + 0.08f, geometry.maxZ() - 0.08f);

        level.addParticle(
                ParticleTypes.SMOKE,
                x,
                y,
                z,
                (unit(seed ^ 0x3333L) - 0.5D) * 0.006D,
                0.008D + unit(seed ^ 0x4444L) * 0.004D,
                (unit(seed ^ 0x5555L) - 0.5D) * 0.006D
        );
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
}
