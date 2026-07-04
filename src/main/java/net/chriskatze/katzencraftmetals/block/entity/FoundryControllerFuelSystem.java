package net.chriskatze.katzencraftmetals.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * Fuel inventory and burn-state owned directly by one Foundry Controller.
 *
 * Keeping this in a dedicated class prevents FoundryControllerBlockEntity from
 * becoming another oversized class while the Foundry gains temperature and
 * additional fuel rules later.
 */
final class FoundryControllerFuelSystem {

    static final int FUEL_SLOT_COUNT = 4;
    static final int COAL_BURN_TIME = 1600;
    static final int COAL_MAXIMUM_TEMPERATURE = 900;

    private final FoundryControllerBlockEntity controller;

    private final SimpleContainer inventory =
            new SimpleContainer(FUEL_SLOT_COUNT) {

                @Override
                public boolean canPlaceItem(
                        int slot,
                        ItemStack stack
                ) {
                    return controller.isFuelSlotUnlocked(slot)
                            && stack.is(Items.COAL);
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    controller.setChanged();
                }
            };

    private int burnTimeRemaining;
    private int maxBurnTime;
    private int activeFuelMaximumTemperature;

    FoundryControllerFuelSystem(
            FoundryControllerBlockEntity controller
    ) {
        this.controller = controller;
    }

    boolean supplyBurnTick() {
        if (
                controller.getLevel() == null
                        || controller.getLevel().isClientSide()
        ) {
            return false;
        }

        if (
                burnTimeRemaining <= 0
                        && !consumeCoal()
        ) {
            return false;
        }

        burnTimeRemaining--;
        controller.setChanged();

        return true;
    }

    private boolean consumeCoal() {
        for (
                int slot = 0;
                slot < controller.getUnlockedFuelSlotCount();
                slot++
        ) {
            ItemStack stack =
                    inventory.getItem(slot);

            if (!stack.is(Items.COAL)) {
                continue;
            }

            inventory.removeItem(
                    slot,
                    1
            );

            burnTimeRemaining =
                    COAL_BURN_TIME;

            maxBurnTime =
                    COAL_BURN_TIME;

            activeFuelMaximumTemperature =
                    COAL_MAXIMUM_TEMPERATURE;

            controller.setChanged();
            return true;
        }

        return false;
    }

    boolean hasAvailableFuel() {
        if (burnTimeRemaining > 0) {
            return true;
        }

        for (
                int slot = 0;
                slot < controller.getUnlockedFuelSlotCount();
                slot++
        ) {
            if (
                    inventory.getItem(slot)
                            .is(Items.COAL)
            ) {
                return true;
            }
        }

        return false;
    }

    boolean isBurning() {
        return burnTimeRemaining > 0;
    }

    int getBurnTimeRemaining() {
        return burnTimeRemaining;
    }

    int getMaxBurnTime() {
        return maxBurnTime;
    }


    int getActiveFuelMaximumTemperature() {
        return burnTimeRemaining > 0
                ? Math.max(
                FoundryControllerTemperature.AMBIENT_TEMPERATURE,
                activeFuelMaximumTemperature
        )
                : COAL_MAXIMUM_TEMPERATURE;
    }

    boolean canReachTemperature(int requiredTemperature) {
        return requiredTemperature <= Math.min(
                controller.getTemperatureSystem()
                        .getTierMaximumTemperature(),
                getActiveFuelMaximumTemperature()
        );
    }

    int getStoredCoalCount() {
        int coalCount =
                0;

        for (int slot = 0; slot < FUEL_SLOT_COUNT; slot++) {
            ItemStack stack =
                    inventory.getItem(slot);

            if (stack.is(Items.COAL)) {
                coalCount +=
                        stack.getCount();
            }
        }

        return coalCount;
    }

    int getHighestOccupiedSlot() {
        for (int slot = FUEL_SLOT_COUNT - 1; slot >= 0; slot--) {
            if (!inventory.getItem(slot).isEmpty()) {
                return slot;
            }
        }

        return -1;
    }

    SimpleContainer getInventory() {
        return inventory;
    }

    /**
     * One-checkpoint migration bridge for old separate Fuel Chamber blocks.
     *
     * Coal and the currently active burn cycle are moved into the Controller.
     * Any coal that does not fit remains safely inside the old chamber.
     */
    void migrateNearbyLegacyFuelChambers(
            FoundryTankNetwork network
    ) {
        if (
                controller.getLevel() == null
                        || controller.getLevel().isClientSide()
                        || network == null
        ) {
            return;
        }

        Set<BlockPos> candidatePositions =
                new HashSet<>();

        for (Direction direction : Direction.values()) {
            candidatePositions.add(
                    controller.getBlockPos()
                            .relative(direction)
            );
        }

        for (BlockPos tankPos : network.getTankPositions()) {
            for (Direction direction : Direction.values()) {
                candidatePositions.add(
                        tankPos.relative(direction)
                );
            }
        }

        candidatePositions.stream()
                .sorted(
                        Comparator
                                .comparingLong(
                                        (BlockPos position) ->
                                                distanceSquared(
                                                        controller.getBlockPos(),
                                                        position
                                                )
                                )
                                .thenComparingInt(
                                        (BlockPos position) ->
                                                position.getY()
                                )
                                .thenComparingInt(
                                        (BlockPos position) ->
                                                position.getX()
                                )
                                .thenComparingInt(
                                        (BlockPos position) ->
                                                position.getZ()
                                )
                )
                .forEach(
                        this::migrateLegacyFuelChamberAt
                );
    }

