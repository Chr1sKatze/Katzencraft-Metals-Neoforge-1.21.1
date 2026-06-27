package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.menu.FoundryControllerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import net.chriskatze.katzencraftmetals.block.custom.FoundryControllerBlock;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public class FoundryControllerBlockEntity
        extends BlockEntity
        implements MenuProvider {

    public static final ResourceLocation MOLTEN_IRON =
            ResourceLocation.fromNamespaceAndPath(
                    "minecraft",
                    "iron"
            );

    public static final int INPUT_SLOT = 0;
    public static final int SLOT_COUNT = 1;

    public static final int MAX_PROGRESS = 20;

    /*
     * Temporary storage value.
     *
     * Later, these units will be transferred into the actual
     * molten-metal tank.
     */
    public static final int MOLTEN_IRON_PER_RAW_IRON = 6;

    private final SimpleContainer inputInventory =
            new SimpleContainer(SLOT_COUNT) {

                @Override
                public boolean canPlaceItem(int slot, ItemStack stack) {
                    return stack.is(Items.RAW_IRON);
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    FoundryControllerBlockEntity.this.setChanged();
                }
            };

    private int progress;

    /*
     * Values synchronized with the open menu:
     *
     * 0 = progress
     * 1 = maximum progress
     * 2 = molten iron amount
     */
    private final ContainerData data = new ContainerData() {

        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> MAX_PROGRESS;
                case 2 -> {
                    FoundryTankBlockEntity tank = getConnectedTank();

                    yield tank != null
                            ? tank.getMoltenAmount()
                            : 0;
                }
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public FoundryControllerBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                ModBlockEntities.FOUNDRY_CONTROLLER.get(),
                pos,
                state
        );
    }

    // =========================
    // SERVER TICK
    // =========================

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            FoundryControllerBlockEntity controller
    ) {
        if (level.isClientSide()) {
            return;
        }

        ItemStack inputStack =
                controller.inputInventory.getItem(INPUT_SLOT);

        /*
         * No valid melting input:
         * cancel incomplete progress.
         */
        if (!inputStack.is(Items.RAW_IRON)) {
            controller.resetProgress();
            return;
        }

        /*
         * The Fuel Chamber must be directly below
         * the Foundry Controller.
         */
        BlockEntity blockEntityBelow =
                level.getBlockEntity(pos.below());

        if (!(blockEntityBelow instanceof FuelChamberBlockEntity fuelChamber)) {
            /*
             * Removing or replacing the Fuel Chamber
             * invalidates the current melting operation.
             */
            controller.resetProgress();
            return;
        }

        /*
         * Determine the position directly behind the Controller.
         */
        Direction front =
                state.getValue(FoundryControllerBlock.FACING);

        BlockPos tankPosition =
                pos.relative(front.getOpposite());

        BlockEntity tankBlockEntity =
                level.getBlockEntity(tankPosition);

        /*
         * The Foundry Tank must be directly behind
         * the Controller.
         */
        if (!(tankBlockEntity instanceof FoundryTankBlockEntity tank)) {
            /*
             * Removing or replacing the Tank invalidates
             * the current melting operation.
             */
            controller.resetProgress();
            return;
        }

        /*
         * A full Tank, or a Tank containing another metal,
         * pauses melting without consuming fuel.
         */
        if (!tank.canAccept(
                MOLTEN_IRON,
                MOLTEN_IRON_PER_RAW_IRON
        )) {
            return;
        }

        /*
         * The Fuel Chamber only consumes a burn tick while
         * a valid melting operation is actively progressing.
         *
         * Running out of coal pauses progress rather than
         * resetting it.
         */
        if (!fuelChamber.supplyBurnTick()) {
            return;
        }

        controller.progress++;

        /*
         * Once enough active melting ticks have passed,
         * transfer the molten iron into the Tank.
         */
        if (controller.progress >= MAX_PROGRESS) {
            controller.finishMelting(tank);
        }

        controller.setChanged();
    }

    private void finishMelting(
            FoundryTankBlockEntity tank
    ) {
        ItemStack inputStack =
                inputInventory.getItem(INPUT_SLOT);

        if (!inputStack.is(Items.RAW_IRON)) {
            resetProgress();
            return;
        }

        if (!tank.canAccept(
                MOLTEN_IRON,
                MOLTEN_IRON_PER_RAW_IRON
        )) {
            return;
        }

        int inserted = tank.insert(
                MOLTEN_IRON,
                MOLTEN_IRON_PER_RAW_IRON
        );

        if (inserted != MOLTEN_IRON_PER_RAW_IRON) {
            return;
        }

        inputStack.shrink(1);
        progress = 0;

        inputInventory.setChanged();
        setChanged();
    }

    private void resetProgress() {
        if (progress == 0) {
            return;
        }

        progress = 0;
        setChanged();
    }

    // =========================
    // MENU
    // =========================

    @Override
    public Component getDisplayName() {
        return Component.translatable(
                "block.katzencraftmetals.foundry_controller"
        );
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(
            int containerId,
            Inventory playerInventory,
            Player player
    ) {
        return new FoundryControllerMenu(
                containerId,
                playerInventory,
                this
        );
    }

    // =========================
    // GETTERS
    // =========================

    public SimpleContainer getInputInventory() {
        return inputInventory;
    }

    public ContainerData getData() {
        return data;
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return MAX_PROGRESS;
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
                "InputInventory",
                inputInventory.createTag(registries)
        );

        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(tag, registries);

        inputInventory.removeAllItems();

        if (tag.contains("InputInventory", Tag.TAG_LIST)) {
            inputInventory.fromTag(
                    tag.getList(
                            "InputInventory",
                            Tag.TAG_COMPOUND
                    ),
                    registries
            );
        }

        progress = Math.max(
                0,
                tag.getInt("Progress")
        );
    }

    @Nullable
    private FoundryTankBlockEntity getConnectedTank() {
        if (level == null) {
            return null;
        }

        Direction front =
                getBlockState().getValue(
                        FoundryControllerBlock.FACING
                );

        /*
         * The tank stands behind the Controller.
         */
        BlockPos tankPosition =
                worldPosition.relative(front.getOpposite());

        BlockEntity blockEntity =
                level.getBlockEntity(tankPosition);

        if (blockEntity instanceof FoundryTankBlockEntity tank) {
            return tank;
        }

        return null;
    }
}