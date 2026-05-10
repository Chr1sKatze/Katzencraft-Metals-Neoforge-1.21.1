package net.chriskatze.katzencraftmetals.menu;

import net.chriskatze.katzencraftmetals.repair.CatoRepairConfig;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockState;

public class KatzencraftAnvilMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess access;

    private final Container inputSlots = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            KatzencraftAnvilMenu.this.updateResult();
        }
    };

    private final ResultContainer resultSlot = new ResultContainer();

    public KatzencraftAnvilMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public KatzencraftAnvilMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(ModMenuTypes.KATZENCRAFT_ANVIL_MENU.get(), containerId);
        this.access = access;

        // Slot 0: damaged item
        this.addSlot(new Slot(inputSlots, 0, 27, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isValidRepairTarget(stack);
            }
        });

        // Slot 1: repair material
        this.addSlot(new Slot(inputSlots, 1, 76, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isValidRepairMaterial(stack);
            }
        });

        // Slot 2: output
        this.addSlot(new Slot(resultSlot, 0, 134, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                ItemStack damagedItem = inputSlots.getItem(0);
                ItemStack repairMaterial = inputSlots.getItem(1);

                if (!damagedItem.isEmpty()) {
                    damagedItem.shrink(1);
                }

                if (!repairMaterial.isEmpty()) {
                    repairMaterial.shrink(1);
                }

                // 🔊 Play anvil use sound with slight pitch variation
                player.level().playSound(
                        null,
                        player.blockPosition(),
                        net.minecraft.sounds.SoundEvents.ANVIL_USE,
                        net.minecraft.sounds.SoundSource.BLOCKS,
                        1.0F,
                        1.0F + player.level().random.nextFloat() * 0.1F
                );

                damageAnvil(player);

                resultSlot.setItem(0, ItemStack.EMPTY);
                updateResult();

                super.onTake(player, stack);
            }
        });

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    private void updateResult() {
        ItemStack damagedItem = inputSlots.getItem(0);
        ItemStack repairMaterial = inputSlots.getItem(1);

        if (damagedItem.isEmpty() || repairMaterial.isEmpty()) {
            resultSlot.setItem(0, ItemStack.EMPTY);
            return;
        }

        if (!damagedItem.isDamaged()) {
            resultSlot.setItem(0, ItemStack.EMPTY);
            return;
        }

        CatoRepairConfig.RepairEntry repairEntry = CatoRepairConfig.get(damagedItem.getItem());

        if (repairEntry == null) {
            resultSlot.setItem(0, ItemStack.EMPTY);
            return;
        }

        if (repairMaterial.getItem() != repairEntry.material()) {
            resultSlot.setItem(0, ItemStack.EMPTY);
            return;
        }

        ItemStack result = damagedItem.copy();
        result.setCount(1);

        int maxDamage = result.getMaxDamage();
        int repairAmount = Math.round(maxDamage * repairEntry.repairPercent());

        int newDamageValue = Math.max(0, result.getDamageValue() - repairAmount);
        result.setDamageValue(newDamageValue);

        resultSlot.setItem(0, result);
    }

    private boolean isValidRepairTarget(ItemStack stack) {
        if (!stack.isDamaged()) {
            return false;
        }

        if (!stack.isDamageableItem()) {
            return false;
        }

        if (!CatoRepairConfig.canRepair(stack.getItem())) {
            return false;
        }

        ItemStack materialStack = inputSlots.getItem(1);

        if (!materialStack.isEmpty()) {
            CatoRepairConfig.RepairEntry repairEntry = CatoRepairConfig.get(stack.getItem());

            if (repairEntry == null) {
                return false;
            }

            return materialStack.getItem() == repairEntry.material();
        }

        return true;
    }

    private boolean isValidRepairMaterial(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        ItemStack damagedItem = inputSlots.getItem(0);

        if (!damagedItem.isEmpty()) {
            CatoRepairConfig.RepairEntry repairEntry = CatoRepairConfig.get(damagedItem.getItem());

            if (repairEntry == null) {
                return false;
            }

            return stack.getItem() == repairEntry.material();
        }

        return isAnyConfiguredRepairMaterial(stack);
    }

    private boolean isAnyConfiguredRepairMaterial(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        for (CatoRepairConfig.RepairEntry entry : CatoRepairConfig.getAllEntries()) {
            if (stack.getItem() == entry.material()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> {
            BlockState state = level.getBlockState(pos);

            if (!(state.getBlock() instanceof AnvilBlock)) {
                return false;
            }

            return player.distanceToSqr(
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D
            ) <= 64.0D;
        }, true);
    }

    private void damageAnvil(Player player) {
        if (player.hasInfiniteMaterials()) {
            return;
        }

        access.execute((level, pos) -> {
            if (level.isClientSide) {
                return;
            }

            if (level.random.nextFloat() >= 0.12F) {
                level.levelEvent(1030, pos, 0);
                return;
            }

            BlockState state = level.getBlockState(pos);

            if (!(state.getBlock() instanceof AnvilBlock)) {
                return;
            }

            BlockState damagedState = AnvilBlock.damage(state);

            if (damagedState == null) {
                level.removeBlock(pos, false);
            } else {
                level.setBlock(pos, damagedState, 2);
            }

            level.levelEvent(1029, pos, 0);
        });
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack originalStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            originalStack = stackInSlot.copy();

            if (index == 2) {
                if (!this.moveItemStackTo(stackInSlot, 3, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(stackInSlot, originalStack);
            } else if (index == 0 || index == 1) {
                if (!this.moveItemStackTo(stackInSlot, 3, this.slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (isValidRepairTarget(stackInSlot)) {
                    if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (isValidRepairMaterial(stackInSlot)) {
                    if (!this.moveItemStackTo(stackInSlot, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == originalStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stackInSlot);
        }

        return originalStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        if (!player.level().isClientSide) {
            this.clearContainer(player, inputSlots);
        }
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
}