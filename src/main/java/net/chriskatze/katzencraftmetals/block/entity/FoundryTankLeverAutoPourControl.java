package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Detects global auto-pour levers for one connected Foundry Tank network.
 *
 * Only powered vanilla-style levers count. Random powered blocks, dust, buttons,
 * redstone torches, repeaters, comparators, and pressure plates do not count.
 *
 * A lever counts if it is attached to:
 * - one of the connected tank blocks
 * - or a block directly adjacent to one of the connected tank blocks
 * - or a solid support block directly connected to one of those adjacent blocks
 * - or one of the attached faucet blocks
 * - or a block directly adjacent to one of the attached faucet blocks
 * - or a solid support block directly connected to one of those adjacent blocks
 *
 * This preserves the original bounded control-rim behavior without restoring
 * Tank BlockEntities. Faucet ticks reuse their cached Controller lookup.
 */
final class FoundryTankLeverAutoPourControl {

    /* One bounded lever scan per Controller/tick, not once per Faucet/tick. */
    private static final Map<FoundryControllerBlockEntity, CachedLeverState>
            LEVER_STATE_CACHE = new WeakHashMap<>();

    private FoundryTankLeverAutoPourControl() {
    }

    static boolean isAutoPourEnabledForFaucet(
            Level level,
            BlockPos faucetPos,
            BlockState faucetState,
            FoundryFaucetBlockEntity faucet
    ) {
        if (
                level == null
                        || faucet == null
                        || faucetState == null
                        || !faucetState.hasProperty(
                        FoundryFaucetBlock.FACING
                )
        ) {
            return false;
        }

        Direction facing =
                faucetState.getValue(
                        FoundryFaucetBlock.FACING
                );

        BlockPos tankPos =
                faucetPos.relative(
                        facing.getOpposite()
                );

        FoundryTankNetwork network =
                faucet.resolveOwnedNetworkForTank(
                        tankPos
                );

        return isAutoPourEnabled(
                level,
                network
        );
    }

    static boolean isAutoPourEnabled(
            Level level,
            @Nullable FoundryTankNetwork network
    ) {
        if (
                level == null
                        || network == null
                        || !network.isActive()
        ) {
            return false;
        }

        FoundryControllerBlockEntity controller =
                network.getAttachedController();

        if (controller != null) {
            long gameTime = level.getGameTime();
            long structureRevision = network.getStructureRevision();
            CachedLeverState cached = LEVER_STATE_CACHE.get(controller);
            if (cached == null || cached.structureRevision != structureRevision || cached.world.get() != level) {
                cached = new CachedLeverState(level, structureRevision, network.getTankPositions());
                LEVER_STATE_CACHE.put(controller, cached);
            }
            if (cached.gameTime != gameTime) {
                cached.enabled = cached.search.enabled(level);
                cached.gameTime = gameTime;
            }
            return cached.enabled;
        }

        return scanAutoPourEnabled(level, network);
    }


    private static boolean scanAutoPourEnabled(Level level, FoundryTankNetwork network) {
        return new LeverSearch(network.getTankPositions()).enabled(level);
    }

    /** Bounded geometry only. Read lever power, faucet attachment and support sturdiness LIVE.
     * No world-state result is retained across ticks; levers/support edits need no event hooks.
     */
    static final class LeverSearch {
        private record Origin(BlockPos pos, Direction faucetFacing) {
            boolean active(Level level) {
                if (faucetFacing == null) return true;
                if (!level.hasChunkAt(pos)) return false;
                BlockState state = level.getBlockState(pos);
                return state.getBlock() instanceof FoundryFaucetBlock
                        && state.hasProperty(FoundryFaucetBlock.FACING)
                        && state.getValue(FoundryFaucetBlock.FACING) == faucetFacing;
            }
        }
        private record Path(Origin origin, BlockPos support) {}
        private final Map<BlockPos, List<Path>> anchors = new java.util.LinkedHashMap<>();
        private final BlockPos[] probes;

