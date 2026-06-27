package net.chriskatze.katzencraftmetals.menu;

import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.block.entity.FuelChamberBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class FuelChamberMenu extends AbstractContainerMenu {

    private static final int FUEL_SLOT_COUNT = 3;

    private final Container container;
    private final ContainerLevelAccess access;

    /*
     * Client constructor.
     *
     * The block position is sent when the menu is opened.
     */
    public FuelChamberMenu(
            int containerId,
            Inventory playerInventory,
            FriendlyByteBuf extraData
    ) {
        this(
                containerId,
                playerInventory,
                getBlockEntity(playerInventory, extraData)
        );
    }

    /*
     * Server constructor.
     */
    public FuelChamberMenu(
            int containerId,
            Inventory playerInventory,
            FuelChamberBlockEntity blockEntity
    ) {
        super(ModMenuTypes.FUEL_CHAMBER_MENU.get(), containerId);

        this.container = blockEntity.getFuelInventory();
        this.access = ContainerLevelAccess.create(
                blockEntity.getLevel(),
                blockEntity.getBlockPos()
        );

        checkContainerSize(container, FUEL_SLOT_COUNT);
        container.startOpen(playerInventory.player);

        // =========================
        // FUEL CHAMBER SLOTS
        // =========================

        for (int slot = 0; slot < FUEL_SLOT_COUNT; slot++) {
            int x = 62 + slot * 18;

            this.addSlot(new Slot(container, slot, x, 35) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(Items.COAL);
                }
            });
        }

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private static FuelChamberBlockEntity getBlockEntity(
            Inventory playerInventory,
            FriendlyByteBuf extraData
    ) {
        var pos = extraData.readBlockPos();
        var blockEntity = playerInventory.player.level().getBlockEntity(pos);

        if (!(blockEntity instanceof FuelChamberBlockEntity fuelChamber)) {
            throw new IllegalStateException(
                    "Fuel Chamber block entity not found at " + pos
            );
        }

        return fuelChamber;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(
                access,
                player,
                ModBlocks.FUEL_CHAMBER.get()
        );
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack originalStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        originalStack = stack.copy();

        /*
         * Menu indices:
         *
         * 0-2   Fuel Chamber
         * 3-29  Player inventory
         * 30-38 Player hotbar
         */
        if (index < FUEL_SLOT_COUNT) {
            if (!moveItemStackTo(
                    stack,
                    FUEL_SLOT_COUNT,
                    this.slots.size(),
                    true
            )) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(Items.COAL)) {
            if (!moveItemStackTo(
                    stack,
                    0,
                    FUEL_SLOT_COUNT,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else if (index < 30) {
            if (!moveItemStackTo(
                    stack,
                    30,
                    39,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(
                    stack,
                    3,
                    30,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == originalStack.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);

        return originalStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        8 + column * 18,
                        84 + row * 18
                ));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(
                    playerInventory,
                    column,
                    8 + column * 18,
                    142
            ));
        }
    }
}