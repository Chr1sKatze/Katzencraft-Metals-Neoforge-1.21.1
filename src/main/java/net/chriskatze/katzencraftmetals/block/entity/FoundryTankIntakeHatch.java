package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.block.custom.FoundryTankBlock;
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

/** Handles open intake hatches stored directly in Tank blockstate. */
final class FoundryTankIntakeHatch {

    private static final int INTAKE_TRANSFER_INTERVAL_TICKS = 8;

    private static final Comparator<BlockPos> POSITION_ORDER =
            Comparator
                    .comparingInt((BlockPos pos) -> pos.getY())
                    .thenComparingInt(BlockPos::getX)
                    .thenComparingInt(BlockPos::getZ);

    private FoundryTankIntakeHatch() {
    }

    static void processOpenHatches(
            FoundryControllerBlockEntity controller
    ) {
        Level level = controller.getLevel();

        if (
                level == null
                        || level.isClientSide()
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

        long gameTime = level.getGameTime();

        /* Avoid allocating/sorting the Tank list on the seven idle ticks. */
        if (gameTime % INTAKE_TRANSFER_INTERVAL_TICKS != 0L) {
            return;
        }

        List<BlockPos> positions =
                new ArrayList<>(network.getTankPositions());

        positions.sort(POSITION_ORDER);

        for (BlockPos tankPos : positions) {
            BlockState tankState = level.getBlockState(tankPos);

            if (
                    !(tankState.getBlock() instanceof FoundryTankBlock)
                            || !tankState.hasProperty(
                            FoundryTankBlock.HATCH_OPEN
                    )
                            || !tankState.getValue(
                            FoundryTankBlock.HATCH_OPEN
                    )
            ) {
                continue;
            }

            if (
                    level.getBlockState(tankPos.above())
                            .getBlock()
                            instanceof FoundryTankBlock
            ) {
                /*
                 * FoundryTankBlock.updateShape already closes HATCH_OPEN when
                 * a Tank is placed above. The Controller tick must never
                 * rewrite Tank BlockStates as a second source of truth.
                 */
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
        if (
                pullConnectedHopperIntoController(
                        level,
                        hatchPos,
                        controller
                )
        ) {
            return;
        }

        pullLooseItemIntoController(
                level,
                hatchPos,
                controller
        );
    }

    private static boolean pullLooseItemIntoController(
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
            ItemStack stack = itemEntity.getItem();
            ItemStack visualStack = stack.copy();

            int inserted =
                    insertOneItemIntoControllerInputs(
                            controller,
                            stack
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

            stack.shrink(inserted);

            if (stack.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(stack);
            }

            return true;
        }

        return false;
    }

    private static boolean pullConnectedHopperIntoController(
            Level level,
            BlockPos hatchPos,
            FoundryControllerBlockEntity controller
    ) {
        BlockPos hopperPos = hatchPos.above();
        BlockState hopperState = level.getBlockState(hopperPos);

        if (!isDownwardEnabledHopper(hopperState)) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(hopperPos);

        if (!(blockEntity instanceof Container hopperInventory)) {
            return false;
        }

        for (
                int slot = 0;
                slot < hopperInventory.getContainerSize();
                slot++
        ) {
            ItemStack hopperStack = hopperInventory.getItem(slot);

            if (hopperStack.isEmpty()) {
                continue;
            }

            ItemStack visualStack = hopperStack.copy();
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

            hopperStack.shrink(inserted);
            hopperInventory.setItem(slot, hopperStack);
            hopperInventory.setChanged();
            return true;
        }

        return false;
    }

    private static boolean isDownwardEnabledHopper(
            BlockState state
    ) {
        return state.getBlock() instanceof HopperBlock
                && state.hasProperty(HopperBlock.FACING)
                && state.getValue(HopperBlock.FACING) == Direction.DOWN
                && (
                !state.hasProperty(HopperBlock.ENABLED)
                        || state.getValue(HopperBlock.ENABLED)
        );
    }

    private static int insertOneItemIntoControllerInputs(
            FoundryControllerBlockEntity controller,
            ItemStack sourceStack
    ) {
        if (sourceStack.isEmpty()) {
            return 0;
        }

        ItemStack one = sourceStack.copy();
        one.setCount(1);
        return insertIntoControllerInputs(controller, one);
    }

    private static int insertIntoControllerInputs(
            FoundryControllerBlockEntity controller,
            ItemStack sourceStack
    ) {
        if (
                sourceStack.isEmpty()
                        || !controller.canMelt(sourceStack)
        ) {
            return 0;
        }

        SimpleContainer inventory = controller.getInputInventory();
        ItemStack remaining = sourceStack.copy();
        int originalCount = remaining.getCount();

        for (
                int slot = 0;
                slot < inventory.getContainerSize()
                        && !remaining.isEmpty();
                slot++
        ) {
            if (!inventory.canPlaceItem(slot, remaining)) {
                continue;
            }

            ItemStack slotStack = inventory.getItem(slot);

            if (slotStack.isEmpty()) {
                int moved =
                        Math.min(
                                remaining.getCount(),
                                remaining.getMaxStackSize()
                        );

                ItemStack inserted = remaining.copy();
                inserted.setCount(moved);
                inventory.setItem(slot, inserted);
                remaining.shrink(moved);
                continue;
            }

            if (!ItemStack.isSameItemSameComponents(slotStack, remaining)) {
                continue;
            }

            int space =
                    slotStack.getMaxStackSize()
                            - slotStack.getCount();

            if (space <= 0) {
                continue;
            }

            int moved = Math.min(space, remaining.getCount());
            slotStack.grow(moved);
            remaining.shrink(moved);
            inventory.setChanged();
        }

        return originalCount - remaining.getCount();
    }
}
