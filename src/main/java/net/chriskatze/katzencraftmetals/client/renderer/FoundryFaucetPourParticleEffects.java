package net.chriskatze.katzencraftmetals.client.renderer;

import net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock;
import net.chriskatze.katzencraftmetals.block.entity.FoundryFaucetBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Small client-side particle accents for active pouring.
 *
 * Tanks stay enclosed and do not spawn lava particles anymore. The open pour
 * and target basin are where real vanilla lava particles make visual sense.
 */
final class FoundryFaucetPourParticleEffects {

    private static final long OUTLET_INTERVAL_TICKS =
            5L;

    private static final long BASIN_INTERVAL_TICKS =
            6L;

    private static final long LONG_DROP_BASIN_INTERVAL_TICKS =
            4L;

    private static final float OUTLET_START_PROGRESS =
            0.18f;

    private static final float BASIN_START_PROGRESS =
            0.84f;

    private static final float LONG_DROP_BASIN_START_PROGRESS =
            0.78f;

    /*
     * In faucet-local coordinates, a 3-block target basin surface lives roughly
     * between -2.75 and -2.11. A 4-block target basin surface lives roughly
     * between -3.75 and -3.11, so -3.0 cleanly identifies the new long-drop case.
     */
    private static final float LONG_DROP_SURFACE_Y_THRESHOLD =
            -3.0f;

    private static final float BASIN_LAVA_CHANCE =
            0.52f;

    private static final float BASIN_SMOKE_CHANCE =
            0.34f;

    private static final float LONG_DROP_BASIN_LAVA_CHANCE =
            0.72f;

    private static final float LONG_DROP_BASIN_EXTRA_LAVA_CHANCE =
            0.55f;

    private static final float LONG_DROP_BASIN_SMOKE_CHANCE =
            0.42f;

    private static final float STREAM_CENTER_X =
            (
                    FoundryFaucetRenderConstants.STREAM_MIN_X
                            + FoundryFaucetRenderConstants.STREAM_MAX_X
            ) * 0.5f;

    private static final float STREAM_CENTER_Z =
            (
                    FoundryFaucetRenderConstants.STREAM_MIN_Z
                            + FoundryFaucetRenderConstants.STREAM_MAX_Z
            ) * 0.5f;

    private static final float OUTLET_Y =
            FoundryFaucetRenderConstants.STREAM_OUTER_TOP_Y;

    private final Map<FoundryFaucetBlockEntity, Long> lastOutletTick =
            new WeakHashMap<>();

    private final Map<FoundryFaucetBlockEntity, Long> lastBasinTick =
            new WeakHashMap<>();

    void spawnWhilePouring(
            FoundryFaucetBlockEntity faucet,
            FoundryFaucetRenderContext context,
            float animationProgress
    ) {
        Level level =
                faucet.getLevel();

        if (
                level == null
                        || !level.isClientSide()
                        || !faucet.isPouring()
        ) {
            return;
        }

        if (!faucet.getBlockState().hasProperty(FoundryFaucetBlock.FACING)) {
            return;
        }

        Direction facing =
                faucet.getBlockState()
                        .getValue(
                                FoundryFaucetBlock.FACING
                        );

        long gameTime =
                level.getGameTime();

        if (animationProgress >= OUTLET_START_PROGRESS) {
            maybeSpawnOutletParticles(
                    faucet,
                    facing,
                    gameTime
            );
        }

        boolean longDrop =
                isLongBasinDrop(
                        context
                );

        float basinStartProgress =
                longDrop
                        ? LONG_DROP_BASIN_START_PROGRESS
                        : BASIN_START_PROGRESS;

        if (animationProgress >= basinStartProgress) {
            maybeSpawnBasinParticles(
                    faucet,
                    facing,
                    context,
                    gameTime,
                    longDrop
            );
        }
    }

