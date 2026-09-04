package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.block.custom.FoundryControllerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Controller-facing Tank structure operations.
 *
 * Ownership no longer lives on individual Tank BlockEntities. The Controller's
 * event-driven FoundryControllerTankStructure cache is authoritative.
 */
final class FoundryControllerNetwork {

    private FoundryControllerNetwork() {
    }

    static Direction getFacing(
            FoundryControllerBlockEntity controller
    ) {
        return controller.getBlockState().getValue(
                FoundryControllerBlock.FACING
        );
    }

    static boolean isValidTankAttachmentPosition(
            FoundryControllerBlockEntity controller,
            BlockPos tankPos
    ) {
        if (
                tankPos == null
                        || tankPos.getY()
                        != controller.getBlockPos().getY()
        ) {
            return false;
        }

        int deltaX =
                tankPos.getX()
                        - controller.getBlockPos().getX();

        int deltaZ =
                tankPos.getZ()
                        - controller.getBlockPos().getZ();

        if (Math.abs(deltaX) + Math.abs(deltaZ) != 1) {
            return false;
        }

        Direction facing = getFacing(controller);

        int forwardDistance =
                deltaX * facing.getStepX()
                        + deltaZ * facing.getStepZ();

        return forwardDistance <= 0;
    }

    static boolean canOwnTankLayout(
            FoundryControllerBlockEntity controller,
            Set<BlockPos> tankPositions
    ) {
        if (
                tankPositions == null
                        || tankPositions.isEmpty()
        ) {
            return false;
        }

        boolean touchesController = false;
        Direction facing = getFacing(controller);
        BlockPos directlyInFront =
                controller.getBlockPos()
                        .relative(facing);

        for (BlockPos tankPos : tankPositions) {
            /* Keep the Controller's direct front face clear. */
            if (tankPos.equals(directlyInFront)) {
                return false;
            }

            if (isValidTankAttachmentPosition(controller, tankPos)) {
                touchesController = true;
            }
        }

        return touchesController;
    }

    static List<BlockPos> getValidTankAttachmentPositions(
            FoundryControllerBlockEntity controller
    ) {
        Direction facing = getFacing(controller);

        return List.of(
                controller.getBlockPos()
                        .relative(facing.getOpposite()),
                controller.getBlockPos()
                        .relative(facing.getClockWise()),
                controller.getBlockPos()
                        .relative(facing.getCounterClockWise())
        );
    }

    static FoundryTankNetwork getOwnedTankNetwork(
            FoundryControllerBlockEntity controller
    ) {
        return controller.getTankStructure()
                .getNetwork();
    }

    static boolean ensureTankNetwork(
            FoundryControllerBlockEntity controller
    ) {
        return getOwnedTankNetwork(controller) != null;
    }

    static void releaseFoundry(
            FoundryControllerBlockEntity controller
    ) {
        Set<BlockPos> releasedPositions =
                new HashSet<>(
                        controller.getTankStructure()
                                .getTankPositions()
                );

        /*
         * Tanks are only physical vessels. Breaking the Controller removes the
         * storage owner, so molten contents cannot remain hidden in the blocks.
         */
        controller.getTankStorage()
                .clearForControllerRemoval();

        controller.getTankStructure()
                .clearForControllerRemoval();

        /*
         * A neighboring Controller may have been deliberately blocked from
         * claiming these Tank positions while this Controller existed. Give it
         * one event-driven rebuild now that the reservation is gone.
         */
        if (controller.getLevel() != null) {
            for (BlockPos releasedPos : releasedPositions) {
                FoundryControllerTankStructure.markNearbyControllersDirty(
                        controller.getLevel(),
                        releasedPos
                );
            }
        }

        /*
         * Do not send a BlockEntity update for a Controller that is in the
         * middle of being removed. Its BlockEntity still carries the old
         * Controller BlockState at this point; sending that state back to the
         * client can create a ghost "respawned" Controller. Vanilla's block
         * removal packet is the only state update needed here.
         */
    }
}
