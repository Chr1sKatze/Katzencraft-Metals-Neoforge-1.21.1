package net.chriskatze.katzencraftmetals.menu;

import net.chriskatze.katzencraftmetals.enchantment.CatoEnchantments;
import net.chriskatze.katzencraftmetals.enchantment.scroll.ScrollHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class CatoEnchantingMenu extends AbstractContainerMenu {

    private final BlockPos tablePos;

    private final Container inputSlots = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            CatoEnchantingMenu.this.slotsChanged(this);
        }
    };

    public CatoEnchantingMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos());
    }

    public CatoEnchantingMenu(int containerId, Inventory playerInventory, BlockPos tablePos) {
        super(ModMenuTypes.CATO_ENCHANTING_MENU.get(), containerId);

        this.tablePos = tablePos;

        this.addSlot(new Slot(inputSlots, 0, 35, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return !stack.isEmpty() && canBeCatoEnchanted(stack);
            }
        });

        this.addSlot(new Slot(inputSlots, 1, 80, 47) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ScrollHelper.isScroll(stack);
            }
        });

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    public ItemStack getTargetStack() {
        return inputSlots.getItem(0);
    }

    public ItemStack getScrollStack() {
        return inputSlots.getItem(1);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack originalStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            originalStack = stackInSlot.copy();

            if (index < 2) {
                if (!this.moveItemStackTo(stackInSlot, 2, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (ScrollHelper.isScroll(stackInSlot)) {
                    if (!this.moveItemStackTo(stackInSlot, 1, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (canBeCatoEnchanted(stackInSlot)) {
                    if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) {
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

    public void applyEnchant(Player player, int optionIndex) {
        if (player.level().isClientSide) {
            return;
        }

        if (countNearbyBookshelves(player.level(), tablePos) < 15) {
            return;
        }

        ItemStack target = getTargetStack();
        ItemStack scroll = getScrollStack();

        if (target.isEmpty() || scroll.isEmpty()) {
            return;
        }

        var scrollInfoOptional = ScrollHelper.getScrollInfo(scroll);

        if (scrollInfoOptional.isEmpty()) {
            return;
        }

        var scrollInfo = scrollInfoOptional.get();

        var options = CatoEnchantments.getAvailableDefinitions(scrollInfo.category(), target);

        if (optionIndex < 0 || optionIndex >= options.size()) {
            return;
        }

        var definition = options.get(optionIndex);

        var enchantmentOptional = CatoEnchantments.resolve(
                player.level().registryAccess(),
                definition
        );

        if (enchantmentOptional.isEmpty()) {
            return;
        }

        var enchantment = enchantmentOptional.get();

        int targetLevel = scrollInfo.tier().targetLevel();
        int maxLevel = Math.min(definition.maxLevel(), CatoEnchantments.GLOBAL_MAX_ENCHANT_LEVEL);

        if (targetLevel > maxLevel) {
            return;
        }

        var existingEnchantments = EnchantmentHelper.getEnchantmentsForCrafting(target);
        int currentLevel = existingEnchantments.getLevel(enchantment);

        if (currentLevel != targetLevel - 1) {
            return;
        }

        if (currentLevel == 0 && existingEnchantments.entrySet().size() >= CatoEnchantments.MAX_ENCHANTMENTS_PER_ITEM) {
            return;
        }

        EnchantmentHelper.updateEnchantments(target, mutableEnchantments -> {
            mutableEnchantments.set(enchantment, targetLevel);
        });

        scroll.shrink(1);

        inputSlots.setChanged();
        this.broadcastChanges();
    }

    private static boolean canBeCatoEnchanted(ItemStack stack) {
        return CatoEnchantments.getDefinitions().stream()
                .anyMatch(definition -> definition.validTarget().test(stack));
    }

    private static int countNearbyBookshelves(Level level, BlockPos tablePos) {
        int count = 0;

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) != 2 && Math.abs(z) != 2) {
                    continue;
                }

                for (int y = 0; y <= 1; y++) {
                    BlockPos shelfPos = tablePos.offset(x, y, z);

                    if (level.getBlockState(shelfPos).is(Blocks.BOOKSHELF)) {
                        count++;
                    }
                }
            }
        }

        return Math.min(count, 15);
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