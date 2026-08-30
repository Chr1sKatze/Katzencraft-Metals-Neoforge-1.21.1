package net.chriskatze.katzencraftmetals.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Handles open top-tank intake hatches.
 *
 * The hatch is process-item-only:
 * - meltable items landing on top are pulled into the Controller input slots
 * - a downward-facing Hopper above the hatch feeds one meltable item at normal
 *   Hopper speed
 * - every successful pull spawns a short-lived fake visual item falling into
 *   the Tank interior
 * - fuel and random junk are ignored
 * - no items are ever extracted through the hatch
 */
final class FoundryTankIntakeHatch {

    private static final int SUCTION_INTERVAL_TICKS =
            5;

    private static final int HOPPER_TRANSFER_INTERVAL_TICKS =
            8;

    private static final Comparator<BlockPos> POSITION_ORDER =
            Comparator
                    .comparingInt((BlockPos pos) -> pos.getY())
                    .thenComparingInt(pos -> pos.getX())
                    .thenComparingInt(pos -> pos.getZ());

    private FoundryTankIntakeHatch() {
    }

    static void processOpenHatches(
            FoundryControllerBlockEntity controller
    ) {
        Level level =
                controller.getLevel();

        if (
                level == null
                        || level.isClientSide()
        ) {
            return;
        }

        long gameTime =
                level.getGameTime();

        FoundryTankNetwork network =
                controller.getOwnedTankNetwork();

        if (
                network == null
                        || !network.isActive()
        ) {
            return;
        }

        List<BlockPos> tankPositions =
                new ArrayList<>(
                        network.getTankPositions()
                );

        tankPositions.sort(
                POSITION_ORDER
        );

        for (BlockPos tankPos : tankPositions) {
            BlockEntity blockEntity =
                    level.getBlockEntity(
                            tankPos
                    );

            if (!(blockEntity instanceof FoundryTankBlockEntity tank)) {
                continue;
            }

            if (!tank.isIntakeHatchOpen()) {
                continue;
            }

            /*
             * If the player later places another Tank above this one, the hatch
             * can no longer be a top intake. Close it automatically so the
             * visual and behavior stay honest.
             */
            if (!tank.isTopTank()) {
                tank.setIntakeHatchOpen(false);
                continue;
            }

            pullItemsIntoController(
                    level,
                    tankPos,
                    controller,
                    gameTime
            );
        }
    }

    private static void pullItemsIntoController(
            Level level,
            BlockPos hatchPos,
            FoundryControllerBlockEntity controller,
            long gameTime
    ) {
        if (
                gameTime
                        % HOPPER_TRANSFER_INTERVAL_TICKS
                        == 0L
        ) {
            pullConnectedHopperIntoController(
                    level,
                    hatchPos,
                    controller
            );
        }

        if (
                gameTime
                        % SUCTION_INTERVAL_TICKS
                        != 0L
        ) {
            return;
        }

        pullLooseItemsIntoController(
                level,
                hatchPos,
                controller
        );
    }

    private static void pullLooseItemsIntoController(
            Level level,
            BlockPos hatchPos,
            FoundryControllerBlockEntity controller
    ) {
        AABB suctionBox =
                new AABB(
                        hatchPos.getX() + 0.125,
                        hatchPos.getY() + 1.0,
                        hatchPos.getZ() + 0.125,
                        hatchPos.getX() + 0.875,
                        hatchPos.getY() + 1.45,
                        hatchPos.getZ() + 0.875
                );

        List<ItemEntity> itemEntities =
                level.getEntitiesOfClass(
                        ItemEntity.class,
                        suctionBox,
                        itemEntity ->
                                itemEntity.isAlive()
                                        && !itemEntity.getItem().isEmpty()
                );

        for (ItemEntity itemEntity : itemEntities) {
            ItemStack itemStack =
                    itemEntity.getItem();

            ItemStack visualStack =
                    itemStack.copy();

            int inserted =
                    insertIntoControllerInputs(
                            controller,
                            itemStack
                    );

            if (inserted <= 0) {
                continue;
            }

            FoundryIntakeItemVisualEvents.spawnVisuals(
                    level,
                    hatchPos,
                    controller,
                    visualStack,
                    inserted
            );

            itemStack.shrink(
                    inserted
            );

            if (itemStack.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(
                        itemStack
                );
            }
        }
    }

