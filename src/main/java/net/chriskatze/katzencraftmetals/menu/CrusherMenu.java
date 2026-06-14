package net.chriskatze.katzencraftmetals.menu;

import net.chriskatze.katzencraftmetals.block.ModBlocks;
import net.chriskatze.katzencraftmetals.block.entity.CrusherBlockEntity;
import net.chriskatze.katzencraftmetals.recipe.ModRecipes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

public class CrusherMenu extends AbstractContainerMenu {

    private final CrusherBlockEntity blockEntity;
    private final Container container;
    private final ContainerData data;

    public CrusherMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                getBlockEntity(playerInventory, extraData),
                new SimpleContainerData(4));
    }

    public CrusherMenu(int containerId, Inventory playerInventory, CrusherBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.CRUSHER_MENU.get(), containerId);

        this.blockEntity = blockEntity;
        this.container = blockEntity.getInventory();
        this.data = data;

        addDataSlots(data);

        // Input slot
        this.addSlot(new Slot(container, 0, 56, 17) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return hasCrusherRecipe(stack);
            }
        });

        // Fuel slot
        this.addSlot(new Slot(container, 4, 56, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AbstractFurnaceBlockEntity.isFuel(stack);
            }
        });

        // Output slots
        this.addSlot(new OutputSlot(container, 1, 110, 35));
        this.addSlot(new OutputSlot(container, 2, 130, 35));
        this.addSlot(new OutputSlot(container, 3, 150, 35));

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private static CrusherBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf extraData) {
        var pos = extraData.readBlockPos();
        var blockEntity = playerInventory.player.level().getBlockEntity(pos);

        if (blockEntity instanceof CrusherBlockEntity crusherBlockEntity) {
            return crusherBlockEntity;
        }

        throw new IllegalStateException("CrusherBlockEntity missing at " + pos);
    }

    public int getScaledProgress() {
        int progress = data.get(0);
        int maxProgress = data.get(1);
        int arrowSize = 24;

        return maxProgress != 0 && progress != 0
                ? progress * arrowSize / maxProgress
                : 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(
                ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player,
                ModBlocks.CRUSHER.get()
        );
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack originalStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            originalStack = stackInSlot.copy();

            // Machine slots: menu indices 0-4
            if (index < 5) {
                if (!this.moveItemStackTo(stackInSlot, 5, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                boolean moved = false;

                // Fuel slot is MENU INDEX 1
                if (AbstractFurnaceBlockEntity.isFuel(stackInSlot)) {
                    moved = this.moveItemStackTo(stackInSlot, 1, 2, false);
                }

                // Input slot is MENU INDEX 0
                if (!moved && hasCrusherRecipe(stackInSlot)) {
                    moved = this.moveItemStackTo(stackInSlot, 0, 1, false);
                }

                if (!moved) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return originalStack;
    }

    private boolean hasCrusherRecipe(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        if (blockEntity.getLevel() == null) {
            return false;
        }

        return blockEntity.getLevel().getRecipeManager()
                .getRecipeFor(
                        ModRecipes.CRUSHER_TYPE.get(),
                        new SingleRecipeInput(stack),
                        blockEntity.getLevel()
                )
                .isPresent();
    }

    public int getScaledFuelProgress() {
        int burnTime = data.get(2);
        int maxBurnTime = data.get(3);

        if (maxBurnTime <= 0) {
            return 0;
        }

        return Math.max(1, burnTime * 14 / maxBurnTime);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    private static class OutputSlot extends Slot {
        public OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}