package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.menu.CrusherMenu;
import net.chriskatze.katzencraftmetals.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CrusherBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer {

    private final SimpleContainer inventory = new SimpleContainer(5);

    private static final int[] INPUT_SLOTS = new int[]{0};
    private static final int[] OUTPUT_SLOTS = new int[]{1, 2, 3};
    private static final int[] FUEL_SLOTS = new int[]{4};

    // 0 = input
    // 1 = main output
    // 2 = second output chance
    // 3 = third output chance
    // 4 = fuel

    private int progress = 0;
    private int maxProgress = 100;

    private int burnTime = 0;
    private int maxBurnTime = 0;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> CrusherBlockEntity.this.progress;
                case 1 -> CrusherBlockEntity.this.maxProgress;
                case 2 -> CrusherBlockEntity.this.burnTime;
                case 3 -> CrusherBlockEntity.this.maxBurnTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> CrusherBlockEntity.this.progress = value;
                case 2 -> CrusherBlockEntity.this.burnTime = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUSHER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CrusherBlockEntity be) {
        if (level.isClientSide()) return;

        // 🔥 Burn fuel continuously (even without input)
        if (be.burnTime > 0) {
            be.burnTime--;
            setChanged(level, pos, state);
        }

        ItemStack input = be.inventory.getItem(0);

        // ❌ No input → reset progress but keep burning fuel
        if (input.isEmpty()) {
            if (be.progress != 0) {
                be.progress = 0;
                setChanged(level, pos, state);
            }
            return;
        }

        SingleRecipeInput recipeInput = new SingleRecipeInput(input);

        var recipeOptional = level.getRecipeManager()
                .getRecipeFor(ModRecipes.CRUSHER_TYPE.get(), recipeInput, level);

        // ❌ No recipe → reset progress
        if (recipeOptional.isEmpty()) {
            if (be.progress != 0) {
                be.progress = 0;
                setChanged(level, pos, state);
            }
            return;
        }

        var recipe = recipeOptional.get().value();

        be.maxProgress = recipe.processingTime();

        ItemStack mainResult = recipe.getMainOutput();
        ItemStack secondResult = recipe.getSecondOutput();
        ItemStack thirdResult = recipe.getThirdOutput();

        // 🚫 Output full → pause WITHOUT wasting fuel
        if (!canInsert(be.inventory.getItem(1), mainResult)) {
            return;
        }

        // 🔥 Try to consume fuel if not burning
        if (be.burnTime <= 0) {
            ItemStack fuelStack = be.inventory.getItem(4);

            if (!fuelStack.isEmpty() && isFuel(fuelStack)) {
                be.burnTime = getFuelBurnTime(fuelStack);
                be.maxBurnTime = be.burnTime;
                fuelStack.shrink(1);
                setChanged(level, pos, state);
            }
        }

        // ❌ Still no fuel → reset progress and stop
        if (be.burnTime <= 0) {
            if (be.progress != 0) {
                be.progress = 0;
                setChanged(level, pos, state);
            }
            return;
        }

        // ⚙️ Progress machine (fuel already consumed above)
        be.progress++;

        if (be.progress < be.maxProgress) {
            setChanged(level, pos, state);
            return;
        }

        // 🎲 Roll bonus outputs
        boolean wantsSecond = !secondResult.isEmpty() && level.random.nextFloat() < recipe.secondChance();
        boolean wantsThird = !thirdResult.isEmpty() && level.random.nextFloat() < recipe.thirdChance();

        if (wantsSecond && !canInsert(be.inventory.getItem(2), secondResult)) return;
        if (wantsThird && !canInsert(be.inventory.getItem(3), thirdResult)) return;

        // ✅ Craft complete
        be.progress = 0;

        be.inventory.removeItem(0, 1);
        insert(be.inventory, 1, mainResult);

        if (wantsSecond) {
            insert(be.inventory, 2, secondResult);
        }

        if (wantsThird) {
            insert(be.inventory, 3, thirdResult);
        }

        setChanged(level, pos, state);
    }

    private static boolean canInsert(ItemStack slot, ItemStack result) {
        return slot.isEmpty()
                || (ItemStack.isSameItemSameComponents(slot, result)
                && slot.getCount() + result.getCount() <= slot.getMaxStackSize());
    }

    private static void insert(SimpleContainer inventory, int slotIndex, ItemStack stack) {
        ItemStack slot = inventory.getItem(slotIndex);

        if (slot.isEmpty()) {
            inventory.setItem(slotIndex, stack.copy());
        } else {
            slot.grow(stack.getCount());
        }
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return OUTPUT_SLOTS;
        }

        if (side == Direction.UP) {
            return INPUT_SLOTS;
        }

        return FUEL_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        if (slot == 0) {
            return hasCrusherRecipe(stack);
        }

        if (slot == 4) {
            return isFuel(stack);
        }

        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot >= 1 && slot <= 3;
    }

    @Override
    public int getContainerSize() {
        return inventory.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return inventory.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventory.getItem(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = inventory.removeItem(slot, amount);

        if (!stack.isEmpty()) {
            setChanged();
        }

        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = inventory.removeItemNoUpdate(slot);

        if (!stack.isEmpty()) {
            setChanged();
        }

        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        inventory.setItem(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        inventory.clearContent();
        setChanged();
    }

    private boolean hasCrusherRecipe(ItemStack stack) {
        if (stack.isEmpty() || level == null) {
            return false;
        }

        return level.getRecipeManager()
                .getRecipeFor(
                        ModRecipes.CRUSHER_TYPE.get(),
                        new SingleRecipeInput(stack),
                        level
                )
                .isPresent();
    }

    private static boolean isFuel(ItemStack stack) {
        return net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.isFuel(stack);
    }

    private static int getFuelBurnTime(ItemStack stack) {
        return net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
                .getFuel()
                .getOrDefault(stack.getItem(), 0);
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.katzencraftmetals.crusher");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CrusherMenu(containerId, playerInventory, this, this.data);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.put("inventory", inventory.createTag(registries));
        tag.putInt("progress", progress);
        tag.putInt("burnTime", burnTime);
        tag.putInt("maxBurnTime", maxBurnTime);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        inventory.fromTag(tag.getList("inventory", 10), registries);
        progress = tag.getInt("progress");
        burnTime = tag.getInt("burnTime");
        maxBurnTime = tag.getInt("maxBurnTime");
    }
}