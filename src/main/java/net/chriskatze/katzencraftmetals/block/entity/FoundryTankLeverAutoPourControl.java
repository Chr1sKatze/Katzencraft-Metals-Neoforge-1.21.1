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
 * Only powered vanilla levers count. Random powered blocks, dust, buttons,
 * redstone torches, repeaters, comparators, and pressure plates do not count.
 *
 * A lever counts if it is attached to:
 * - one of the connected tank blocks
 * - or a block directly adjacent to one of the connected tank blocks
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
                network == null
                        || !network.isActive()
        ) {
            return false;
        }

        Set<BlockPos> candidateAnchors =
                collectLeverAnchorCandidates(
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
            FoundryTankNetwork network
    ) {
        Set<BlockPos> anchors =
                new HashSet<>();

        for (BlockPos tankPos : network.getTankPositions()) {
            anchors.add(
                    tankPos.immutable()
            );

            for (Direction direction : Direction.values()) {
                anchors.add(
                        tankPos.relative(
                                direction
                        ).immutable()
                );
            }
        }

        return anchors;
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
