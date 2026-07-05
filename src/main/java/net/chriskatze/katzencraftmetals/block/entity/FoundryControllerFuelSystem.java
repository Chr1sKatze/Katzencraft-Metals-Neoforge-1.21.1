package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.fuel.FoundryFuelDefinition;
import net.chriskatze.katzencraftmetals.fuel.FoundryFuels;
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
import java.util.Optional;
import java.util.Set;

/**
 * Fuel inventory and active burn cycle owned by one Foundry Controller.
 */
final class FoundryControllerFuelSystem {

    static final int FUEL_SLOT_COUNT = 4;

    /*
     * Kept for compatibility with older callers and saves that still use the
     * old Coal-specific names.
     */
    static final int COAL_BURN_TIME =
            FoundryFuels.COAL.burnTime();

    static final int COAL_MAXIMUM_TEMPERATURE =
            FoundryFuels.COAL.maximumTemperature();

    private final FoundryControllerBlockEntity controller;

    private final SimpleContainer inventory =
            new SimpleContainer(FUEL_SLOT_COUNT) {

                @Override
                public boolean canPlaceItem(
                        int slot,
                        ItemStack stack
                ) {
                    return controller.isFuelSlotUnlocked(slot)
                            && FoundryFuels.isFuel(stack);
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

    /**
     * Supplies one active burn tick.
     *
     * A running low-temperature fuel is never silently deleted. If a hotter
     * job is queued while a weaker fuel cycle is still active, the old cycle
     * finishes first. The next cycle then selects the weakest stored fuel that
     * can reach the requested temperature.
     */
    boolean supplyBurnTick(
            int requiredTemperature
    ) {
        if (
                controller.getLevel() == null
                        || controller.getLevel().isClientSide()
        ) {
            return false;
        }

        if (
                burnTimeRemaining <= 0
                        && !consumeBestFuel(
                        requiredTemperature
                )
        ) {
            return false;
        }

        burnTimeRemaining--;

        if (burnTimeRemaining <= 0) {
            burnTimeRemaining = 0;
        }

        controller.setChanged();
        return true;
    }

    private boolean consumeBestFuel(
            int requiredTemperature
    ) {
        int bestSlot =
                -1;

        FoundryFuelDefinition bestDefinition =
                null;

        for (
                int slot = 0;
                slot < controller.getUnlockedFuelSlotCount();
                slot++
        ) {
            ItemStack stack =
                    inventory.getItem(slot);

            Optional<FoundryFuelDefinition> definitionOptional =
                    FoundryFuels.find(stack);

            if (definitionOptional.isEmpty()) {
                continue;
            }

            FoundryFuelDefinition definition =
                    definitionOptional.get();

            if (
                    definition.maximumTemperature()
                            < requiredTemperature
            ) {
                continue;
            }

            if (
                    bestDefinition == null
                            || definition.maximumTemperature()
                            < bestDefinition.maximumTemperature()
                            || (
                            definition.maximumTemperature()
                                    == bestDefinition.maximumTemperature()
                                    && slot < bestSlot
                    )
            ) {
                bestSlot =
                        slot;

                bestDefinition =
                        definition;
            }
        }

        if (
                bestSlot < 0
                        || bestDefinition == null
        ) {
            return false;
        }

        inventory.removeItem(
                bestSlot,
                1
        );

        burnTimeRemaining =
                bestDefinition.burnTime();

        maxBurnTime =
                bestDefinition.burnTime();

        activeFuelMaximumTemperature =
                bestDefinition.maximumTemperature();

        controller.setChanged();
        return true;
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
                    FoundryFuels.isFuel(
                            inventory.getItem(slot)
                    )
            ) {
                return true;
            }
        }

        return false;
    }

    boolean canReachTemperature(
            int requiredTemperature
    ) {
        if (
                requiredTemperature
                        > controller.getTemperatureSystem()
                        .getTierMaximumTemperature()
        ) {
            return false;
        }

        if (
                burnTimeRemaining > 0
                        && activeFuelMaximumTemperature
                        >= requiredTemperature
        ) {
            return true;
        }

        for (
                int slot = 0;
                slot < controller.getUnlockedFuelSlotCount();
                slot++
        ) {
            Optional<FoundryFuelDefinition> definition =
                    FoundryFuels.find(
                            inventory.getItem(slot)
                    );

            if (
                    definition.isPresent()
                            && definition.get()
                            .maximumTemperature()
                            >= requiredTemperature
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
        return Math.max(
                FoundryControllerTemperature.AMBIENT_TEMPERATURE,
                activeFuelMaximumTemperature
        );
    }

    int getStoredFuelCount() {
        int count =
                0;

        for (int slot = 0; slot < FUEL_SLOT_COUNT; slot++) {
            ItemStack stack =
                    inventory.getItem(slot);

            if (FoundryFuels.isFuel(stack)) {
                count +=
                        stack.getCount();
            }
        }

        return count;
    }

    /** Compatibility name retained for existing callers. */
    int getStoredCoalCount() {
        return getStoredFuelCount();
    }

    int getHighestOccupiedSlot() {
        for (
                int slot = FUEL_SLOT_COUNT - 1;
                slot >= 0;
                slot--
        ) {
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
     * Legacy chambers only contained Coal, so this migration remains
     * intentionally Coal-specific.
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

            activeFuelMaximumTemperature =
                    Math.max(
                            activeFuelMaximumTemperature,
                            COAL_MAXIMUM_TEMPERATURE
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
                        tag.contains(
                                "ActiveFuelMaximumTemperature"
                        )
                                ? tag.getInt(
                                "ActiveFuelMaximumTemperature"
                        )
                                : COAL_MAXIMUM_TEMPERATURE
                )
                        : 0;
    }
}