    private void maybeSpawnOutletParticles(
            FoundryFaucetBlockEntity faucet,
            Direction facing,
            long gameTime
    ) {
        Long lastTick =
                lastOutletTick.get(
                        faucet
                );

        if (
                lastTick != null
                        && lastTick == gameTime
        ) {
            return;
        }

        lastOutletTick.put(
                faucet,
                gameTime
        );

        if (gameTime % OUTLET_INTERVAL_TICKS != 0L) {
            return;
        }

        long seed =
                mix64(
                        faucet.getBlockPos()
                                .asLong()
                                ^ gameTime
                                ^ 0xFA0C37L
                );

        if (unitFloat(seed ^ 0x1001L) < 0.36f) {
            spawnOutletLava(
                    faucet,
                    facing,
                    seed
            );
        }

        if (unitFloat(seed ^ 0x1002L) < 0.18f) {
            spawnOutletSmoke(
                    faucet,
                    facing,
                    seed
            );
        }
    }

    private void maybeSpawnBasinParticles(
            FoundryFaucetBlockEntity faucet,
            Direction facing,
            FoundryFaucetRenderContext context,
            long gameTime,
            boolean longDrop
    ) {
        Long lastTick =
                lastBasinTick.get(
                        faucet
                );

        if (
                lastTick != null
                        && lastTick == gameTime
        ) {
            return;
        }

        lastBasinTick.put(
                faucet,
                gameTime
        );

        long interval =
                longDrop
                        ? LONG_DROP_BASIN_INTERVAL_TICKS
                        : BASIN_INTERVAL_TICKS;

        if (gameTime % interval != 0L) {
            return;
        }

        long seed =
                mix64(
                        faucet.getBlockPos()
                                .asLong()
                                ^ gameTime
                                ^ 0xCA1170L
                );

        float lavaChance =
                longDrop
                        ? LONG_DROP_BASIN_LAVA_CHANCE
                        : BASIN_LAVA_CHANCE;

        float smokeChance =
                longDrop
                        ? LONG_DROP_BASIN_SMOKE_CHANCE
                        : BASIN_SMOKE_CHANCE;

        if (unitFloat(seed ^ 0x2001L) < lavaChance) {
            spawnBasinLava(
                    faucet,
                    facing,
                    context,
                    seed,
                    longDrop
            );
        }

        /*
         * 4-block drops are visually farther away, so give them a restrained
         * second possible lava pop. Short pours stay only slightly boosted.
         */
        if (
                longDrop
                        && unitFloat(seed ^ 0x2003L)
                        < LONG_DROP_BASIN_EXTRA_LAVA_CHANCE
        ) {
            spawnBasinLava(
                    faucet,
                    facing,
                    context,
                    seed ^ 0x51A7BEEFL,
                    true
            );
        }

        if (unitFloat(seed ^ 0x2002L) < smokeChance) {
            spawnBasinSmoke(
                    faucet,
                    facing,
                    context,
                    seed,
                    longDrop
            );
        }
    }

    private static void spawnOutletLava(
            FoundryFaucetBlockEntity faucet,
            Direction facing,
            long seed
    ) {
        ParticlePoint point =
                localToWorld(
                        faucet.getBlockPos(),
                        facing,
                        STREAM_CENTER_X
                                + centeredNoise(seed ^ 0x3001L) * 0.05f,
                        OUTLET_Y
                                + centeredNoise(seed ^ 0x3002L) * 0.025f,
                        STREAM_CENTER_Z
                                + centeredNoise(seed ^ 0x3003L) * 0.04f
                );

        faucet.getLevel().addParticle(
                ParticleTypes.LAVA,
                point.x(),
                point.y(),
                point.z(),
                facing.getStepX() * 0.012d,
                -0.006d,
                facing.getStepZ() * 0.012d
        );
    }

    private static void spawnOutletSmoke(
            FoundryFaucetBlockEntity faucet,
            Direction facing,
            long seed
    ) {
        ParticlePoint point =
                localToWorld(
                        faucet.getBlockPos(),
                        facing,
                        STREAM_CENTER_X
                                + centeredNoise(seed ^ 0x4001L) * 0.06f,
                        OUTLET_Y
                                + 0.015f,
                        STREAM_CENTER_Z
                                + centeredNoise(seed ^ 0x4002L) * 0.05f
                );

        faucet.getLevel().addParticle(
                ParticleTypes.SMOKE,
                point.x(),
                point.y(),
                point.z(),
                facing.getStepX() * 0.004d,
                0.010d,
                facing.getStepZ() * 0.004d
        );
    }

