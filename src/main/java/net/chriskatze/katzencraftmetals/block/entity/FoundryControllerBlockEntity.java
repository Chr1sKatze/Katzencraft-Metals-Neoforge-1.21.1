package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.block.custom.FoundryControllerBlock;
import net.chriskatze.katzencraftmetals.menu.FoundryControllerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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

import java.util.UUID;

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
    public static final int MOLTEN_IRON_PER_RAW_IRON = 6;

    /*
     * Every Controller owns one persistent UUID.
     *
     * Tanks assigned to this Foundry store this same UUID.
     */
    private UUID controllerId =
            UUID.randomUUID();

    private final SimpleContainer inputInventory =
            new SimpleContainer(SLOT_COUNT) {

                @Override
                public boolean canPlaceItem(
                        int slot,
                        ItemStack stack
                ) {
                    return stack.is(Items.RAW_IRON);
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    FoundryControllerBlockEntity.this.setChanged();
                }
            };

    private int progress;

    private final ContainerData data =
            new ContainerData() {

                @Override
                public int get(int index) {
                    return switch (index) {
                        case 0 -> progress;
                        case 1 -> MAX_PROGRESS;
                        case 2 -> {
                            FoundryTankBlockEntity tank =
                                    getConnectedTank();

                            yield tank != null
                                    ? tank.getMoltenAmount()
                                    : 0;
                        }
                        default -> 0;
                    };
                }

                @Override
                public void set(
                        int index,
                        int value
                ) {
                    if (index == 0) {
                        progress = value;
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
    // CONTROLLER ID / CONNECTION
    // =========================

    public UUID getControllerId() {
        return controllerId;
    }

    public BlockPos getAttachedTankPosition() {
        Direction front =
                getBlockState().getValue(
                        FoundryControllerBlock.FACING
                );

        return worldPosition.relative(
                front.getOpposite()
        );
    }

    @Nullable
    public FoundryTankNetwork getOwnedTankNetwork() {
        if (level == null) {
            return null;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(
                        getAttachedTankPosition()
                );

        if (!(blockEntity instanceof FoundryTankBlockEntity tank)) {
            return null;
        }

        FoundryTankNetwork network =
                tank.getNetwork();

        if (
                network == null
                        || !controllerId.equals(
                        network.getOwnerId()
                )
        ) {
            return null;
        }

        return network;
    }

    /**
     * Establishes or restores this Controller's automatic Tank connection.
     *
     * This also migrates worlds created by the previous prototype, where
     * Tank UUIDs were not yet the persistent Controller UUID.
     */
    public boolean ensureAttachedTankNetwork() {
        if (
                level == null
                        || level.isClientSide()
        ) {
            return false;
        }

        BlockPos tankPosition =
                getAttachedTankPosition();

        BlockEntity blockEntity =
                level.getBlockEntity(tankPosition);

        if (!(blockEntity instanceof FoundryTankBlockEntity tank)) {
            return false;
        }

        FoundryTankNetwork existing =
                tank.getNetwork();

        if (
                existing != null
                        && controllerId.equals(
                        existing.getOwnerId()
                )
        ) {
            return true;
        }

        /*
         * A differently owned active network belongs to another Controller
         * and must never be stolen.
         */
        if (
                existing != null
                        && existing.getOwnerId() != null
                        && existing.isActive()
        ) {
            return false;
        }

        /*
         * Old prototype IDs and cut-off owner IDs are treated as orphaned.
         */
        if (
                existing != null
                        && existing.getOwnerId() != null
                        && !existing.isActive()
        ) {
            existing.releaseOwnership();
        }

        return FoundryTankNetwork.claimLargestUnassignedLayout(
                level,
                tankPosition,
                controllerId
        ) != null;
    }

    public void releaseTankNetwork() {
        FoundryTankNetwork network =
                getOwnedTankNetwork();

        if (network != null) {
            network.releaseOwnership();
        }
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

        if (!controller.ensureAttachedTankNetwork()) {
            controller.resetProgress();
            return;
        }

        ItemStack inputStack =
                controller.inputInventory.getItem(INPUT_SLOT);

        if (!inputStack.is(Items.RAW_IRON)) {
            controller.resetProgress();
            return;
        }

        BlockEntity blockEntityBelow =
                level.getBlockEntity(pos.below());

        if (!(blockEntityBelow instanceof FuelChamberBlockEntity fuelChamber)) {
            controller.resetProgress();
            return;
        }

        FoundryTankBlockEntity tank =
                controller.getConnectedTank();

        if (tank == null) {
            controller.resetProgress();
            return;
        }

        if (!tank.canAccept(
                MOLTEN_IRON,
                MOLTEN_IRON_PER_RAW_IRON
        )) {
            return;
        }

        if (!fuelChamber.supplyBurnTick()) {
            return;
        }

        controller.progress++;

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

        int inserted =
                tank.insert(
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

    @Nullable
    private FoundryTankBlockEntity getConnectedTank() {
        if (level == null) {
            return null;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(
                        getAttachedTankPosition()
                );

        if (blockEntity instanceof FoundryTankBlockEntity tank) {
            FoundryTankNetwork network =
                    tank.getNetwork();

            if (
                    network != null
                            && controllerId.equals(
                            network.getOwnerId()
                    )
                            && network.isActive()
            ) {
                return tank;
            }
        }

        return null;
    }

    // =========================
    // SAVE / LOAD
    // =========================

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.saveAdditional(
                tag,
                registries
        );

        tag.putString(
                "ControllerId",
                controllerId.toString()
        );

        tag.put(
                "InputInventory",
                inputInventory.createTag(registries)
        );

        tag.putInt(
                "Progress",
                progress
        );
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        super.loadAdditional(
                tag,
                registries
        );

        if (tag.contains("ControllerId")) {
            try {
                controllerId =
                        UUID.fromString(
                                tag.getString(
                                        "ControllerId"
                                )
                        );
            } catch (IllegalArgumentException ignored) {
                controllerId =
                        UUID.randomUUID();
            }
        } else {
            controllerId =
                    UUID.randomUUID();
        }

        inputInventory.removeAllItems();

        if (tag.contains(
                "InputInventory",
                Tag.TAG_LIST
        )) {
            inputInventory.fromTag(
                    tag.getList(
                            "InputInventory",
                            Tag.TAG_COMPOUND
                    ),
                    registries
            );
        }

        progress =
                Math.max(
                        0,
                        tag.getInt("Progress")
                );
    }
}
