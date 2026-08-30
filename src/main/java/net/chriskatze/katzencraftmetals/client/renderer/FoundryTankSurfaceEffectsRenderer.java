package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankNetwork;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.HOT_SPOT_COUNT;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.HOT_SPOT_MARGIN;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.HOT_SPOT_MAX_SIZE;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.HOT_SPOT_MIN_SIZE;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.HOT_SPOT_Y_OFFSET;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.LIQUID_EPSILON;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.SURFACE_PARTICLE_INTERVAL_TICKS;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.SURFACE_PARTICLE_TOP_LIMIT;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.SURFACE_SMOKE_HEADROOM_REQUIRED;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.SURFACE_SMOKE_PARTICLE_SPAWN_OFFSET;

/**
 * Adds subtle ambient cues to the exposed top molten surface inside tanks:
 *
 * - tiny hot spots / bubble-like flickers on the molten surface
 * - occasional smoke particles in the available enclosed headspace
 *
 * Tanks intentionally do not spawn lava particles. The storage tanks are a
 * closed system; real lava particles are reserved for active pouring and the
 * casting target where splashing looks believable.
 */
final class FoundryTankSurfaceEffectsRenderer {

    private static final double FULL_HOT_SPOT_DISTANCE_SQ =
            12.0D * 12.0D;

    private static final double MEDIUM_HOT_SPOT_DISTANCE_SQ =
            18.0D * 18.0D;

    private static final double MAX_HOT_SPOT_DISTANCE_SQ =
            24.0D * 24.0D;

    private static final double MAX_SMOKE_DISTANCE_SQ =
            24.0D * 24.0D;

    private static final int LARGE_NETWORK_TANK_COUNT =
            32;

    private static final int HUGE_NETWORK_TANK_COUNT =
            48;

    private static final int MEDIUM_HOT_SPOT_SELECTION_MODULO =
            2;

    private static final int FAR_HOT_SPOT_SELECTION_MODULO =
            3;

    private static final long STALE_SMOKE_NETWORK_ENTRY_PRUNE_TICKS =
            200L;

    private final Map<FoundryTankBlockEntity, Long> lastParticleTickByTank =
            new WeakHashMap<>();

    private final Map<SurfaceEffectNetworkKey, Long> lastSmokeAttemptTickByNetwork =
            new HashMap<>();

    void renderAndSpawn(
            FoundryTankBlockEntity tank,
            MoltenMetalDefinition definition,
            FoundryTankLiquidGeometry geometry,
            float partialTick,
            PoseStack.Pose pose,
            MultiBufferSource bufferSource,
            int packedOverlay,
            float frameMinV,
            float frameMaxV
    ) {
        if (geometry.surfaceY() - geometry.minY() <= LIQUID_EPSILON) {
            return;
        }

        double distanceSquared =
                distanceSquaredToCamera(
                        tank
                );

        int networkTankCount =
                networkTankCount(
                        tank
                );

        int hotSpotCount =
                adjustedHotSpotCountForDistanceAndNetworkSize(
                        hotSpotCountForDistance(
                                distanceSquared
                        ),
                        distanceSquared,
                        networkTankCount
                );

        if (
                hotSpotCount > 0
                        && shouldRenderHotSpotsForTank(
                        tank,
                        distanceSquared,
                        networkTankCount
                )
        ) {
            renderHotSpots(
                    tank,
                    definition,
                    geometry,
                    partialTick,
                    pose,
                    bufferSource,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    hotSpotCount
            );
        }

        if (distanceSquared <= MAX_SMOKE_DISTANCE_SQ) {
            maybeSpawnSmokeParticle(
                    tank,
                    geometry
            );
        }
    }

    private static int hotSpotCountForDistance(
            double distanceSquared
    ) {
        if (distanceSquared <= FULL_HOT_SPOT_DISTANCE_SQ) {
            return HOT_SPOT_COUNT;
        }

        if (distanceSquared <= MEDIUM_HOT_SPOT_DISTANCE_SQ) {
            return Math.max(
                    1,
                    HOT_SPOT_COUNT / 2
            );
        }

        if (distanceSquared <= MAX_HOT_SPOT_DISTANCE_SQ) {
            return 1;
        }

        return 0;
    }

