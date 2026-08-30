package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.block.custom.FoundryFaucetBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
 * This allows a small attached control rim/platform around the foundry, without
 * flood-filling through an entire building.
 */
final class FoundryTankLeverAutoPourControl {

    private FoundryTankLeverAutoPourControl() {
    }

    static boolean isAutoPourEnabledForFaucet(
            Level level,
            BlockPos faucetPos,
            BlockState faucetState
    ) {
        if (
                level == null
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

        BlockEntity blockEntity =
                level.getBlockEntity(
                        tankPos
                );

        if (!(blockEntity instanceof FoundryTankBlockEntity tank)) {
            return false;
        }

        FoundryTankNetwork network =
                tank.getNetwork();

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
         * Free tank-side Faucets are treated as foundry attachments instead of
         * crafted standalone gameplay blocks. For lever auto-pour, that means
         * levers around the attached Faucet should count as part of the same
         * foundry control area.
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

            /*
             * Existing behavior: levers attached directly to a neighboring block
             * around the tank/faucet count.
             */
            anchors.add(
                    directNeighbor
            );

            directSupportCandidates.add(
                    directNeighbor
            );
        }

        /*
         * New behavior: one extra bounded step through solid blocks directly
         * touching the foundry edge. This supports a small rim/platform like:
         *
         * [P][P][P][P]
         * [T][T][T][P]
         * [T][T][T][P]
         *
         * without searching through the rest of the building.
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

        /*
         * Do not extend through random non-solid things. If a block has at least
         * one sturdy face, it is a reasonable lever support / control-platform
         * block.
         */
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
}
