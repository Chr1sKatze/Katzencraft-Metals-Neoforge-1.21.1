package net.chriskatze.katzencraftmetals.menu;

import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.block.entity.FoundryControllerBlockEntity;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
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
    private static final int PLAYER_INVENTORY_END = 31;

    private static final int HOTBAR_START = 31;
    private static final int HOTBAR_END = 40;

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

        /*
         * Melting input.
         */
        addSlot(
                new Slot(
                        inputContainer,
                        FoundryControllerBlockEntity.INPUT_SLOT,
                        80,
                        35
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

        /*
         * Three coal slots formerly provided by the separate Fuel Chamber.
         */
        for (
                int slot = 0;
                slot < FUEL_SLOT_COUNT;
                slot++
        ) {
            addSlot(
                    new Slot(
                            fuelContainer,
                            slot,
                            8 + slot * 18,
                            35
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

    public int getScaledProgress(
            int width
    ) {
        int progress =
                data.get(0);

        int maxProgress =
                data.get(1);

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

    public int getScaledBurnTime(
            int width
    ) {
        int burnTime =
                data.get(
                        FoundryControllerBlockEntity.BURN_TIME_DATA_INDEX
                );

        int maxBurnTime =
                data.get(
                        FoundryControllerBlockEntity.MAX_BURN_TIME_DATA_INDEX
                );

        if (
                burnTime <= 0
                        || maxBurnTime <= 0
        ) {
            return 0;
        }

        return burnTime
                * width
                / maxBurnTime;
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
                        FoundryControllerBlockEntity.SELECTED_METAL_DATA_INDEX
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
        for (int row = 0; row < 3; row++) {
            for (
                    int column = 0;
                    column < 9;
                    column++
            ) {
                addSlot(
                        new Slot(
                                playerInventory,
                                column
                                        + row * 9
                                        + 9,
                                8
                                        + column * 18,
                                84
                                        + row * 18
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
                            8
                                    + column * 18,
                            142
                    )
            );
        }
    }
}
