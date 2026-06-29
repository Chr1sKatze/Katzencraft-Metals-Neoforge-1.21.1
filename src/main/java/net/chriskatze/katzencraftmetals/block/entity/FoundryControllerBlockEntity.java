package net.chriskatze.katzencraftmetals.block.entity;

import net.chriskatze.katzencraftmetals.menu.FoundryControllerMenu;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class FoundryControllerBlockEntity
        extends BlockEntity
        implements MenuProvider {

    public static final ResourceLocation MOLTEN_IRON =
            ModMoltenMetals.IRON.id();

    public static final int INPUT_SLOT = 0;
    public static final int INPUT_SLOT_COUNT = 1;

    public static final int FUEL_SLOT_COUNT =
            FoundryControllerFuelSystem.FUEL_SLOT_COUNT;

    public static final int SLOT_COUNT =
            INPUT_SLOT_COUNT
                    + FUEL_SLOT_COUNT;

    public static final int MAX_PROGRESS = 20;

    public static final int COAL_BURN_TIME =
            FoundryControllerFuelSystem.COAL_BURN_TIME;

    public static final int MOLTEN_IRON_PER_RAW_IRON =
            ModMoltenMetals.IRON.unitsPerOre();

    private UUID controllerId =
            UUID.randomUUID();

    private final FoundryControllerProcessing processing =
            new FoundryControllerProcessing(this);

    private final FoundryControllerFuelSystem fuelSystem =
            new FoundryControllerFuelSystem(this);

    private final SimpleContainer inputInventory =
            new SimpleContainer(INPUT_SLOT_COUNT) {

                @Override
                public boolean canPlaceItem(
                        int slot,
                        ItemStack stack
                ) {
                    return slot == INPUT_SLOT
                            && processing.canMelt(
                            stack
                    );
                }

                @Override
                public void setChanged() {
                    super.setChanged();
                    FoundryControllerBlockEntity.this.setChanged();
                }
            };

    public static final int METAL_DATA_START = 2;

    public static final int METAL_DATA_COUNT =
            ModMoltenMetals.values().size();

    public static final int SELECTED_METAL_DATA_INDEX =
            METAL_DATA_START
                    + METAL_DATA_COUNT;

    public static final int TOTAL_AMOUNT_DATA_INDEX =
            SELECTED_METAL_DATA_INDEX
                    + 1;

    public static final int CAPACITY_DATA_INDEX =
            TOTAL_AMOUNT_DATA_INDEX
                    + 1;

    public static final int BURN_TIME_DATA_INDEX =
            CAPACITY_DATA_INDEX
                    + 1;

    public static final int MAX_BURN_TIME_DATA_INDEX =
            BURN_TIME_DATA_INDEX
                    + 1;

    public static final int DATA_COUNT =
            MAX_BURN_TIME_DATA_INDEX
                    + 1;

    private final ContainerData data =
            new ContainerData() {

                @Override
                public int get(
                        int index
                ) {
                    if (index == 0) {
                        return processing.getProgress();
                    }

                    if (index == 1) {
                        return processing.getMaxProgress();
                    }

                    FoundryTankNetwork network =
                            getOwnedTankNetwork();

                    if (
                            index >= METAL_DATA_START
                                    && index < SELECTED_METAL_DATA_INDEX
                    ) {
                        int syncId =
                                index
                                        - METAL_DATA_START;

                        ResourceLocation metal =
                                ModMoltenMetals.bySyncId(
                                                syncId
                                        )
                                        .map(
                                                definition ->
                                                        definition.id()
                                        )
                                        .orElse(null);

                        return network != null
                                && metal != null
                                ? network.getMoltenAmount(
                                metal
                        )
                                : 0;
                    }

                    if (index == SELECTED_METAL_DATA_INDEX) {
                        ResourceLocation selected =
                                network != null
                                        ? processing
                                        .getSelectedOutputMetalOrDefault(
                                                network
                                        )
                                        : null;

                        return selected != null
                                ? ModMoltenMetals.getSyncId(
                                selected
                        )
                                : -1;
                    }

                    if (index == TOTAL_AMOUNT_DATA_INDEX) {
                        return network != null
                                ? network.getTotalMoltenAmount()
                                : 0;
                    }

                    if (index == CAPACITY_DATA_INDEX) {
                        return network != null
                                ? network.getCapacity()
                                : 0;
                    }

                    if (index == BURN_TIME_DATA_INDEX) {
                        return fuelSystem.getBurnTimeRemaining();
                    }

                    if (index == MAX_BURN_TIME_DATA_INDEX) {
                        return fuelSystem.getMaxBurnTime();
                    }

                    return 0;
                }

                @Override
                public void set(
                        int index,
                        int value
                ) {
                    if (index == 0) {
                        processing.setProgressFromMenuData(
                                value
                        );

                        return;
                    }

                    if (index == 1) {
                        processing.setMaxProgressFromMenuData(
                                value
                        );
                    }
                }

                @Override
                public int getCount() {
                    return DATA_COUNT;
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
    // CONTROLLER / TANK NETWORK
    // =========================

    public UUID getControllerId() {
        return controllerId;
    }

    public Direction getFacing() {
        return FoundryControllerNetwork.getFacing(
                this
        );
    }

    public boolean isValidTankAttachmentPosition(
            BlockPos tankPos
    ) {
        return FoundryControllerNetwork
                .isValidTankAttachmentPosition(
                        this,
                        tankPos
                );
    }

    public boolean canOwnTankLayout(
            Set<BlockPos> tankPositions
    ) {
        return FoundryControllerNetwork
                .canOwnTankLayout(
                        this,
                        tankPositions
                );
    }

    public List<BlockPos> getValidTankAttachmentPositions() {
        return FoundryControllerNetwork
                .getValidTankAttachmentPositions(
                        this
                );
    }

    @Nullable
    public FoundryTankNetwork getOwnedTankNetwork() {
        return FoundryControllerNetwork
                .getOwnedTankNetwork(
                        this
                );
    }

    public boolean ensureTankNetwork() {
        return FoundryControllerNetwork
                .ensureTankNetwork(
                        this
                );
    }

    public void releaseFoundry() {
        FoundryControllerNetwork.releaseFoundry(
                this
        );
    }

    // =========================
    // PROCESSING / OUTPUT
    // =========================

    public boolean canMelt(
            ItemStack stack
    ) {
        return processing.canMelt(
                stack
        );
    }

    @Nullable
    public ResourceLocation getSelectedOutputMetal() {
        return processing.getSelectedOutputMetal();
    }

    public boolean setSelectedOutputMetal(
            ResourceLocation metal
    ) {
        return processing.setSelectedOutputMetal(
                metal
        );
    }

    @Nullable
    public ResourceLocation getSelectedOutputMetalOrDefault(
            FoundryTankNetwork network
    ) {
        return processing
                .getSelectedOutputMetalOrDefault(
                        network
                );
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            FoundryControllerBlockEntity controller
    ) {
        controller.processing.tick(
                level,
                pos
        );
    }

    // =========================
    // MENU / INVENTORIES
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

    public SimpleContainer getInputInventory() {
        return inputInventory;
    }

    public SimpleContainer getFuelInventory() {
        return fuelSystem.getInventory();
    }

    FoundryControllerFuelSystem getFuelSystem() {
        return fuelSystem;
    }

    public ContainerData getData() {
        return data;
    }

    public int getProgress() {
        return processing.getProgress();
    }

    public int getMaxProgress() {
        return processing.getMaxProgress();
    }

    public int getBurnTimeRemaining() {
        return fuelSystem.getBurnTimeRemaining();
    }

    public int getMaxBurnTime() {
        return fuelSystem.getMaxBurnTime();
    }

    public boolean isBurning() {
        return fuelSystem.isBurning();
    }

    public boolean hasAvailableFuel() {
        return fuelSystem.hasAvailableFuel();
    }

    public int getStoredCoalCount() {
        return fuelSystem.getStoredCoalCount();
    }

    @Nullable
    public ResourceLocation getActiveMoltenMetal() {
        return processing.getActiveMoltenMetal();
    }

    public int getActiveMoltenAmount() {
        return processing.getActiveMoltenAmount();
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
                inputInventory.createTag(
                        registries
                )
        );

        fuelSystem.save(
                tag,
                registries
        );

        processing.save(
                tag,
                registries
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

        if (
                tag.contains(
                        "InputInventory",
                        Tag.TAG_LIST
                )
        ) {
            inputInventory.fromTag(
                    tag.getList(
                            "InputInventory",
                            Tag.TAG_COMPOUND
                    ),
                    registries
            );
        }

        fuelSystem.load(
                tag,
                registries
        );

        processing.load(
                tag,
                registries
        );
    }
}
