package net.chriskatze.katzencraftmetals.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.chriskatze.katzencraftmetals.menu.FuelChamberMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

public class FuelChamberBlockEntity
        extends BlockEntity
        implements MenuProvider {

    public static final int SLOT_COUNT = 3;

    /*
     * One coal provides 1600 active melting ticks.
     *
     * This value only decreases when the controller calls
     * supplyBurnTick().
     */
    public static final int COAL_BURN_TIME = 1600;

    private final SimpleContainer fuelInventory = new SimpleContainer(SLOT_COUNT) {

        /*
         * All three slots accept normal coal only.
         */
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.is(Items.COAL);
        }

        /*
         * Ensure inventory changes are saved.
         */
        @Override
        public void setChanged() {
            super.setChanged();
            FuelChamberBlockEntity.this.setChanged();
        }
    };

    private int burnTimeRemaining;
    private int maxBurnTime;

    public FuelChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_CHAMBER.get(), pos, state);
    }

    /**
     * Called by the future foundry controller while metal is
     * actively being melted.
     *
     * @return true when one melting tick was supplied
     */
    public boolean supplyBurnTick() {
        if (level == null || level.isClientSide()) {
            return false;
        }

        /*
         * Begin burning another coal only when the previous coal
         * has no stored burn time left.
         */
        if (burnTimeRemaining <= 0 && !consumeCoal()) {
            return false;
        }

        burnTimeRemaining--;
        setChanged();

        return true;
    }

    /**
     * Finds the first available coal slot and consumes one coal.
     */
    private boolean consumeCoal() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = fuelInventory.getItem(slot);

            if (!stack.is(Items.COAL)) {
                continue;
            }

            fuelInventory.removeItem(slot, 1);

            burnTimeRemaining = COAL_BURN_TIME;
            maxBurnTime = COAL_BURN_TIME;

            setChanged();
            return true;
        }

        return false;
    }

    public boolean hasAvailableFuel() {
        if (burnTimeRemaining > 0) {
            return true;
        }

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (fuelInventory.getItem(slot).is(Items.COAL)) {
                return true;
            }
        }

        return false;
    }

    public boolean isBurning() {
        return burnTimeRemaining > 0;
    }

    public int getBurnTimeRemaining() {
        return burnTimeRemaining;
    }

    public int getMaxBurnTime() {
        return maxBurnTime;
    }

    public int getStoredCoalCount() {
        int coalCount = 0;

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = fuelInventory.getItem(slot);

            if (stack.is(Items.COAL)) {
                coalCount += stack.getCount();
            }
        }

        return coalCount;
    }

    public SimpleContainer getFuelInventory() {
        return fuelInventory;
    }

    // =========================
    // SAVE / LOAD
    // =========================

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(tag, registries);

        tag.put(
                "FuelInventory",
                fuelInventory.createTag(registries)
        );

        tag.putInt(
                "BurnTimeRemaining",
                burnTimeRemaining
        );

        tag.putInt(
                "MaxBurnTime",
                maxBurnTime
        );
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(tag, registries);

        if (tag.contains("FuelInventory", Tag.TAG_LIST)) {
            fuelInventory.fromTag(
                    tag.getList(
                            "FuelInventory",
                            Tag.TAG_COMPOUND
                    ),
                    registries
            );
        }

        burnTimeRemaining = Math.max(
                0,
                tag.getInt("BurnTimeRemaining")
        );

        maxBurnTime = Math.max(
                0,
                tag.getInt("MaxBurnTime")
        );
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(
                "block.katzencraftmetals.fuel_chamber"
        );
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(
            int containerId,
            Inventory playerInventory,
            Player player
    ) {
        return new FuelChamberMenu(
                containerId,
                playerInventory,
                this
        );
    }
}