    private static int adjustedHotSpotCountForDistanceAndNetworkSize(
            int baseHotSpotCount,
            double distanceSquared,
            int networkTankCount
    ) {
        if (baseHotSpotCount <= 0) {
            return 0;
        }

        if (
                networkTankCount >= HUGE_NETWORK_TANK_COUNT
                        && distanceSquared <= FULL_HOT_SPOT_DISTANCE_SQ
        ) {
            return Math.max(
                    1,
                    baseHotSpotCount / 2
            );
        }

        if (networkTankCount >= HUGE_NETWORK_TANK_COUNT) {
            return 1;
        }

        if (
                networkTankCount >= LARGE_NETWORK_TANK_COUNT
                        && distanceSquared > FULL_HOT_SPOT_DISTANCE_SQ
        ) {
            return Math.max(
                    1,
                    baseHotSpotCount / 2
            );
        }

        return baseHotSpotCount;
    }

    private static boolean shouldRenderHotSpotsForTank(
            FoundryTankBlockEntity tank,
            double distanceSquared,
            int networkTankCount
    ) {
        if (
                networkTankCount < LARGE_NETWORK_TANK_COUNT
                        || distanceSquared <= FULL_HOT_SPOT_DISTANCE_SQ
        ) {
            return true;
        }

        int modulo =
                distanceSquared <= MEDIUM_HOT_SPOT_DISTANCE_SQ
                        ? MEDIUM_HOT_SPOT_SELECTION_MODULO
                        : FAR_HOT_SPOT_SELECTION_MODULO;

        long seed =
                tank.getBlockPos()
                        .asLong()
                        ^ (long) networkTankCount
                        * 0x9E3779B97F4A7C15L;

        return Math.floorMod(
                mix64(
                        seed
                ),
                modulo
        ) == 0L;
    }

    private static int networkTankCount(
            FoundryTankBlockEntity tank
    ) {
        FoundryTankNetwork network =
                tank.getNetwork();

        if (network == null) {
            return 1;
        }

        return Math.max(
                1,
                network.getTankPositions()
                        .size()
        );
    }

    private static double distanceSquaredToCamera(
            FoundryTankBlockEntity tank
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        Entity camera =
                minecraft.getCameraEntity();

        if (camera == null) {
            return Double.POSITIVE_INFINITY;
        }

        BlockPos pos =
                tank.getBlockPos();

        double centerX =
                pos.getX() + 0.5D;

        double centerY =
                pos.getY() + 0.5D;

        double centerZ =
                pos.getZ() + 0.5D;

        double dx =
                camera.getX() - centerX;

        double dy =
                camera.getY() - centerY;

        double dz =
                camera.getZ() - centerZ;

        return dx * dx
                + dy * dy
                + dz * dz;
    }

