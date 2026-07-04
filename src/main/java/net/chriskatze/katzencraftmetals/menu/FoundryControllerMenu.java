package net.chriskatze.katzencraftmetals.menu;

import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.block.entity.FoundryControllerBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankBlockEntity;
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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.Items;

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

    /*
     * Menu slot indices:
     *
     * 0      Controller melting input
     * 1-3    Controller fuel
     * 4-30   Player inventory
     * 31-39  Player hotbar
     */
    private static final int INPUT_MENU_SLOT = 0;
    private static final int FUEL_MENU_START = 1;
    private static final int FUEL_MENU_END = 4;

    private static final int PLAYER_INVENTORY_START = 4;
    private static final int PLAYER_INVENTORY_END = 22;

    private static final int HOTBAR_START = 22;
    private static final int HOTBAR_END = 31;

    /*
     * These positions match FoundryControllerUiLayout. They are repeated here
     * because the menu package must not depend on a client-only screen class.
     */
    private static final int INPUT_SLOT_X = 22;
    private static final int INPUT_SLOT_Y = 84;

    private static final int FUEL_SLOT_START_X = 13;
    private static final int FUEL_SLOT_Y = 124;
    private static final int FUEL_SLOT_SPACING = 19;

    private static final int PLAYER_INVENTORY_X = 49;
    private static final int PLAYER_INVENTORY_Y = 177;
    private static final int PLAYER_HOTBAR_Y = 213;

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

        this.blockEntity =
                blockEntity;

        this.inputContainer =
                blockEntity.getInputInventory();

        this.fuelContainer =
                blockEntity.getFuelInventory();

        this.data =
                data;

        this.access =
                ContainerLevelAccess.create(
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

        addSlot(
                new Slot(
                        inputContainer,
                        FoundryControllerBlockEntity.INPUT_SLOT,
                        INPUT_SLOT_X,
                        INPUT_SLOT_Y
                ) {

                    @Override
                    public boolean mayPlace(
                            ItemStack stack
                    ) {
                        return blockEntity.canMelt(
                                stack
                        );
                    }
                }
        );

        for (
                int slot = 0;
                slot < FUEL_SLOT_COUNT;
                slot++
        ) {
            addSlot(
                    new Slot(
                            fuelContainer,
                            slot,
                            FUEL_SLOT_START_X
                                    + slot * FUEL_SLOT_SPACING,
                            FUEL_SLOT_Y
                    ) {

                        @Override
                        public boolean mayPlace(
                                ItemStack stack
                        ) {
                            return stack.is(
                                    Items.COAL
                            );
                        }
                    }
            );
        }

        addPlayerInventory(
                playerInventory
        );

        addPlayerHotbar(
                playerInventory
        );
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
                        .getBlockEntity(
                                pos
                        );

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
        int progress =
                getProgress();

        int maxProgress =
                getMaxProgress();

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
        int maxProgress =
                getMaxProgress();

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
        int burnTime =
                getBurnTimeRemaining();

        int maxBurnTime =
                getMaxBurnTime();

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

    public boolean hasFuelAvailable() {
        if (getBurnTimeRemaining() > 0) {
            return true;
        }

        for (int slot = 0; slot < FUEL_SLOT_COUNT; slot++) {
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
        return inputContainer.getItem(
                FoundryControllerBlockEntity.INPUT_SLOT
        );
    }

    public Optional<MoltenMetalDefinition> getInputMoltenMetalDefinition() {
        ItemStack input =
                getInputStack();

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
                        new SingleRecipeInput(
                                input
                        ),
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
                FoundryControllerBlockEntity
                        .TOTAL_AMOUNT_DATA_INDEX
        );
    }

    public int getTankCapacity() {
        return data.get(
                FoundryControllerBlockEntity
                        .CAPACITY_DATA_INDEX
        );
    }

    public int getTankCount() {
        int capacity =
                getTankCapacity();

        if (capacity <= 0) {
            return 0;
        }

        return capacity
                / FoundryTankBlockEntity.CAPACITY;
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
        Slot slot =
                slots.get(
                        index
                );

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack =
                slot.getItem();

        ItemStack originalStack =
                stack.copy();

        if (index < MACHINE_SLOT_COUNT) {
            if (
                    !moveItemStackTo(
                            stack,
                            PLAYER_INVENTORY_START,
                            HOTBAR_END,
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
                            FUEL_MENU_END,
                            false
                    )
            ) {
                return ItemStack.EMPTY;
            }
        } else if (blockEntity.canMelt(stack)) {
            if (
                    !moveItemStackTo(
                            stack,
                            INPUT_MENU_SLOT,
                            INPUT_MENU_SLOT + 1,
                            false
                    )
            ) {
                return ItemStack.EMPTY;
            }
        } else if (
                index >= PLAYER_INVENTORY_START
                        && index < PLAYER_INVENTORY_END
        ) {
            if (
                    !moveItemStackTo(
                            stack,
                            HOTBAR_START,
                            HOTBAR_END,
                            false
                    )
            ) {
                return ItemStack.EMPTY;
            }
        } else if (
                index >= HOTBAR_START
                        && index < HOTBAR_END
        ) {
            if (
                    !moveItemStackTo(
                            stack,
                            PLAYER_INVENTORY_START,
                            PLAYER_INVENTORY_END,
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
        super.removed(
                player
        );

        inputContainer.stopOpen(
                player
        );

        fuelContainer.stopOpen(
                player
        );
    }

    private void addPlayerInventory(
            Inventory playerInventory
    ) {
        /*
         * The top normal inventory row (indices 9-17) is intentionally hidden
         * on this large machine screen. The two lower rows remain available and
         * the reclaimed 18 pixels are used by the Controller panels.
         */
        for (int visibleRow = 0; visibleRow < 2; visibleRow++) {
            int inventoryRow =
                    visibleRow + 1;

            for (
                    int column = 0;
                    column < 9;
                    column++
            ) {
                addSlot(
                        new Slot(
                                playerInventory,
                                column
                                        + inventoryRow * 9
                                        + 9,
                                PLAYER_INVENTORY_X
                                        + column * 18,
                                PLAYER_INVENTORY_Y
                                        + visibleRow * 18
                        )
                );
            }
        }
    }

    private void addPlayerHotbar(
            Inventory playerInventory
    ) {
        for (
                int column = 0;
                column < 9;
                column++
        ) {
            addSlot(
                    new Slot(
                            playerInventory,
                            column,
                            PLAYER_INVENTORY_X
                                    + column * 18,
                            PLAYER_HOTBAR_Y
                    )
            );
        }
    }
}