        LeverSearch(Set<BlockPos> tanks) {
            for (BlockPos tank : tanks) {
                addOrigin(new Origin(tank.immutable(), null));
                for (Direction facing : FoundryFaucetBlock.FACING.getPossibleValues())
                    addOrigin(new Origin(tank.relative(facing), facing));
            }
            Set<BlockPos> candidates = new java.util.LinkedHashSet<>();
            for (BlockPos anchor : anchors.keySet())
                for (Direction direction : DIRECTIONS) candidates.add(anchor.relative(direction));
            probes = candidates.toArray(BlockPos[]::new);
        }

        private void add(BlockPos anchor, Origin origin, BlockPos support) {
            anchors.computeIfAbsent(anchor, ignored -> new ArrayList<>()).add(new Path(origin, support));
        }
        private void addOrigin(Origin origin) {
            add(origin.pos(), origin, null);
            for (Direction direction : DIRECTIONS) {
                BlockPos direct = origin.pos().relative(direction);
                add(direct, origin, null);
                for (Direction extension : DIRECTIONS) add(direct.relative(extension), origin, direct);
            }
        }
        boolean enabled(Level level) {
            // Probe each possible lever position once, instead of six neighbors per anchor.
            // Most blocks are not powered levers: never scan supports for those blocks.
            for (BlockPos lever : probes) {
                if (!level.hasChunkAt(lever)) continue;
                BlockState state = level.getBlockState(lever);
                if (!isPoweredLever(state)) continue;
                BlockPos anchor = getLeverAttachedBlockPos(lever, state);
                List<Path> paths = anchors.get(anchor);
                if (paths == null) continue;
                for (Path path : paths) if (path.origin().active(level)
                        && (path.support() == null || canExtendLeverAnchorsThroughSupportBlock(level, path.support())))
                    return true;
            }
            return false;
        }
    }

    private static final Direction[] DIRECTIONS = Direction.values();

    private static boolean canExtendLeverAnchorsThroughSupportBlock(
            Level level,
            BlockPos supportPos
    ) {
        if (!level.hasChunkAt(supportPos)) return false;
        BlockState state =
                level.getBlockState(
                        supportPos
                );

        if (
                state.isAir()
                        || !state.getFluidState()
                        .isEmpty()
        ) {
            return false;
        }

        for (Direction direction : DIRECTIONS) {
            if (state.isFaceSturdy(
                    level,
                    supportPos,
                    direction
            )) {
                return true;
            }
        }

        return false;
    }

    private static boolean isPoweredLever(
            BlockState state
    ) {
        return state.getBlock() instanceof LeverBlock
                && state.hasProperty(
                BlockStateProperties.POWERED
        )
                && state.getValue(
                BlockStateProperties.POWERED
        );
    }

    @Nullable
    private static BlockPos getLeverAttachedBlockPos(
            BlockPos leverPos,
            BlockState leverState
    ) {
        if (!leverState.hasProperty(
                BlockStateProperties.ATTACH_FACE
        )) {
            return null;
        }

        AttachFace face =
                leverState.getValue(
                        BlockStateProperties.ATTACH_FACE
                );

        Direction attachedBlockDirection =
                switch (face) {
                    case FLOOR -> Direction.DOWN;
                    case CEILING -> Direction.UP;
                    case WALL -> {
                        if (!leverState.hasProperty(
                                BlockStateProperties.HORIZONTAL_FACING
                        )) {
                            yield null;
                        }

                        yield leverState.getValue(
                                BlockStateProperties.HORIZONTAL_FACING
                        ).getOpposite();
                    }
                };

        if (attachedBlockDirection == null) {
            return null;
        }

        return leverPos.relative(
                attachedBlockDirection
        );
    }

    private static final class CachedLeverState {
        final java.lang.ref.WeakReference<Level> world;
        final long structureRevision;
        final LeverSearch search;
        long gameTime = Long.MIN_VALUE;
        boolean enabled;
        CachedLeverState(Level level, long revision, Set<BlockPos> tanks) {
            world = new java.lang.ref.WeakReference<>(level);
            structureRevision = revision;
            search = new LeverSearch(tanks);
        }
    }

}