    private void renderHotSpots(
            FoundryTankBlockEntity tank,
            MoltenMetalDefinition definition,
            FoundryTankLiquidGeometry geometry,
            float partialTick,
            PoseStack.Pose pose,
            MultiBufferSource bufferSource,
            int packedOverlay,
            float frameMinV,
            float frameMaxV,
            int hotSpotCount
    ) {
        VertexConsumer consumer =
                bufferSource.getBuffer(
                        RenderType.entityTranslucent(
                                definition.animatedTexture()
                        )
                );

        long gameTime =
                tank.getLevel()
                        .getGameTime();

        float usableMinX =
                geometry.minX() + HOT_SPOT_MARGIN;

        float usableMaxX =
                geometry.maxX() - HOT_SPOT_MARGIN;

        float usableMinZ =
                geometry.minZ() + HOT_SPOT_MARGIN;

        float usableMaxZ =
                geometry.maxZ() - HOT_SPOT_MARGIN;

        if (
                usableMaxX - usableMinX <= LIQUID_EPSILON
                        || usableMaxZ - usableMinZ <= LIQUID_EPSILON
        ) {
            return;
        }

        float y =
                Math.min(
                        1.0f,
                        geometry.surfaceY()
                                + HOT_SPOT_Y_OFFSET
                );

        int count =
                Math.min(
                        HOT_SPOT_COUNT,
                        Math.max(
                                0,
                                hotSpotCount
                        )
                );

        for (int index = 0; index < count; index++) {
            float pulse =
                    hotSpotPulse(
                            tank.getBlockPos(),
                            index,
                            gameTime + partialTick
                    );

            if (pulse <= 0.18f) {
                continue;
            }

            float centerX =
                    Mth.lerp(
                            stableUnitFloat(
                                    tank.getBlockPos(),
                                    index,
                                    17L
                            ),
                            usableMinX,
                            usableMaxX
                    );

            float centerZ =
                    Mth.lerp(
                            stableUnitFloat(
                                    tank.getBlockPos(),
                                    index,
                                    41L
                            ),
                            usableMinZ,
                            usableMaxZ
                    );

            float outerSize =
                    Mth.lerp(
                            pulse,
                            HOT_SPOT_MIN_SIZE,
                            HOT_SPOT_MAX_SIZE
                    );

            float innerSize =
                    outerSize * 0.50f;

            int outerColor =
                    hotSpotColor(
                            definition.id(),
                            (int) Mth.lerp(
                                    pulse,
                                    195.0f,
                                    230.0f
                            )
                    );

            int innerColor =
                    hotSpotColor(
                            definition.id(),
                            (int) Mth.lerp(
                                    pulse,
                                    242.0f,
                                    255.0f
                            )
                    );

            renderTopSquare(
                    consumer,
                    pose,
                    centerX,
                    centerZ,
                    y,
                    outerSize,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    outerColor
            );

            renderTopSquare(
                    consumer,
                    pose,
                    centerX,
                    centerZ,
                    y + 0.0003f,
                    innerSize,
                    packedOverlay,
                    frameMinV,
                    frameMaxV,
                    innerColor
            );
        }
    }

    private void maybeSpawnSmokeParticle(
            FoundryTankBlockEntity tank,
            FoundryTankLiquidGeometry geometry
    ) {
        long gameTime =
                tank.getLevel()
                        .getGameTime();

        Long lastProcessedTick =
                lastParticleTickByTank.get(
                        tank
                );

        if (
                lastProcessedTick != null
                        && lastProcessedTick == gameTime
        ) {
            return;
        }

        lastParticleTickByTank.put(
                tank,
                gameTime
        );

        if (gameTime % SURFACE_PARTICLE_INTERVAL_TICKS != 0L) {
            return;
        }

        if (!claimNetworkSmokeAttempt(
                tank,
                gameTime
        )) {
            return;
        }

        double topCeilingY =
                resolveParticleCeilingWorldY(
                        tank
                );

        double spawnSurfaceY =
                tank.getBlockPos().getY()
                        + geometry.surfaceY()
                        + SURFACE_SMOKE_PARTICLE_SPAWN_OFFSET;

        double headroom =
                topCeilingY - spawnSurfaceY;

        if (headroom < SURFACE_SMOKE_HEADROOM_REQUIRED) {
            return;
        }

        BlockPos pos =
                tank.getBlockPos();

        long cycleSeed =
                mix64(
                        pos.asLong()
                                ^ (gameTime * 0x9E3779B97F4A7C15L)
                );

        if (unitFloat(cycleSeed ^ 0x51A7L) >= 0.45f) {
            return;
        }

        double spawnX =
                pos.getX()
                        + Mth.lerp(
                        stableUnitFloat(
                                pos,
                                0,
                                gameTime ^ 0x1337L
                        ),
                        geometry.minX() + 0.08f,
                        geometry.maxX() - 0.08f
                );

        double spawnY =
                Math.min(
                        spawnSurfaceY,
                        topCeilingY - SURFACE_SMOKE_HEADROOM_REQUIRED
                );

        double spawnZ =
                pos.getZ()
                        + Mth.lerp(
                        stableUnitFloat(
                                pos,
                                1,
                                gameTime ^ 0xD00DL
                        ),
                        geometry.minZ() + 0.08f,
                        geometry.maxZ() - 0.08f
                );

        double upwardVelocity =
                Math.min(
                        0.008d
                                + unitFloat(
                                cycleSeed ^ 0x7102L
                        ) * 0.004d,
                        Math.max(
                                0.0d,
                                headroom * 0.028d
                        )
                );

        tank.getLevel().addParticle(
                ParticleTypes.SMOKE,
                spawnX,
                spawnY,
                spawnZ,
                horizontalDrift(
                        cycleSeed ^ 0x7101L
                ),
                upwardVelocity,
                horizontalDrift(
                        cycleSeed ^ 0x7103L
                )
        );
    }

