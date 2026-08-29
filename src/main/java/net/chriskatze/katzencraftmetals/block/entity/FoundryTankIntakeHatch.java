package net.chriskatze.katzencraftmetals.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Handles open top-tank intake hatches.
 *
 * The hatch is process-item-only:
 * - meltable items landing on top are pulled into the Controller input slots
 * - fuel and random junk are ignored
 * - no items are ever extracted through the hatch
 */
final class FoundryTankIntakeHatch {

    private static final int SUCTION_INTERVAL_TICKS = 5;

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
                        || level.getGameTime()
                        % SUCTION_INTERVAL_TICKS != 0L
        ) {
            return;
        }

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
                    controller
            );
        }
    }

    private static void pullItemsIntoController(
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

            int inserted =
                    insertIntoControllerInputs(
                            controller,
                            itemStack
                    );

            if (inserted <= 0) {
                continue;
            }

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
