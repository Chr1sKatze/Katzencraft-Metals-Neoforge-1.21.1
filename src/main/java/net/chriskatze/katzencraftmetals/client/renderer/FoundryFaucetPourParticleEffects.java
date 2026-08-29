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
            7L;

    private static final float OUTLET_START_PROGRESS =
            0.18f;

    private static final float BASIN_START_PROGRESS =
            0.88f;

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

        if (animationProgress >= BASIN_START_PROGRESS) {
            maybeSpawnBasinParticles(
                    faucet,
                    facing,
                    context,
                    gameTime
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
            long gameTime
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

        if (gameTime % BASIN_INTERVAL_TICKS != 0L) {
            return;
        }

        long seed =
                mix64(
                        faucet.getBlockPos()
                                .asLong()
                                ^ gameTime
                                ^ 0xCA1170L
                );

        if (unitFloat(seed ^ 0x2001L) < 0.42f) {
            spawnBasinLava(
                    faucet,
                    facing,
                    context,
                    seed
            );
        }

        if (unitFloat(seed ^ 0x2002L) < 0.28f) {
            spawnBasinSmoke(
                    faucet,
                    facing,
                    context,
                    seed
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
            long seed
    ) {
        ParticlePoint point =
                localToWorld(
                        faucet.getBlockPos(),
                        facing,
                        STREAM_CENTER_X
                                + centeredNoise(seed ^ 0x5001L) * 0.12f,
                        context.cauldronSurfaceY()
                                + 0.035f,
                        STREAM_CENTER_Z
                                + centeredNoise(seed ^ 0x5002L) * 0.12f
                );

        faucet.getLevel().addParticle(
                ParticleTypes.LAVA,
                point.x(),
                point.y(),
                point.z(),
                centeredNoise(seed ^ 0x5003L) * 0.010d,
                0.010d,
                centeredNoise(seed ^ 0x5004L) * 0.010d
        );
    }

    private static void spawnBasinSmoke(
            FoundryFaucetBlockEntity faucet,
            Direction facing,
            FoundryFaucetRenderContext context,
            long seed
    ) {
        ParticlePoint point =
                localToWorld(
                        faucet.getBlockPos(),
                        facing,
                        STREAM_CENTER_X
                                + centeredNoise(seed ^ 0x6001L) * 0.14f,
                        context.cauldronSurfaceY()
                                + 0.055f,
                        STREAM_CENTER_Z
                                + centeredNoise(seed ^ 0x6002L) * 0.14f
                );

        faucet.getLevel().addParticle(
                ParticleTypes.SMOKE,
                point.x(),
                point.y(),
                point.z(),
                centeredNoise(seed ^ 0x6003L) * 0.008d,
                0.014d,
                centeredNoise(seed ^ 0x6004L) * 0.008d
        );
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