    private boolean claimNetworkSmokeAttempt(
            FoundryTankBlockEntity tank,
            long gameTime
    ) {
        if (gameTime % STALE_SMOKE_NETWORK_ENTRY_PRUNE_TICKS == 0L) {
            lastSmokeAttemptTickByNetwork.entrySet()
                    .removeIf(
                            entry ->
                                    gameTime
                                            - entry.getValue()
                                            > STALE_SMOKE_NETWORK_ENTRY_PRUNE_TICKS
                    );
        }

        SurfaceEffectNetworkKey key =
                surfaceEffectNetworkKey(
                        tank
                );

        Long lastProcessedTick =
                lastSmokeAttemptTickByNetwork.get(
                        key
                );

        if (
                lastProcessedTick != null
                        && lastProcessedTick == gameTime
        ) {
            return false;
        }

        lastSmokeAttemptTickByNetwork.put(
                key,
                gameTime
        );

        return true;
    }

    private static SurfaceEffectNetworkKey surfaceEffectNetworkKey(
            FoundryTankBlockEntity tank
    ) {
        Level level =
                tank.getLevel();

        FoundryTankNetwork network =
                tank.getNetwork();

        if (network == null) {
            return new SurfaceEffectNetworkKey(
                    level,
                    null,
                    tank.getBlockPos()
                            .immutable()
            );
        }

        UUID ownerId =
                network.getOwnerId();

        if (ownerId != null) {
            return new SurfaceEffectNetworkKey(
                    level,
                    ownerId,
                    BlockPos.ZERO
            );
        }

        return new SurfaceEffectNetworkKey(
                level,
                null,
                findNetworkAnchor(
                        network.getTankPositions(),
                        tank.getBlockPos()
                )
        );
    }

    private static BlockPos findNetworkAnchor(
            Set<BlockPos> positions,
            BlockPos fallback
    ) {
        BlockPos anchor =
                fallback;

        for (BlockPos position : positions) {
            if (
                    position.getY() < anchor.getY()
                            || (
                            position.getY() == anchor.getY()
                                    && position.getX() < anchor.getX()
                    )
                            || (
                            position.getY() == anchor.getY()
                                    && position.getX() == anchor.getX()
                                    && position.getZ() < anchor.getZ()
                    )
            ) {
                anchor =
                        position;
            }
        }

        return anchor.immutable();
    }

    private static double resolveParticleCeilingWorldY(
            FoundryTankBlockEntity tank
    ) {
        FoundryTankNetwork network =
                tank.getNetwork();

        Set<BlockPos> positions =
                network != null
                        ? network.getTankPositions()
                        : Set.of(
                        tank.getBlockPos()
                );

        int highestTankY =
                tank.getBlockPos()
                        .getY();

        for (BlockPos tankPos : positions) {
            if (tankPos.getY() > highestTankY) {
                highestTankY =
                        tankPos.getY();
            }
        }

        return highestTankY + SURFACE_PARTICLE_TOP_LIMIT;
    }

    private static double horizontalDrift(
            long seed
    ) {
        return (
                unitFloat(seed) - 0.5d
        ) * 0.008d;
    }