    private static void pullConnectedHopperIntoController(
            Level level,
            BlockPos hatchPos,
            FoundryControllerBlockEntity controller
    ) {
        BlockPos hopperPos =
                hatchPos.above();

        BlockState hopperState =
                level.getBlockState(
                        hopperPos
                );

        if (!isDownwardEnabledHopper(hopperState)) {
            return;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(
                        hopperPos
                );

        if (!(blockEntity instanceof Container hopperInventory)) {
            return;
        }

        for (
                int slot = 0;
                slot < hopperInventory.getContainerSize();
                slot++
        ) {
            ItemStack hopperStack =
                    hopperInventory.getItem(
                            slot
                    );

            if (hopperStack.isEmpty()) {
                continue;
            }

            ItemStack visualStack =
                    hopperStack.copy();

            int inserted =
                    insertOneItemIntoControllerInputs(
                            controller,
                            hopperStack
                    );

            if (inserted <= 0) {
                continue;
            }

            FoundryIntakeItemVisualEvents.spawnVisuals(
                    level,
                    hatchPos,
                    controller,
                    visualStack,
                    inserted
            );

            hopperStack.shrink(
                    inserted
            );

            hopperInventory.setItem(
                    slot,
                    hopperStack
            );

            hopperInventory.setChanged();

            /*
             * Match normal Hopper behavior: one item per transfer cycle.
             */
            return;
        }
    }

    private static boolean isDownwardEnabledHopper(
            BlockState state
    ) {
        return state.getBlock() instanceof HopperBlock
                && state.hasProperty(
                HopperBlock.FACING
        )
                && state.getValue(
                HopperBlock.FACING
        ) == Direction.DOWN
                && (
                !state.hasProperty(
                        HopperBlock.ENABLED
                )
                        || state.getValue(
                        HopperBlock.ENABLED
                )
        );
    }

    private static int insertOneItemIntoControllerInputs(
            FoundryControllerBlockEntity controller,
            ItemStack sourceStack
    ) {
        if (sourceStack.isEmpty()) {
            return 0;
        }

        ItemStack singleItem =
                sourceStack.copy();

        singleItem.setCount(
                1
        );

        return insertIntoControllerInputs(
                controller,
                singleItem
        );
    }

    private static int insertIntoControllerInputs(
            FoundryControllerBlockEntity controller,
            ItemStack sourceStack
    ) {
        if (
                sourceStack.isEmpty()
                        || !controller.canMelt(
                        sourceStack
                )
        ) {
            return 0;
        }

        SimpleContainer inputInventory =
                controller.getInputInventory();

        ItemStack remaining =
                sourceStack.copy();

        int originalCount =
                remaining.getCount();

        for (
                int slot = 0;
                slot < inputInventory.getContainerSize()
                        && !remaining.isEmpty();
                slot++
        ) {
            if (!inputInventory.canPlaceItem(
                    slot,
                    remaining
            )) {
                continue;
            }

            ItemStack slotStack =
                    inputInventory.getItem(
                            slot
                    );

            if (slotStack.isEmpty()) {
                int moved =
                        Math.min(
                                remaining.getCount(),
                                remaining.getMaxStackSize()
                        );

                ItemStack inserted =
                        remaining.copy();

                inserted.setCount(
                        moved
                );

                inputInventory.setItem(
                        slot,
                        inserted
                );

                remaining.shrink(
                        moved
                );

                continue;
            }

            if (!ItemStack.isSameItemSameComponents(
                    slotStack,
                    remaining
            )) {
                continue;
            }

            int space =
                    slotStack.getMaxStackSize()
                            - slotStack.getCount();

            if (space <= 0) {
                continue;
            }

            int moved =
                    Math.min(
                            space,
                            remaining.getCount()
                    );

            slotStack.grow(
                    moved
            );

            remaining.shrink(
                    moved
            );

            inputInventory.setChanged();
        }

        return originalCount
                - remaining.getCount();
    }
}
