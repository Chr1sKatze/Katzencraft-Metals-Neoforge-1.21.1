package net.chriskatze.katzencraftmetals.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankNetwork;
import net.chriskatze.katzencraftmetals.metal.FoundryMetalLayer;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Dynamically renders the complete visual Foundry Tank multiblock.
 *
 * This class intentionally only coordinates the render order:
 *
 * 1. connected exterior casing
 * 2. seamless molten-metal layers
 * 3. Faucet entrance overlays
 */
public class FoundryTankBlockEntityRenderer
        implements BlockEntityRenderer<FoundryTankBlockEntity> {

    private static final int MAX_VIEW_DISTANCE_CHUNKS =
            20;

    private static final int BLOCKS_PER_CHUNK =
            16;

    private static final int VIEW_DISTANCE =
            MAX_VIEW_DISTANCE_CHUNKS * BLOCKS_PER_CHUNK;

    private static final double FAUCET_OVERLAY_DISTANCE_SQ =
            50.0D * 50.0D;

    private final FoundryTankLiquidSmoother liquidSmoother =
            new FoundryTankLiquidSmoother();

    private final FoundryTankMoltenLayerBuilder moltenLayerBuilder =
            new FoundryTankMoltenLayerBuilder(
                    liquidSmoother
            );

    private final FoundryTankMoltenRenderer moltenRenderer =
            new FoundryTankMoltenRenderer(
                    moltenLayerBuilder
            );

    /*
     * Patch AD:
     *
     * AC cached these values only for one render frame. That barely helped,
     * because a Tank block entity is usually rendered once per frame.
     *
     * These values do not depend on partialTick. They are block/world structure
     * facts for the current client game tick:
     *
     * - whether this Tank is completely surrounded by the same component
     * - whether this Tank is the top Tank in its column
     * - whether this Tank has any attached faucet
     * - whether this Tank structure has any renderable molten metal
     *
     * Keeping the cache for the whole client tick lets every rendered frame
     * inside that tick reuse the expensive neighbor/network checks.
     */
    private final Map<BlockPos, Boolean> completelyHiddenTickCache =
            new HashMap<>();

    private final Map<BlockPos, Boolean> topTankTickCache =
            new HashMap<>();

    private final Map<BlockPos, Boolean> attachedFaucetTickCache =
            new HashMap<>();

    private final Map<TankStructureRenderKey, Boolean> emptyStructureTickCache =
            new HashMap<>();

    private Level renderTickCacheLevel;
    private long renderTickCacheGameTime =
            Long.MIN_VALUE;

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
        Level level =
                tank.getLevel();

        if (level == null) {
            return;
        }

        prepareRenderTickCache(
                level
        );

        boolean completelyHiddenInsideSameComponent =
                isCompletelyHiddenInsideSameComponentCached(
                        tank
                );

        boolean emptyStructure =
                isCurrentStructureEmptyCached(
                        tank
                );

        /*
         * Fully internal empty Tanks cannot draw anything:
         *
         * - no exterior casing
         * - no top hatch
         * - no faucet overlay
         * - no liquid
         *
         * Returning here preserves the exact same image while avoiding the
         * remaining hatch/liquid/overlay paths for internal empty Tanks.
         */
        if (
                completelyHiddenInsideSameComponent
                        && emptyStructure
        ) {
            return;
        }

        poseStack.pushPose();

        PoseStack.Pose pose =
                poseStack.last();

        if (!completelyHiddenInsideSameComponent) {
            FoundryTankCasingRenderer.render(
                    tank,
                    pose,
                    bufferSource,
                    packedLight,
                    packedOverlay
            );

            /*
             * Intake hatch geometry can only be visible on the top Tank of a
             * column. The hatch renderer already protects itself, but avoiding
             * the call here saves a large number of no-op renderer entries in
             * stacked 4x4x4 structures.
             */
            if (isTopTankCached(
                    tank
            )) {
                FoundryTankIntakeHatchRenderer.render(
                        tank,
                        pose,
                        bufferSource,
                        packedLight,
                        packedOverlay
                );
            }
        }

        /*
         * Patch AB already makes the molten renderer cheap for empty
         * structures, but skipping the call entirely here avoids entering the
         * liquid renderer from hundreds of empty Tank block entities.
         */
        if (!emptyStructure) {
            moltenRenderer.render(
                    tank,
                    partialTick,
                    pose,
                    bufferSource,
                    packedOverlay
            );
        }

        if (
                !completelyHiddenInsideSameComponent
                        && hasAttachedFaucetCached(
                        tank
                )
                        && shouldRenderFaucetOverlay(
                        tank
                )
        ) {
            /*
             * Render translucent entrance shadows after the molten metal so the
             * shadow blends over the visible liquid instead of hiding it through
             * the depth buffer.
             *
             * Past about 50 blocks this tiny entrance shadow is not readable,
             * but it still performs per-side checks and translucent buffer
             * work, so it is the only detail skipped in this LOD step.
             */
            FoundryTankFaucetOverlayRenderer.render(
                    tank,
                    partialTick,
                    pose,
                    bufferSource,
                    packedLight,
                    packedOverlay
            );
        }

        poseStack.popPose();
    }

    private void prepareRenderTickCache(
            Level level
    ) {
        long gameTime =
                level.getGameTime();

        if (
                renderTickCacheLevel != level
                        || renderTickCacheGameTime != gameTime
        ) {
            completelyHiddenTickCache.clear();
            topTankTickCache.clear();
            attachedFaucetTickCache.clear();
            emptyStructureTickCache.clear();

            renderTickCacheLevel =
                    level;

            renderTickCacheGameTime =
                    gameTime;
        }
    }

    private boolean isCompletelyHiddenInsideSameComponentCached(
            FoundryTankBlockEntity tank
    ) {
        BlockPos cacheKey =
                tank.getBlockPos()
                        .immutable();

        Boolean cached =
                completelyHiddenTickCache.get(
                        cacheKey
                );

        if (cached != null) {
            return cached;
        }

        boolean hidden =
                isCompletelyHiddenInsideSameComponent(
                        tank
                );

        completelyHiddenTickCache.put(
                cacheKey,
                hidden
        );

        return hidden;
    }

    private boolean isTopTankCached(
            FoundryTankBlockEntity tank
    ) {
        BlockPos cacheKey =
                tank.getBlockPos()
                        .immutable();

        Boolean cached =
                topTankTickCache.get(
                        cacheKey
                );

        if (cached != null) {
            return cached;
        }

        boolean topTank =
                tank.isTopTank();

        topTankTickCache.put(
                cacheKey,
                topTank
        );

        return topTank;
    }

    private boolean hasAttachedFaucetCached(
            FoundryTankBlockEntity tank
    ) {
        BlockPos cacheKey =
                tank.getBlockPos()
                        .immutable();

        Boolean cached =
                attachedFaucetTickCache.get(
                        cacheKey
                );

        if (cached != null) {
            return cached;
        }

        boolean hasAttachedFaucet =
                hasAttachedFaucet(
                        tank
                );

        attachedFaucetTickCache.put(
                cacheKey,
                hasAttachedFaucet
        );

        return hasAttachedFaucet;
    }

    private boolean isCurrentStructureEmptyCached(
            FoundryTankBlockEntity tank
    ) {
        TankStructureRenderKey cacheKey =
                createStructureRenderKey(
                        tank
                );

        Boolean cached =
                emptyStructureTickCache.get(
                        cacheKey
                );

        if (cached != null) {
            return cached;
        }

        boolean empty =
                isCurrentStructureEmpty(
                        tank
                );

        emptyStructureTickCache.put(
                cacheKey,
                empty
        );

        return empty;
    }

    private static boolean isCompletelyHiddenInsideSameComponent(
            FoundryTankBlockEntity tank
    ) {
        /*
         * A Tank that has same-component neighbors on all six sides cannot
         * expose casing, faucet overlays, or a top intake hatch.
         *
         * It may still need molten rendering if an empty Tank above makes the
         * liquid surface visible through the multiblock interior.
         */
        for (Direction direction : Direction.values()) {
            if (!FoundryTankVisualConnections.isSameComponent(
                    tank,
                    direction
            )) {
                return false;
            }
        }

        return true;
    }

    private static boolean hasAttachedFaucet(
            FoundryTankBlockEntity tank
    ) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (FoundryTankVisualConnections.hasAttachedFaucet(
                    tank,
                    direction
            )) {
                return true;
            }
        }

        return false;
    }

    private static boolean isCurrentStructureEmpty(
            FoundryTankBlockEntity tank
    ) {
        Level level =
                tank.getLevel();

        if (level == null) {
            return true;
        }

        FoundryTankNetwork network =
                tank.getNetwork();

        if (
                network == null
                        || network.getTankPositions()
                        .isEmpty()
        ) {
            return !tankHasAnyRenderableLocalMetal(
                    tank
            );
        }

        for (BlockPos tankPos : network.getTankPositions()) {
            if (
                    level.getBlockEntity(
                            tankPos
                    )
                            instanceof FoundryTankBlockEntity networkTank
                            && tankHasAnyRenderableLocalMetal(
                            networkTank
                    )
            ) {
                return false;
            }
        }

        return true;
    }

    private static boolean tankHasAnyRenderableLocalMetal(
            FoundryTankBlockEntity tank
    ) {
        for (FoundryMetalLayer layer : tank.getLocalMetalLayers()) {
            if (
                    layer.amount() > 0
                            && ModMoltenMetals.contains(
                            layer.metal()
                    )
            ) {
                return true;
            }
        }

        return false;
    }

    private static TankStructureRenderKey createStructureRenderKey(
            FoundryTankBlockEntity tank
    ) {
        Level level =
                tank.getLevel();

        FoundryTankNetwork network =
                tank.getNetwork();

        if (network != null) {
            UUID ownerId =
                    network.getOwnerId();

            if (ownerId != null) {
                return new TankStructureRenderKey(
                        level,
                        ownerId,
                        BlockPos.ZERO
                );
            }

            return new TankStructureRenderKey(
                    level,
                    null,
                    canonicalNetworkAnchor(
                            network.getTankPositions(),
                            tank.getBlockPos()
                    )
            );
        }

        return new TankStructureRenderKey(
                level,
                null,
                tank.getBlockPos()
                        .immutable()
        );
    }

    private static BlockPos canonicalNetworkAnchor(
            Set<BlockPos> positions,
            BlockPos fallback
    ) {
        if (
                positions == null
                        || positions.isEmpty()
        ) {
            return fallback.immutable();
        }

        BlockPos best =
                null;

        for (BlockPos position : positions) {
            if (
                    best == null
                            || compareBlockPositions(
                            position,
                            best
                    ) < 0
            ) {
                best =
                        position;
            }
        }

        return best == null
                ? fallback.immutable()
                : best.immutable();
    }

    private static int compareBlockPositions(
            BlockPos first,
            BlockPos second
    ) {
        int byY =
                Integer.compare(
                        first.getY(),
                        second.getY()
                );

        if (byY != 0) {
            return byY;
        }

        int byX =
                Integer.compare(
                        first.getX(),
                        second.getX()
                );

        if (byX != 0) {
            return byX;
        }

        return Integer.compare(
                first.getZ(),
                second.getZ()
        );
    }

    private static boolean shouldRenderFaucetOverlay(
            FoundryTankBlockEntity tank
    ) {
        Entity camera =
                Minecraft.getInstance()
                        .cameraEntity;

        if (camera == null) {
            return true;
        }

        double centerX =
                tank.getBlockPos()
                        .getX()
                        + 0.5D;

        double centerY =
                tank.getBlockPos()
                        .getY()
                        + 0.5D;

        double centerZ =
                tank.getBlockPos()
                        .getZ()
                        + 0.5D;

        double deltaX =
                camera.getX() - centerX;

        double deltaY =
                camera.getY() - centerY;

        double deltaZ =
                camera.getZ() - centerZ;

        double distanceSquared =
                deltaX * deltaX
                        + deltaY * deltaY
                        + deltaZ * deltaZ;

        return distanceSquared <= FAUCET_OVERLAY_DISTANCE_SQ;
    }

    @Override
    public boolean shouldRenderOffScreen(
            FoundryTankBlockEntity tank
    ) {
        /*
         * This renderer only draws the current Tank block and small local
         * overlays. It does not need to stay active when the block entity is
         * outside the camera frustum.
         *
         * Keeping this false lets Minecraft skip offscreen Tank block entities
         * normally, which matters a lot for large 4x4x4 foundries and multiple
         * foundries in one area.
         */
        return false;
    }

    @Override
    public int getViewDistance() {
        /*
         * Block-entity renderers have their own distance cap in addition to the
         * user's actual chunk render distance.
         *
         * This cap supports up to 20 chunks:
         *
         * 20 chunks * 16 blocks = 320 blocks.
         *
         * If the player's normal render distance is lower than 20 chunks, the
         * foundry still disappears earlier because those chunks are not rendered
         * / available to the client. This value only prevents the block-entity
         * renderer from cutting the foundry off early when the player has a high
         * render distance.
         */
        return VIEW_DISTANCE;
    }

    private record TankStructureRenderKey(
            Level level,
            UUID ownerId,
            BlockPos fallbackAnchor
    ) {
    }
}