    private static float hotSpotPulse(
            BlockPos pos,
            int index,
            double time
    ) {
        float speed =
                0.055f
                        + 0.012f * index;

        float phaseOffset =
                stableUnitFloat(
                        pos,
                        index,
                        99L
                ) * 6.2831855f;

        float pulse =
                (
                        Mth.sin(
                                (float) (time * speed)
                                        + phaseOffset
                        ) + 1.0f
                ) * 0.5f;

        return pulse * pulse;
    }

    private static int hotSpotColor(
            ResourceLocation metalId,
            int alpha
    ) {
        int rgb =
                switch (metalId.getPath()) {
                    case "gold" -> 0xFFE37A;
                    case "copper" -> 0xFF9A52;
                    case "mythril" -> 0xAEEBFF;
                    case "platinum" -> 0xFFF4D8;
                    case "steel" -> 0xFFB06A;
                    case "iron" -> 0xFF9F58;
                    default -> 0xFFB56A;
                };

        return (
                Mth.clamp(
                        alpha,
                        0,
                        255
                ) << 24
        ) | rgb;
    }

    private static float stableUnitFloat(
            BlockPos pos,
            int index,
            long salt
    ) {
        long seed =
                mix64(
                        pos.asLong()
                                ^ (salt * 0x9E3779B97F4A7C15L)
                                ^ (long) index * 0xC2B2AE3D27D4EB4FL
                );

        return unitFloat(seed);
    }

    private static float unitFloat(
            long seed
    ) {
        return (
                (seed >>> 40) & 0xFFFFFFL
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

    private static void renderTopSquare(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float centerX,
            float centerZ,
            float y,
            float size,
            int packedOverlay,
            float frameMinV,
            float frameMaxV,
            int color
    ) {
        float minX =
                centerX - size * 0.5f;

        float maxX =
                centerX + size * 0.5f;

        float minZ =
                centerZ - size * 0.5f;

        float maxZ =
                centerZ + size * 0.5f;

        renderColoredQuad(
                consumer,
                pose,
                minX,
                y,
                minZ,
                minX,
                y,
                maxZ,
                maxX,
                y,
                maxZ,
                maxX,
                y,
                minZ,
                minX,
                minZ,
                minX,
                maxZ,
                maxX,
                maxZ,
                maxX,
                minZ,
                0.0f,
                1.0f,
                0.0f,
                packedOverlay,
                frameMinV,
                frameMaxV,
                color
        );
    }

    private static void renderColoredQuad(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float x4,
            float y4,
            float z4,
            float u1,
            float localV1,
            float u2,
            float localV2,
            float u3,
            float localV3,
            float u4,
            float localV4,
            float normalX,
            float normalY,
            float normalZ,
            int packedOverlay,
            float frameMinV,
            float frameMaxV,
            int color
    ) {
        addColoredVertex(
                consumer,
                pose,
                x1,
                y1,
                z1,
                u1,
                localV1,
                normalX,
                normalY,
                normalZ,
                packedOverlay,
                frameMinV,
                frameMaxV,
                color
        );

        addColoredVertex(
                consumer,
                pose,
                x2,
                y2,
                z2,
                u2,
                localV2,
                normalX,
                normalY,
                normalZ,
                packedOverlay,
                frameMinV,
                frameMaxV,
                color
        );

        addColoredVertex(
                consumer,
                pose,
                x3,
                y3,
                z3,
                u3,
                localV3,
                normalX,
                normalY,
                normalZ,
                packedOverlay,
                frameMinV,
                frameMaxV,
                color
        );

        addColoredVertex(
                consumer,
                pose,
                x4,
                y4,
                z4,
                u4,
                localV4,
                normalX,
                normalY,
                normalZ,
                packedOverlay,
                frameMinV,
                frameMaxV,
                color
        );
    }

    private static void addColoredVertex(
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
            float frameMaxV,
            int color
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
                .setColor(color)
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
    private record SurfaceEffectNetworkKey(
            Level level,
            UUID ownerId,
            BlockPos fallbackAnchor
    ) {
    }

}
