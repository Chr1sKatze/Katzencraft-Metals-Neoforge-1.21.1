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
import java.util.HashSet;
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

            if (
                    cached != null
                            && cached.gameTime() == gameTime
                            && cached.structureRevision() == structureRevision
            ) {
                return cached.enabled();
            }

            boolean enabled = scanAutoPourEnabled(level, network);
            LEVER_STATE_CACHE.put(
                    controller,
                    new CachedLeverState(
                            gameTime,
                            structureRevision,
                            enabled
                    )
            );
            return enabled;
        }

        return scanAutoPourEnabled(level, network);
    }


    private static boolean scanAutoPourEnabled(
            Level level,
            FoundryTankNetwork network
    ) {
        Set<BlockPos> candidateAnchors =
                collectLeverAnchorCandidates(
                        level,
                        network
                );

        for (BlockPos anchorPos : candidateAnchors) {
            if (hasPoweredLeverAttachedToAnchor(
                    level,
                    anchorPos
            )) {
                return true;
            }
        }

        return false;
    }

    private static Set<BlockPos> collectLeverAnchorCandidates(
            Level level,
            FoundryTankNetwork network
    ) {
        Set<BlockPos> anchors =
                new HashSet<>();

        for (BlockPos tankPos : network.getTankPositions()) {
            addFoundryBlockLeverAnchors(
                    level,
                    anchors,
                    tankPos
            );
        }

        /*
         * Faucets are foundry attachments, so a lever around an attached
         * Faucet remains part of the same bounded control area.
         */
        for (
                BlockPos faucetPos : FoundryTankNetwork.findAttachedFaucets(
                level,
                network.getTankPositions()
        )
        ) {
            addFoundryBlockLeverAnchors(
                    level,
                    anchors,
                    faucetPos
            );
        }

        return anchors;
    }

    private static void addFoundryBlockLeverAnchors(
            Level level,
            Set<BlockPos> anchors,
            BlockPos foundryBlockPos
    ) {
        anchors.add(
                foundryBlockPos.immutable()
        );

        List<BlockPos> directSupportCandidates =
                new ArrayList<>();

        for (Direction direction : Direction.values()) {
            BlockPos directNeighbor =
                    foundryBlockPos.relative(
                            direction
                    ).immutable();

            anchors.add(
                    directNeighbor
            );

            directSupportCandidates.add(
                    directNeighbor
            );
        }

        /*
         * One additional bounded step through a solid support block preserves
         * the small control rim/platform behavior without flood-filling through
         * an entire building.
         */
        for (BlockPos supportPos : directSupportCandidates) {
            if (!canExtendLeverAnchorsThroughSupportBlock(
                    level,
                    supportPos
            )) {
                continue;
            }

            for (Direction direction : Direction.values()) {
                anchors.add(
                        supportPos.relative(
                                direction
                        ).immutable()
                );
            }
        }
    }

    private static boolean canExtendLeverAnchorsThroughSupportBlock(
            Level level,
            BlockPos supportPos
    ) {
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

        for (Direction direction : Direction.values()) {
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

    private static boolean hasPoweredLeverAttachedToAnchor(
            Level level,
            BlockPos anchorPos
    ) {
        for (Direction direction : Direction.values()) {
            BlockPos leverPos =
                    anchorPos.relative(
                            direction
                    );

            BlockState leverState =
                    level.getBlockState(
                            leverPos
                    );

            if (!isPoweredLever(leverState)) {
                continue;
            }

            BlockPos attachedBlockPos =
                    getLeverAttachedBlockPos(
                            leverPos,
                            leverState
                    );

            if (anchorPos.equals(attachedBlockPos)) {
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

    private record CachedLeverState(
            long gameTime,
            long structureRevision,
            boolean enabled
    ) {
    }

}
