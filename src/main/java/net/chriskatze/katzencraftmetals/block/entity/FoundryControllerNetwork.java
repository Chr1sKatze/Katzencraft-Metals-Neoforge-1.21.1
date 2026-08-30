package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.block.custom.FoundryControllerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tank-layout and ownership operations for one Foundry Controller.
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
                tankPos.getY()
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

        Direction facing =
                getFacing(controller);

        int forwardDistance =
                deltaX * facing.getStepX()
                        + deltaZ * facing.getStepZ();

        return forwardDistance <= 0;
    }

    static boolean canOwnTankLayout(
            FoundryControllerBlockEntity controller,
            Set<BlockPos> tankPositions
    ) {
        if (tankPositions.isEmpty()) {
            return false;
        }

        boolean touchesController =
                false;

        Direction facing =
                getFacing(controller);

        BlockPos directlyInFront =
                controller.getBlockPos()
                        .relative(
                                facing
                        );

        for (BlockPos tankPos : tankPositions) {
            /*
             * The controller front face itself must stay clear.
             *
             * Tanks farther in front are allowed so C-shaped or ring-like
             * footprints can wrap around the controller, but the directly
             * blocked front face still stays invalid.
             */
            if (tankPos.equals(directlyInFront)) {
                return false;
            }

            if (
                    isValidTankAttachmentPosition(
                            controller,
                            tankPos
                    )
            ) {
                touchesController =
                        true;
            }
        }

        return touchesController;
    }

    static List<BlockPos> getValidTankAttachmentPositions(
            FoundryControllerBlockEntity controller
    ) {
        Direction facing =
                getFacing(controller);

        return List.of(
                controller.getBlockPos()
                        .relative(
                                facing.getOpposite()
                        ),
                controller.getBlockPos()
                        .relative(
                                facing.getClockWise()
                        ),
                controller.getBlockPos()
                        .relative(
                                facing.getCounterClockWise()
                        )
        );
    }

    @Nullable
    static FoundryTankNetwork getOwnedTankNetwork(
            FoundryControllerBlockEntity controller
    ) {
        if (controller.getLevel() == null) {
            return null;
        }

        FoundryTankNetwork best =
                null;

        Set<BlockPos> alreadyChecked =
                new HashSet<>();

        for (
                BlockPos attachmentPos :
                getValidTankAttachmentPositions(
                        controller
                )
        ) {
            BlockEntity blockEntity =
                    controller.getLevel()
                            .getBlockEntity(
                                    attachmentPos
                            );

            if (
                    !(blockEntity
                            instanceof FoundryTankBlockEntity tank)
            ) {
                continue;
            }

            FoundryTankNetwork network =
                    tank.getNetwork();

            if (
                    network == null
                            || !controller.getControllerId()
                            .equals(
                                    network.getOwnerId()
                            )
                            || !canOwnTankLayout(
                            controller,
                            network.getTankPositions()
                    )
            ) {
                continue;
            }

            BlockPos networkKey =
                    network.getTankPositions()
                            .stream()
                            .min(
                                    Comparator
                                            .comparingInt(
                                                    (BlockPos pos) ->
                                                            pos.getY()
                                            )
                                            .thenComparingInt(
                                                    BlockPos::getX
                                            )
                                            .thenComparingInt(
                                                    BlockPos::getZ
                                            )
                            )
                            .orElse(
                                    attachmentPos
                            );

            if (!alreadyChecked.add(networkKey)) {
                continue;
            }

            if (
                    best == null
                            || network.getTankCount()
                            > best.getTankCount()
            ) {
                best =
                        network;
            }
        }

        return best;
    }

    static boolean ensureTankNetwork(
            FoundryControllerBlockEntity controller
    ) {
        if (
                controller.getLevel() == null
                        || controller.getLevel().isClientSide()
        ) {
            return false;
        }

        if (
                getOwnedTankNetwork(
                        controller
                ) != null
        ) {
            return true;
        }

        return FoundryTankNetwork
                .claimLargestUnassignedLayoutForController(
                        controller.getLevel(),
                        controller
                )
                != null;
    }

    static void releaseFoundry(
            FoundryControllerBlockEntity controller
    ) {
        FoundryTankNetwork network =
                getOwnedTankNetwork(
                        controller
                );

        if (network != null) {
            network.releaseOwnership();
        }
    }
}