    private void migrateLegacyFuelChamberAt(
            BlockPos position
    ) {
        BlockEntity blockEntity =
                controller.getLevel()
                        .getBlockEntity(position);

        if (
                !(blockEntity
                        instanceof FuelChamberBlockEntity fuelChamber)
        ) {
            return;
        }

        fuelChamber.tryAutoAssign(null);

        if (
                !controller.getControllerId()
                        .equals(
                                fuelChamber.getControllerId()
                        )
        ) {
            return;
        }

        int legacyMaxBurnTime =
                fuelChamber.getMaxBurnTime();

        int legacyBurnTime =
                fuelChamber.takeBurnTimeForControllerMigration();

        if (legacyBurnTime > 0) {
            long combinedRemaining =
                    (long) burnTimeRemaining
                            + legacyBurnTime;

            long combinedMaximum =
                    (long) maxBurnTime
                            + Math.max(
                                    legacyBurnTime,
                                    legacyMaxBurnTime
                            );

            burnTimeRemaining =
                    (int) Math.min(
                            Integer.MAX_VALUE,
                            combinedRemaining
                    );

            maxBurnTime =
                    (int) Math.min(
                            Integer.MAX_VALUE,
                            Math.max(
                                    combinedRemaining,
                                    combinedMaximum
                            )
                    );

            controller.setChanged();
        }

        for (
                int sourceSlot = 0;
                sourceSlot < FuelChamberBlockEntity.SLOT_COUNT;
                sourceSlot++
        ) {
            moveCoalFromLegacySlot(
                    fuelChamber,
                    sourceSlot
            );
        }
    }

    private void moveCoalFromLegacySlot(
            FuelChamberBlockEntity fuelChamber,
            int sourceSlot
    ) {
        ItemStack sourceStack =
                fuelChamber.getFuelInventory()
                        .getItem(sourceSlot);

        if (
                sourceStack.isEmpty()
                        || !sourceStack.is(Items.COAL)
        ) {
            return;
        }

        for (
                int targetSlot = 0;
                targetSlot < controller.getUnlockedFuelSlotCount()
                        && !sourceStack.isEmpty();
                targetSlot++
        ) {
            ItemStack targetStack =
                    inventory.getItem(targetSlot);

            if (targetStack.isEmpty()) {
                int moved =
                        Math.min(
                                sourceStack.getCount(),
                                sourceStack.getMaxStackSize()
                        );

                ItemStack inserted =
                        sourceStack.copy();

                inserted.setCount(moved);

                inventory.setItem(
                        targetSlot,
                        inserted
                );

                sourceStack.shrink(moved);
                continue;
            }

            if (!targetStack.is(Items.COAL)) {
                continue;
            }

            int freeSpace =
                    targetStack.getMaxStackSize()
                            - targetStack.getCount();

            if (freeSpace <= 0) {
                continue;
            }

            int moved =
                    Math.min(
                            sourceStack.getCount(),
                            freeSpace
                    );

            targetStack.grow(moved);
            sourceStack.shrink(moved);

            inventory.setChanged();
        }

        fuelChamber.getFuelInventory()
                .setItem(
                        sourceSlot,
                        sourceStack
                );
    }

    private static long distanceSquared(
            BlockPos first,
            BlockPos second
    ) {
        long deltaX =
                first.getX()
                        - second.getX();

        long deltaY =
                first.getY()
                        - second.getY();

        long deltaZ =
                first.getZ()
                        - second.getZ();

        return deltaX * deltaX
                + deltaY * deltaY
                + deltaZ * deltaZ;
    }

    void save(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        tag.put(
                "FuelInventory",
                inventory.createTag(
                        registries
                )
        );

        tag.putInt(
                "BurnTimeRemaining",
                burnTimeRemaining
        );

        tag.putInt(
                "MaxBurnTime",
                maxBurnTime
        );

        tag.putInt(
                "ActiveFuelMaximumTemperature",
                activeFuelMaximumTemperature
        );
    }

    void load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        inventory.removeAllItems();

        if (
                tag.contains(
                        "FuelInventory",
                        Tag.TAG_LIST
                )
        ) {
            inventory.fromTag(
                    tag.getList(
                            "FuelInventory",
                            Tag.TAG_COMPOUND
                    ),
                    registries
            );
        }

        burnTimeRemaining =
                Math.max(
                        0,
                        tag.getInt(
                                "BurnTimeRemaining"
                        )
                );

        maxBurnTime =
                Math.max(
                        burnTimeRemaining,
                        tag.getInt(
                                "MaxBurnTime"
                        )
                );

        activeFuelMaximumTemperature =
                burnTimeRemaining > 0
                        ? Math.max(
                        FoundryControllerTemperature.AMBIENT_TEMPERATURE,
                        tag.contains("ActiveFuelMaximumTemperature")
                                ? tag.getInt("ActiveFuelMaximumTemperature")
                                : COAL_MAXIMUM_TEMPERATURE
                )
                        : 0;
    }
}
