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

import java.util.HashSet;
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
 * - or one of the attached faucet blocks
 * - or a block directly adjacent to one of the attached faucet blocks
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
            addAnchorAndNeighbors(
                    anchors,
                    tankPos
            );
        }

        /*
         * Free tank-side Faucets are now treated as foundry attachments instead
         * of crafted standalone gameplay blocks. For lever auto-pour, that means
         * levers around the attached Faucet should count as part of the same
         * foundry control area.
         */
        for (
                BlockPos faucetPos : FoundryTankNetwork.findAttachedFaucets(
                level,
                network.getTankPositions()
        )
        ) {
            addAnchorAndNeighbors(
                    anchors,
                    faucetPos
            );
        }

        return anchors;
    }

    private static void addAnchorAndNeighbors(
            Set<BlockPos> anchors,
            BlockPos center
    ) {
        anchors.add(
                center.immutable()
        );

        for (Direction direction : Direction.values()) {
            anchors.add(
                    center.relative(
                            direction
                    ).immutable()
            );
        }
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