    private static void spawnBasinLava(
            FoundryFaucetBlockEntity faucet,
            Direction facing,
            FoundryFaucetRenderContext context,
            long seed,
            boolean longDrop
    ) {
        float spread =
                longDrop
                        ? 0.16f
                        : 0.13f;

        double speed =
                longDrop
                        ? 0.014d
                        : 0.011d;

        ParticlePoint point =
                localToWorld(
                        faucet.getBlockPos(),
                        facing,
                        STREAM_CENTER_X
                                + centeredNoise(seed ^ 0x5001L) * spread,
                        context.cauldronSurfaceY()
                                + 0.035f,
                        STREAM_CENTER_Z
                                + centeredNoise(seed ^ 0x5002L) * spread
                );

        faucet.getLevel().addParticle(
                ParticleTypes.LAVA,
                point.x(),
                point.y(),
                point.z(),
                centeredNoise(seed ^ 0x5003L) * speed,
                0.010d,
                centeredNoise(seed ^ 0x5004L) * speed
        );
    }

    private static void spawnBasinSmoke(
            FoundryFaucetBlockEntity faucet,
            Direction facing,
            FoundryFaucetRenderContext context,
            long seed,
            boolean longDrop
    ) {
        float spread =
                longDrop
                        ? 0.18f
                        : 0.15f;

        double speed =
                longDrop
                        ? 0.010d
                        : 0.009d;

        ParticlePoint point =
                localToWorld(
                        faucet.getBlockPos(),
                        facing,
                        STREAM_CENTER_X
                                + centeredNoise(seed ^ 0x6001L) * spread,
                        context.cauldronSurfaceY()
                                + 0.055f,
                        STREAM_CENTER_Z
                                + centeredNoise(seed ^ 0x6002L) * spread
                );

        faucet.getLevel().addParticle(
                ParticleTypes.SMOKE,
                point.x(),
                point.y(),
                point.z(),
                centeredNoise(seed ^ 0x6003L) * speed,
                0.014d,
                centeredNoise(seed ^ 0x6004L) * speed
        );
    }

    private static boolean isLongBasinDrop(
            FoundryFaucetRenderContext context
    ) {
        return context.cauldronSurfaceY()
                <= LONG_DROP_SURFACE_Y_THRESHOLD;
    }

    private static ParticlePoint localToWorld(
            BlockPos blockPos,
            Direction facing,
            float localX,
            float localY,
            float localZ
    ) {
        float rotatedX;
        float rotatedZ;

        /*
         * Mirrors FoundryFaucetRenderTransform#rotateForFacing, but for raw
         * world particle coordinates instead of PoseStack geometry.
         */
        switch (facing) {
            case EAST -> {
                rotatedX = 1.0f - localZ;
                rotatedZ = localX;
            }
            case SOUTH -> {
                rotatedX = 1.0f - localX;
                rotatedZ = 1.0f - localZ;
            }
            case WEST -> {
                rotatedX = localZ;
                rotatedZ = 1.0f - localX;
            }
            default -> {
                rotatedX = localX;
                rotatedZ = localZ;
            }
        }

        return new ParticlePoint(
                blockPos.getX() + rotatedX,
                blockPos.getY() + localY,
                blockPos.getZ() + rotatedZ
        );
    }

    private static float centeredNoise(
            long seed
    ) {
        return unitFloat(seed) - 0.5f;
    }

    private static float unitFloat(
            long seed
    ) {
        long mixed =
                mix64(
                        seed
                );

        return (
                (mixed >>> 40) & 0xFFFFFFL
        ) / (float) 0xFFFFFFL;
    }

    private static long mix64(
            long value
    ) {
        value =
                (value ^ (value >>> 33))
                        * 0xff51afd7ed558ccdL;
        value =
                (value ^ (value >>> 33))
                        * 0xc4ceb9fe1a85ec53L;
        return value ^ (value >>> 33);
    }

    private record ParticlePoint(
            double x,
            double y,
            double z
    ) {
    }
}
