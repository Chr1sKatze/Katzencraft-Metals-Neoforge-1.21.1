package net.chriskatze.katzencraftmetals.menu;

import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.block.entity.FoundryControllerBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankNetwork;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.chriskatze.katzencraftmetals.recipe.ModRecipes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import java.util.Optional;

public class FoundryControllerMenu
        extends AbstractContainerMenu {

    private static final int INPUT_SLOT_COUNT =
            FoundryControllerBlockEntity.INPUT_SLOT_COUNT;

    private static final int FUEL_SLOT_COUNT =
            FoundryControllerBlockEntity.FUEL_SLOT_COUNT;

    private static final int MACHINE_SLOT_COUNT =
            INPUT_SLOT_COUNT
                    + FUEL_SLOT_COUNT;

    private static final int DATA_COUNT =
            FoundryControllerBlockEntity.DATA_COUNT;

    private static final int INPUT_MENU_START = 0;
    private static final int INPUT_MENU_END = 8;

    private static final int FUEL_MENU_START = 8;
    private static final int FUEL_MENU_END = 12;

    private static final int PLAYER_INVENTORY_START = 12;
    private static final int PLAYER_INVENTORY_END = 39;

    private static final int[] INPUT_SLOT_X = {
            43,
            43,
            62,
            62,
            81,
            81,
            100,
            100
    };

    private static final int[] INPUT_SLOT_Y = {
            78,
            97,
            78,
            97,
            78,
            97,
            78,
            97
    };

    private static final int FUEL_SLOT_START_X = 43;
    private static final int FUEL_SLOT_Y = 121;
    private static final int SLOT_SPACING = 19;

    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 177;

    private final FoundryControllerBlockEntity blockEntity;
    private final Container inputContainer;
    private final Container fuelContainer;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    public FoundryControllerMenu(
            int containerId,
            Inventory playerInventory,
            FriendlyByteBuf extraData
    ) {
        this(
                containerId,
                playerInventory,
                getBlockEntity(
                        playerInventory,
                        extraData
                ),
                new SimpleContainerData(
                        DATA_COUNT
                )
        );
    }

    public FoundryControllerMenu(
            int containerId,
            Inventory playerInventory,
            FoundryControllerBlockEntity blockEntity
    ) {
        this(
                containerId,
                playerInventory,
                blockEntity,
                blockEntity.getData()
        );
    }

    private FoundryControllerMenu(
            int containerId,
            Inventory playerInventory,
            FoundryControllerBlockEntity blockEntity,
            ContainerData data
    ) {
        super(
                ModMenuTypes.FOUNDRY_CONTROLLER_MENU.get(),
                containerId
        );

        this.blockEntity = blockEntity;
        this.inputContainer = blockEntity.getInputInventory();
        this.fuelContainer = blockEntity.getFuelInventory();
        this.data = data;
        this.access = ContainerLevelAccess.create(
                blockEntity.getLevel(),
                blockEntity.getBlockPos()
        );

        checkContainerSize(
                inputContainer,
                INPUT_SLOT_COUNT
        );

        checkContainerSize(
                fuelContainer,
                FUEL_SLOT_COUNT
        );

        checkContainerDataCount(
                data,
                DATA_COUNT
        );

        inputContainer.startOpen(
                playerInventory.player
        );

        fuelContainer.startOpen(
                playerInventory.player
        );

        addDataSlots(
                data
        );

        addInputSlots();
        addFuelSlots();
        addPlayerInventory(
                playerInventory
        );
    }

    private void addInputSlots() {
        for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) {
            final int inputSlot = slot;

            addSlot(
                    new Slot(
                            inputContainer,
                            inputSlot,
                            INPUT_SLOT_X[inputSlot],
                            INPUT_SLOT_Y[inputSlot]
                    ) {

                        @Override
                        public boolean mayPlace(
                                ItemStack stack
                        ) {
                            return isInputSlotUnlocked(inputSlot)
                                    && blockEntity.canMelt(stack);
                        }

                        @Override
                        public boolean mayPickup(
                                Player player
                        ) {
                            /*
                             * Existing items remain removable even if a legacy
                             * save loads them into a currently locked slot.
                             */
                            return true;
                        }
                    }
            );
        }
    }

    private void addFuelSlots() {
        for (int slot = 0; slot < FUEL_SLOT_COUNT; slot++) {
            final int fuelSlot = slot;

            addSlot(
                    new Slot(
                            fuelContainer,
                            fuelSlot,
                            FUEL_SLOT_START_X
                                    + fuelSlot * SLOT_SPACING,
                            FUEL_SLOT_Y
                    ) {

                        @Override
                        public boolean mayPlace(
                                ItemStack stack
                        ) {
                            return isFuelSlotUnlocked(fuelSlot)
                                    && stack.is(Items.COAL);
                        }

                        @Override
                        public boolean mayPickup(
                                Player player
                        ) {
                            return true;
                        }
                    }
            );
        }
    }

    private static FoundryControllerBlockEntity getBlockEntity(
            Inventory playerInventory,
            FriendlyByteBuf extraData
    ) {
        var pos =
                extraData.readBlockPos();

        var blockEntity =
                playerInventory.player
                        .level()
                        .getBlockEntity(pos);

        if (
                blockEntity
                        instanceof FoundryControllerBlockEntity controller
        ) {
            return controller;
        }

        throw new IllegalStateException(
                "FoundryControllerBlockEntity missing at "
                        + pos
        );
    }

    public int getProgress() {
        return data.get(0);
    }

    public int getMaxProgress() {
        return data.get(1);
    }

    public int getScaledProgress(
            int width
    ) {
        int progress = getProgress();
        int maxProgress = getMaxProgress();

        if (
                progress <= 0
                        || maxProgress <= 0
        ) {
            return 0;
        }

        return progress
                * width
                / maxProgress;
    }

    public int getProgressPercent() {
        int maxProgress = getMaxProgress();

        if (maxProgress <= 0) {
            return 0;
        }

        return Math.max(
                0,
                Math.min(
                        100,
                        getProgress()
                                * 100
                                / maxProgress
                )
        );
    }

    public int getBurnTimeRemaining() {
        return data.get(
                FoundryControllerBlockEntity.BURN_TIME_DATA_INDEX
        );
    }

    public int getMaxBurnTime() {
        return data.get(
                FoundryControllerBlockEntity.MAX_BURN_TIME_DATA_INDEX
        );
    }

    public int getScaledBurnTime(
            int size
    ) {
        int burnTime = getBurnTimeRemaining();
        int maxBurnTime = getMaxBurnTime();

        if (
                burnTime <= 0
                        || maxBurnTime <= 0
        ) {
            return 0;
        }

        return burnTime
                * size
                / maxBurnTime;
    }

    public int getFoundryTier() {
        return data.get(
                FoundryControllerBlockEntity.TIER_DATA_INDEX
        );
    }

    public int getFoundryExperience() {
        return data.get(
                FoundryControllerBlockEntity.EXPERIENCE_DATA_INDEX
        );
    }

    public int getTierExperience() {
        return data.get(
                FoundryControllerBlockEntity.TIER_EXPERIENCE_DATA_INDEX
        );
    }

    public int getTierExperienceNeeded() {
        return Math.max(
                1,
                data.get(
                        FoundryControllerBlockEntity
                                .TIER_EXPERIENCE_NEEDED_DATA_INDEX
                )
        );
    }

    public int getScaledExperience(
            int width
    ) {
        if (getFoundryTier() >= 4) {
            return width;
        }

        return Math.max(
                0,
                Math.min(
                        width,
                        getTierExperience()
                                * width
                                / getTierExperienceNeeded()
                )
        );
    }

    public int getUnlockedInputSlotCount() {
        return Math.min(
                INPUT_SLOT_COUNT,
                Math.max(
                        2,
                        getFoundryTier() * 2
                )
        );
    }

    public int getUnlockedFuelSlotCount() {
        return Math.min(
                FUEL_SLOT_COUNT,
                Math.max(
                        1,
                        getFoundryTier()
                )
        );
    }

    public boolean isInputSlotUnlocked(
            int slot
    ) {
        return slot >= 0
                && slot < getUnlockedInputSlotCount();
    }

    public boolean isFuelSlotUnlocked(
            int slot
    ) {
        return slot >= 0
                && slot < getUnlockedFuelSlotCount();
    }

    public int getActiveInputSlot() {
        return data.get(
                FoundryControllerBlockEntity.ACTIVE_INPUT_SLOT_DATA_INDEX
        );
    }

    public boolean hasFuelAvailable() {
        if (getBurnTimeRemaining() > 0) {
            return true;
        }

        for (
                int slot = 0;
                slot < getUnlockedFuelSlotCount();
                slot++
        ) {
            if (
                    fuelContainer.getItem(slot)
                            .is(Items.COAL)
            ) {
                return true;
            }
        }

        return false;
    }

    public ItemStack getInputStack() {
        int activeSlot = getActiveInputSlot();

        if (
                activeSlot >= 0
                        && activeSlot < INPUT_SLOT_COUNT
        ) {
            return inputContainer.getItem(activeSlot);
        }

        for (
                int slot = 0;
                slot < getUnlockedInputSlotCount();
                slot++
        ) {
            ItemStack stack = inputContainer.getItem(slot);

            if (!stack.isEmpty()) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    public Optional<MoltenMetalDefinition> getInputMoltenMetalDefinition() {
        ItemStack input = getInputStack();

        if (
                input.isEmpty()
                        || blockEntity.getLevel() == null
        ) {
            return Optional.empty();
        }

        return blockEntity.getLevel()
                .getRecipeManager()
                .getRecipeFor(
                        ModRecipes.FOUNDRY_MELTING_TYPE.get(),
                        new SingleRecipeInput(input),
                        blockEntity.getLevel()
                )
                .map(
                        holder ->
                                holder.value()
                                        .moltenMetal()
                )
                .flatMap(
                        ModMoltenMetals::get
                );
    }

    public int getMetalAmount(
            MoltenMetalDefinition definition
    ) {
        int syncId =
                ModMoltenMetals.getSyncId(
                        definition.id()
                );

        if (syncId < 0) {
            return 0;
        }

        return data.get(
                FoundryControllerBlockEntity.METAL_DATA_START
                        + syncId
        );
    }

    public Optional<MoltenMetalDefinition> getSelectedMetalDefinition() {
        return ModMoltenMetals.bySyncId(
                data.get(
                        FoundryControllerBlockEntity
                                .SELECTED_METAL_DATA_INDEX
                )
        );
    }

    public int getTotalMoltenAmount() {
        return data.get(
                FoundryControllerBlockEntity.TOTAL_AMOUNT_DATA_INDEX
        );
    }

    public int getTankCapacity() {
        return data.get(
                FoundryControllerBlockEntity.CAPACITY_DATA_INDEX
        );
    }

    public int getTankCount() {
        int capacity = getTankCapacity();

        if (capacity <= 0) {
            return 0;
        }

        return capacity
                / FoundryTankBlockEntity.CAPACITY;
    }

    public int getMaximumTankCount() {
        return FoundryTankNetwork.MAX_TANK_COUNT;
    }

    @Override
    public boolean clickMenuButton(
            Player player,
            int buttonId
    ) {
        Optional<MoltenMetalDefinition> definition =
                ModMoltenMetals.bySyncId(
                        buttonId
                );

        return definition.isPresent()
                && blockEntity.setSelectedOutputMetal(
                definition.get()
                        .id()
        );
    }

    @Override
    public boolean stillValid(
            Player player
    ) {
        return stillValid(
                access,
                player,
                ModBlocks.FOUNDRY_CONTROLLER.get()
        );
    }

    @Override
    public ItemStack quickMoveStack(
            Player player,
            int index
    ) {
        Slot slot = slots.get(index);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack originalStack = stack.copy();

        if (index < MACHINE_SLOT_COUNT) {
            if (
                    !moveItemStackTo(
                            stack,
                            PLAYER_INVENTORY_START,
                            PLAYER_INVENTORY_END,
                            true
                    )
            ) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(Items.COAL)) {
            if (
                    !moveItemStackTo(
                            stack,
                            FUEL_MENU_START,
                            FUEL_MENU_START
                                    + getUnlockedFuelSlotCount(),
                            false
                    )
            ) {
                return ItemStack.EMPTY;
            }
        } else if (blockEntity.canMelt(stack)) {
            if (
                    !moveItemStackTo(
                            stack,
                            INPUT_MENU_START,
                            INPUT_MENU_START
                                    + getUnlockedInputSlotCount(),
                            false
                    )
            ) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(
                    ItemStack.EMPTY
            );
        } else {
            slot.setChanged();
        }

        if (
                stack.getCount()
                        == originalStack.getCount()
        ) {
            return ItemStack.EMPTY;
        }

        slot.onTake(
                player,
                stack
        );

        return originalStack;
    }

    @Override
    public void removed(
            Player player
    ) {
        super.removed(player);

        inputContainer.stopOpen(player);
        fuelContainer.stopOpen(player);
    }

    private void addPlayerInventory(
            Inventory playerInventory
    ) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(
                        new Slot(
                                playerInventory,
                                column
                                        + row * 9
                                        + 9,
                                PLAYER_INVENTORY_X
                                        + column * 18,
                                PLAYER_INVENTORY_Y
                                        + row * 18
                        )
                );
            }
        }
    }
}
